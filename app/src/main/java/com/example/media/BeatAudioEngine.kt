package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.model.BeatTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "BeatAudioEngine"

class BeatAudioEngine(private val context: Context) {

    private var customMediaPlayer: MediaPlayer? = null
    private var synthAudioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var currentTrack: BeatTrack? = null
    private var isPlaying = false
    private var volume = 0.8f

    fun setTrack(track: BeatTrack) {
        stop()
        currentTrack = track
        if (track.customUri != null) {
            setupCustomMediaPlayer(Uri.parse(track.customUri))
        }
    }

    private fun setupCustomMediaPlayer(uri: Uri) {
        try {
            customMediaPlayer?.release()
            customMediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setVolume(volume, volume)
                isLooping = true
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing custom media player", e)
        }
    }

    fun play(startPositionMs: Long = 0L) {
        isPlaying = true
        val track = currentTrack ?: return

        if (track.customUri != null) {
            customMediaPlayer?.let {
                try {
                    it.seekTo(startPositionMs.toInt())
                    it.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting custom media player", e)
                }
            }
        } else {
            startSynthesizer(track, startPositionMs)
        }
    }

    fun pause() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            synthAudioTrack?.pause()
            synthAudioTrack?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing synth audio track", e)
        }
        try {
            customMediaPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing custom player", e)
        }
    }

    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            synthAudioTrack?.stop()
            synthAudioTrack?.release()
            synthAudioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping synth audio track", e)
        }
        try {
            customMediaPlayer?.stop()
            customMediaPlayer?.release()
            customMediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping custom player", e)
        }
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            synthAudioTrack?.setVolume(volume)
            customMediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    fun checkBeatDrop(currentPositionMs: Long, track: BeatTrack? = currentTrack): Boolean {
        if (track == null) return false
        val windowMs = 120L // active within 120ms of drop
        for (drop in track.accentDropsMs) {
            if (currentPositionMs in (drop - 30)..(drop + windowMs)) {
                return true
            }
        }
        // Also check regular beat tempo
        val interval = track.beatIntervalMs
        if (interval > 0) {
            val offset = currentPositionMs % interval
            if (offset in 0..60 || offset in (interval - 60)..interval) {
                return true
            }
        }
        return false
    }

    private fun startSynthesizer(track: BeatTrack, initialPositionMs: Long) {
        synthJob?.cancel()
        synthJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            synthAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            try {
                synthAudioTrack?.setVolume(volume)
                synthAudioTrack?.play()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start synth audio track", e)
                return@launch
            }

            val bpm = track.bpm
            val beatIntervalSec = 60.0 / bpm
            val samplesPerBeat = (sampleRate * beatIntervalSec).toInt()
            var currentSample = ((initialPositionMs / 1000.0) * sampleRate).toLong()

            val pcmChunk = ShortArray(1024)

            while (isActive && isPlaying) {
                for (i in pcmChunk.indices) {
                    val beatPos = (currentSample % samplesPerBeat).toInt()
                    val beatFraction = beatPos.toDouble() / samplesPerBeat

                    // Multi-tone kick / percussion waveform
                    val kickDecay = exp(-beatFraction * 20.0)
                    val kickFreq = 55.0 + 90.0 * kickDecay
                    val kickTone = sin(2.0 * PI * kickFreq * (beatPos.toDouble() / sampleRate)) * kickDecay

                    // Snare or Rim hit on beat 2 and 4
                    val isSnareBeat = (currentSample / samplesPerBeat) % 2 == 1L
                    val snareTone = if (isSnareBeat && beatFraction < 0.2) {
                        val noise = (Math.random() * 2.0 - 1.0)
                        noise * exp(-beatFraction * 35.0)
                    } else 0.0

                    // Bass groove
                    val bassFreq = when (((currentSample / (samplesPerBeat * 4)) % 4).toInt()) {
                        0 -> 65.4 // C2
                        1 -> 73.4 // D2
                        2 -> 82.4 // E2
                        else -> 55.0 // A1
                    }
                    val bassTone = sin(2.0 * PI * bassFreq * (currentSample.toDouble() / sampleRate)) * 0.35

                    val mixed = (kickTone * 0.6 + snareTone * 0.4 + bassTone * 0.3) * volume
                    val clamped = (mixed.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt()
                    pcmChunk[i] = clamped.toShort()
                    currentSample++
                }

                synthAudioTrack?.write(pcmChunk, 0, pcmChunk.size)
                delay(10)
            }
        }
    }

    fun release() {
        stop()
    }
}
