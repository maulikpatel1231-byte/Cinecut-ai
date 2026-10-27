package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AiThinkingState
import com.example.ui.components.AiDirectorDialog
import com.example.ui.components.CinematicFxControls
import com.example.ui.components.EmptyGalleryState
import com.example.ui.components.ExportProgressDialog
import com.example.ui.components.MusicBeatsSheet
import com.example.ui.components.SplitTrimControls
import com.example.ui.components.TimelineStrip
import com.example.ui.components.VideoPreviewCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.viewmodel.VideoEditorViewModel

enum class EditorTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AI_DIRECTOR("AI Director", Icons.Default.Psychology),
    MUSIC_BEATS("Beats & Music", Icons.Default.MusicNote),
    SPLIT_TRIM("Split & Trim", Icons.Default.ContentCut),
    CINEMATIC_FX("Cinematic FX", Icons.Default.ColorLens)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScreen(
    viewModel: VideoEditorViewModel = viewModel()
) {
    val videoSource by viewModel.videoSource.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val selectedSegmentIndex by viewModel.selectedSegmentIndex.collectAsState()
    val currentPlayheadMs by viewModel.currentPlayheadMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBeatFlash by viewModel.isBeatFlashActive.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val colorAdjustments by viewModel.colorAdjustments.collectAsState()
    val activeBeatTrack by viewModel.activeBeatTrack.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val videoVolume by viewModel.videoVolume.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()
    val aiState by viewModel.aiThinkingState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    var showAiDirectorDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(EditorTab.AI_DIRECTOR) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.importVideo(uri)
        }
    }

    val currentSegment = segments.getOrNull(selectedSegmentIndex)

    if (videoSource == null) {
        EmptyGalleryState(
            onImportVideo = { uri -> viewModel.importVideo(uri) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CINECUT AI",
                                    color = CyanAccent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AmberGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = AmberGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = videoSource?.displayName ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        // Change / Import Video
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.testTag("import_new_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Import New Video",
                                tint = Color.White
                            )
                        }

                        // AI Director Trigger
                        Button(
                            onClick = { showAiDirectorDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("top_ai_director_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Split", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Export & Merge Final Output
                        Button(
                            onClick = { viewModel.exportFinalVideo() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("export_merge_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Merge & Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    EditorTab.values().forEach { tab ->
                        val isSelected = (selectedTab == tab)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                if (tab == EditorTab.AI_DIRECTOR) {
                                    showAiDirectorDialog = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanAccent,
                                indicatorColor = CyanAccent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            containerColor = DarkBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Video Player & Real-time Canvas Area
                VideoPreviewCard(
                    videoSource = videoSource!!,
                    currentSegment = currentSegment,
                    currentPlayheadMs = currentPlayheadMs,
                    isPlaying = isPlaying,
                    isBeatFlash = isBeatFlash,
                    aspectRatioPreset = aspectRatio,
                    colorAdjustments = colorAdjustments,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeek = { pos -> viewModel.seekTo(pos) }
                )

                // Multi-segment Scrubber Timeline Strip
                TimelineStrip(
                    segments = segments,
                    selectedSegmentIndex = selectedSegmentIndex,
                    currentPlayheadMs = currentPlayheadMs,
                    totalDurationMs = videoSource!!.durationMs,
                    onSelectSegment = { idx -> viewModel.selectSegment(idx) },
                    onSeek = { pos -> viewModel.seekTo(pos) },
                    onSplitAtPlayhead = { viewModel.splitCurrentSegmentAtPlayhead() },
                    onChangeTransition = { idx, trans -> viewModel.setSegmentTransition(idx, trans) }
                )

                // Tab Content for Custom Editing Fine-Tuning
                when (selectedTab) {
                    EditorTab.AI_DIRECTOR -> {
                        // Quick Action Card for AI Director
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "AI DEEP THINKING DIRECTOR",
                                            color = CyanAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Auto-Split, Rhythm Sync & Color",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = { showAiDirectorDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Open Director", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Select from Action, Bollywood Romance, Gujarati Garba Pulse, or Cyberpunk styles. The AI analyzes your video duration and creates cinematic cuts with beat drops.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    EditorTab.MUSIC_BEATS -> {
                        MusicBeatsSheet(
                            activeTrack = activeBeatTrack,
                            selectedLanguage = selectedLanguage,
                            videoVolume = videoVolume,
                            musicVolume = musicVolume,
                            colorAdjustments = colorAdjustments,
                            onSelectLanguage = { lang -> viewModel.setTrackLanguage(lang) },
                            onSelectTrack = { trk -> viewModel.selectBeatTrack(trk) },
                            onImportCustomAudio = { uri, name -> viewModel.importCustomAudio(uri, name) },
                            onSnapCutsToBeat = { trk -> viewModel.autoSnapCutsToBeat(trk) },
                            onUpdateVideoVolume = { v -> viewModel.setVideoVolume(v) },
                            onUpdateMusicVolume = { v -> viewModel.setMusicVolume(v) },
                            onUpdateAdjustments = { adj -> viewModel.updateColorAdjustments(adj) }
                        )
                    }

                    EditorTab.SPLIT_TRIM -> {
                        SplitTrimControls(
                            selectedSegment = currentSegment,
                            selectedSegmentIndex = selectedSegmentIndex,
                            totalSegments = segments.size,
                            totalDurationMs = videoSource!!.durationMs,
                            onTrim = { idx, s, e -> viewModel.trimSegment(idx, s, e) },
                            onSplitAtPlayhead = { viewModel.splitCurrentSegmentAtPlayhead() },
                            onSpeedChange = { idx, spd -> viewModel.setSegmentSpeed(idx, spd) },
                            onReorder = { from, to -> viewModel.reorderSegment(from, to) },
                            onDelete = { idx -> viewModel.deleteSegment(idx) }
                        )
                    }

                    EditorTab.CINEMATIC_FX -> {
                        CinematicFxControls(
                            selectedSegment = currentSegment,
                            selectedSegmentIndex = selectedSegmentIndex,
                            colorAdjustments = colorAdjustments,
                            aspectRatioPreset = aspectRatio,
                            onSelectColorGrade = { idx, grade -> viewModel.setSegmentColorGrade(idx, grade) },
                            onSelectTransition = { idx, trans -> viewModel.setSegmentTransition(idx, trans) },
                            onSelectAspectRatio = { r -> viewModel.setAspectRatio(r) },
                            onUpdateAdjustments = { adj -> viewModel.updateColorAdjustments(adj) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // AI Director Dialog Modal
    if (showAiDirectorDialog || aiState !is AiThinkingState.Idle) {
        AiDirectorDialog(
            aiState = aiState,
            onStartThinking = { style -> viewModel.startAiDirector(style) },
            onApplyPlan = { style, segs ->
                viewModel.applyAiPlan(style, segs)
                showAiDirectorDialog = false
            },
            onDismiss = {
                viewModel.dismissAiThinking()
                showAiDirectorDialog = false
            }
        )
    }

    // Export Progress & Save Dialog
    if (exportState !is com.example.model.ExportState.Idle) {
        ExportProgressDialog(
            exportState = exportState,
            onDismiss = { viewModel.dismissExportDialog() }
        )
    }
}
