package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiCinematicDirector
import com.example.ai.DirectorResult
import com.example.media.BeatAudioEngine
import com.example.media.BeatTrackRepository
import com.example.media.VideoEngine
import com.example.model.AiThinkingState
import com.example.model.AspectRatioPreset
import com.example.model.BeatTrack
import com.example.model.CinematicStyle
import com.example.model.ColorAdjustments
import com.example.model.ColorGradePreset
import com.example.model.ExportState
import com.example.model.TrackLanguage
import com.example.model.TransitionType
import com.example.model.VideoSegment
import com.example.model.VideoSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "VideoEditorVM"

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val director = AiCinematicDirector()
    val audioEngine = BeatAudioEngine(application)

    private val _videoSource = MutableStateFlow<VideoSource?>(null)
    val videoSource: StateFlow<VideoSource?> = _videoSource.asStateFlow()

    private val _segments = MutableStateFlow<List<VideoSegment>>(emptyList())
    val segments: StateFlow<List<VideoSegment>> = _segments.asStateFlow()

    private val _selectedSegmentIndex = MutableStateFlow(0)
    val selectedSegmentIndex: StateFlow<Int> = _selectedSegmentIndex.asStateFlow()

    private val _currentPlayheadMs = MutableStateFlow(0L)
    val currentPlayheadMs: StateFlow<Long> = _currentPlayheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBeatFlashActive = MutableStateFlow(false)
    val isBeatFlashActive: StateFlow<Boolean> = _isBeatFlashActive.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioPreset.SCOPE_239)
    val aspectRatio: StateFlow<AspectRatioPreset> = _aspectRatio.asStateFlow()

    private val _colorAdjustments = MutableStateFlow(ColorAdjustments())
    val colorAdjustments: StateFlow<ColorAdjustments> = _colorAdjustments.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(TrackLanguage.ALL)
    val selectedLanguage: StateFlow<TrackLanguage> = _selectedLanguage.asStateFlow()

    private val _activeBeatTrack = MutableStateFlow<BeatTrack?>(BeatTrackRepository.defaultTracks.firstOrNull())
    val activeBeatTrack: StateFlow<BeatTrack?> = _activeBeatTrack.asStateFlow()

    private val _videoVolume = MutableStateFlow(0.7f)
    val videoVolume: StateFlow<Float> = _videoVolume.asStateFlow()

    private val _musicVolume = MutableStateFlow(0.8f)
    val musicVolume: StateFlow<Float> = _musicVolume.asStateFlow()

    private val _aiThinkingState = MutableStateFlow<AiThinkingState>(AiThinkingState.Idle)
    val aiThinkingState: StateFlow<AiThinkingState> = _aiThinkingState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private var playbackLoopJob: Job? = null

    init {
        _activeBeatTrack.value?.let {
            audioEngine.setTrack(it)
            audioEngine.setVolume(_musicVolume.value)
        }
    }

    fun importVideo(uri: Uri) {
        viewModelScope.launch {
            _isPlaying.value = false
            audioEngine.stop()
            val meta = VideoEngine.loadVideoMetadata(getApplication(), uri)
            if (meta != null) {
                _videoSource.value = meta
                // Initialize default segment with entire video length
                val initialSegment = VideoSegment(
                    id = UUID.randomUUID().toString(),
                    startMs = 0L,
                    endMs = meta.durationMs,
                    speed = 1.0f,
                    transition = TransitionType.NONE,
                    label = "Full Clip",
                    colorGrade = ColorGradePreset.NATURAL
                )
                _segments.value = listOf(initialSegment)
                _selectedSegmentIndex.value = 0
                _currentPlayheadMs.value = 0L

                // Auto-suggest 9:16 if vertical video, else 2.39:1 scope
                if (meta.height > meta.width) {
                    _aspectRatio.value = AspectRatioPreset.VERTICAL_916
                } else {
                    _aspectRatio.value = AspectRatioPreset.SCOPE_239
                }
            } else {
                Log.e(TAG, "Failed to load metadata for URI: $uri")
            }
        }
    }

    fun startAiDirector(style: CinematicStyle) {
        val video = _videoSource.value ?: return
        viewModelScope.launch {
            _aiThinkingState.value = AiThinkingState.Thinking(
                step = com.example.model.AiThinkingStep(
                    1, 5, "Initializing", "Warming up AI Director engine...", ""
                ),
                progress = 0.05f,
                accumulatedLogs = listOf("Starting Deep Thinking for ${style.displayName}...")
            )

            val accumulatedLogs = mutableListOf<String>()
            director.analyzeAndSplitVideo(video, style, _activeBeatTrack.value).collect { result ->
                when (result) {
                    is DirectorResult.Progress -> {
                        accumulatedLogs.add(result.thought)
                        _aiThinkingState.value = AiThinkingState.Thinking(
                            step = result.step,
                            progress = result.progressPercentage,
                            accumulatedLogs = accumulatedLogs.toList()
                        )
                    }
                    is DirectorResult.Completed -> {
                        _aiThinkingState.value = AiThinkingState.Ready(
                            style = result.style,
                            suggestedSegments = result.segments,
                            reasoningSummary = result.reasoningSummary,
                            beatDropMatches = result.totalBeatMatches
                        )
                    }
                    is DirectorResult.Failed -> {
                        _aiThinkingState.value = AiThinkingState.Error(result.error)
                    }
                }
            }
        }
    }

    fun applyAiPlan(style: CinematicStyle, suggestedSegments: List<VideoSegment>) {
        _segments.value = suggestedSegments
        _selectedSegmentIndex.value = 0
        _currentPlayheadMs.value = 0L

        // Apply recommended style colors
        _colorAdjustments.value = _colorAdjustments.value.copy(
            letterboxScope = (style != CinematicStyle.REEL_TRENDING),
            contrast = when (style) {
                CinematicStyle.ACTION_BLOCKBUSTER -> 1.35f
                CinematicStyle.NEO_NOIR -> 1.5f
                CinematicStyle.CYBER_THRILLER -> 1.4f
                else -> 1.15f
            },
            saturation = when (style) {
                CinematicStyle.GUJARATI_GARBA_PULSE -> 1.45f
                CinematicStyle.NEO_NOIR -> 0.2f
                else -> 1.2f
            }
        )

        if (style == CinematicStyle.REEL_TRENDING) {
            _aspectRatio.value = AspectRatioPreset.VERTICAL_916
        }

        _aiThinkingState.value = AiThinkingState.Idle
    }

    fun dismissAiThinking() {
        _aiThinkingState.value = AiThinkingState.Idle
    }

    // Timeline editing actions
    fun splitCurrentSegmentAtPlayhead() {
        val currentMs = _currentPlayheadMs.value
        val currentSegs = _segments.value.toMutableList()
        val index = currentSegs.indexOfFirst { currentMs in it.startMs..it.endMs }

        if (index != -1) {
            val target = currentSegs[index]
            // Minimum cut duration of 300ms on both sides
            if (currentMs - target.startMs >= 300L && target.endMs - currentMs >= 300L) {
                val segA = target.copy(
                    id = UUID.randomUUID().toString(),
                    endMs = currentMs,
                    transition = TransitionType.NONE
                )
                val segB = target.copy(
                    id = UUID.randomUUID().toString(),
                    startMs = currentMs,
                    label = "Cut ${index + 2}"
                )

                currentSegs[index] = segA
                currentSegs.add(index + 1, segB)
                _segments.value = currentSegs
                _selectedSegmentIndex.value = index + 1
            }
        }
    }

    fun trimSegment(index: Int, newStartMs: Long, newEndMs: Long) {
        val currentSegs = _segments.value.toMutableList()
        if (index in currentSegs.indices) {
            val original = currentSegs[index]
            val safeStart = newStartMs.coerceAtLeast(0L)
            val safeEnd = newEndMs.coerceAtLeast(safeStart + 200L)
            currentSegs[index] = original.copy(startMs = safeStart, endMs = safeEnd)
            _segments.value = currentSegs
        }
    }

    fun deleteSegment(index: Int) {
        val currentSegs = _segments.value.toMutableList()
        if (currentSegs.size > 1 && index in currentSegs.indices) {
            currentSegs.removeAt(index)
            _segments.value = currentSegs
            _selectedSegmentIndex.value = (index - 1).coerceAtLeast(0)
        }
    }

    fun reorderSegment(from: Int, to: Int) {
        val currentSegs = _segments.value.toMutableList()
        if (from in currentSegs.indices && to in currentSegs.indices) {
            val item = currentSegs.removeAt(from)
            currentSegs.add(to, item)
            _segments.value = currentSegs
            _selectedSegmentIndex.value = to
        }
    }

    fun setSegmentTransition(index: Int, transition: TransitionType) {
        val currentSegs = _segments.value.toMutableList()
        if (index in currentSegs.indices) {
            currentSegs[index] = currentSegs[index].copy(transition = transition)
            _segments.value = currentSegs
        }
    }

    fun setSegmentColorGrade(index: Int, colorGrade: ColorGradePreset) {
        val currentSegs = _segments.value.toMutableList()
        if (index in currentSegs.indices) {
            currentSegs[index] = currentSegs[index].copy(colorGrade = colorGrade)
            _segments.value = currentSegs
        }
    }

    fun setSegmentSpeed(index: Int, speed: Float) {
        val currentSegs = _segments.value.toMutableList()
        if (index in currentSegs.indices) {
            currentSegs[index] = currentSegs[index].copy(speed = speed)
            _segments.value = currentSegs
        }
    }

    fun selectSegment(index: Int) {
        if (index in _segments.value.indices) {
            _selectedSegmentIndex.value = index
            _currentPlayheadMs.value = _segments.value[index].startMs
        }
    }

    // Music & Beat Controls
    fun setTrackLanguage(lang: TrackLanguage) {
        _selectedLanguage.value = lang
    }

    fun selectBeatTrack(track: BeatTrack) {
        _activeBeatTrack.value = track
        audioEngine.setTrack(track)
        audioEngine.setVolume(_musicVolume.value)
        if (_isPlaying.value) {
            audioEngine.play(_currentPlayheadMs.value)
        }
    }

    fun importCustomAudio(uri: Uri, displayName: String) {
        val custom = BeatTrack(
            id = "custom_${System.currentTimeMillis()}",
            title = displayName.ifBlank { "Custom Track" },
            language = TrackLanguage.CUSTOM,
            artist = "Imported Audio",
            genre = "User Audio",
            bpm = 120,
            durationMs = 60_000L,
            accentDropsMs = listOf(4000L, 8000L, 12000L, 16000L, 24000L),
            customUri = uri.toString()
        )
        selectBeatTrack(custom)
    }

    fun autoSnapCutsToBeat(track: BeatTrack) {
        val video = _videoSource.value ?: return
        val drops = track.accentDropsMs.filter { it in 1000L..(video.durationMs - 1000L) }
        if (drops.isEmpty()) return

        val boundaries = (listOf(0L) + drops + listOf(video.durationMs)).distinct().sorted()
        val newSegs = mutableListOf<VideoSegment>()
        for (i in 0 until boundaries.size - 1) {
            val s = boundaries[i]
            val e = boundaries[i + 1]
            if (e - s < 500L) continue
            newSegs.add(
                VideoSegment(
                    id = UUID.randomUUID().toString(),
                    startMs = s,
                    endMs = e,
                    transition = if (i % 2 == 0) TransitionType.FLASH_WHITE else TransitionType.ZOOM_IN,
                    label = "Beat Drop ${i + 1}",
                    colorGrade = _segments.value.getOrNull(i)?.colorGrade ?: ColorGradePreset.NATURAL
                )
            )
        }
        if (newSegs.isNotEmpty()) {
            _segments.value = newSegs
            _selectedSegmentIndex.value = 0
        }
    }

    fun setVideoVolume(vol: Float) {
        _videoVolume.value = vol.coerceIn(0f, 1f)
    }

    fun setMusicVolume(vol: Float) {
        _musicVolume.value = vol.coerceIn(0f, 1f)
        audioEngine.setVolume(_musicVolume.value)
    }

    fun updateColorAdjustments(adjustments: ColorAdjustments) {
        _colorAdjustments.value = adjustments
    }

    fun setAspectRatio(ratio: AspectRatioPreset) {
        _aspectRatio.value = ratio
    }

    // Playback scrubber loop
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val video = _videoSource.value ?: return
        _isPlaying.value = true
        audioEngine.play(_currentPlayheadMs.value)

        playbackLoopJob?.cancel()
        playbackLoopJob = viewModelScope.launch {
            val totalDuration = video.durationMs
            var pos = _currentPlayheadMs.value

            while (isActive && _isPlaying.value) {
                delay(33) // ~30 fps tick
                pos += 33
                if (pos >= totalDuration) {
                    pos = 0L // loop
                }
                _currentPlayheadMs.value = pos

                // Beat flash detection
                val isDrop = audioEngine.checkBeatDrop(pos, _activeBeatTrack.value)
                _isBeatFlashActive.value = isDrop && _colorAdjustments.value.beatFlashEnabled

                // Keep selected segment in sync with playhead
                val segs = _segments.value
                val segIdx = segs.indexOfFirst { pos in it.startMs..it.endMs }
                if (segIdx != -1 && segIdx != _selectedSegmentIndex.value) {
                    _selectedSegmentIndex.value = segIdx
                }
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        _isBeatFlashActive.value = false
        audioEngine.pause()
        playbackLoopJob?.cancel()
        playbackLoopJob = null
    }

    fun seekTo(positionMs: Long) {
        val video = _videoSource.value ?: return
        val clamped = positionMs.coerceIn(0L, video.durationMs)
        _currentPlayheadMs.value = clamped

        val segs = _segments.value
        val segIdx = segs.indexOfFirst { clamped in it.startMs..it.endMs }
        if (segIdx != -1) {
            _selectedSegmentIndex.value = segIdx
        }

        if (_isPlaying.value) {
            audioEngine.play(clamped)
        }
    }

    // Export & Merge Final Output
    fun exportFinalVideo() {
        val video = _videoSource.value ?: return
        val segs = _segments.value
        if (segs.isEmpty()) return

        pause()
        _exportState.value = ExportState.Exporting(0.02f, "Initializing cinematic render engine...")

        viewModelScope.launch {
            val result = VideoEngine.exportAndMerge(
                context = getApplication(),
                source = video,
                segments = segs,
                colorAdjustments = _colorAdjustments.value,
                onProgress = { progress, msg ->
                    _exportState.value = ExportState.Exporting(progress, msg)
                }
            )

            result.fold(
                onSuccess = { file ->
                    _exportState.value = ExportState.Success(
                        outputUri = Uri.fromFile(file),
                        outputPath = file.absolutePath,
                        durationMs = segs.sumOf { it.durationMs }
                    )
                },
                onFailure = { err ->
                    _exportState.value = ExportState.Error(err.localizedMessage ?: "Failed to export video")
                }
            )
        }
    }

    fun dismissExportDialog() {
        _exportState.value = ExportState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        playbackLoopJob?.cancel()
    }
}
