package com.example.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.MediaPlayer
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AspectRatioPreset
import com.example.model.ColorAdjustments
import com.example.model.VideoSegment
import com.example.model.VideoSource
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import kotlin.random.Random

@Composable
fun VideoPreviewCard(
    videoSource: VideoSource,
    currentSegment: VideoSegment?,
    currentPlayheadMs: Long,
    isPlaying: Boolean,
    isBeatFlash: Boolean,
    aspectRatioPreset: AspectRatioPreset,
    colorAdjustments: ColorAdjustments,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    // Camera shake animation on beat
    val shakeOffset = remember { Animatable(0f) }
    val zoomScale by animateFloatAsState(
        targetValue = if (isBeatFlash && colorAdjustments.beatShakeEnabled) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "beatZoom"
    )

    LaunchedEffect(isBeatFlash) {
        if (isBeatFlash && colorAdjustments.beatShakeEnabled) {
            val randomX = (Random.nextFloat() * 16f) - 8f
            shakeOffset.snapTo(randomX)
            shakeOffset.animateTo(0f, tween(150))
        }
    }

    // VideoView sync with playhead & play/pause state
    LaunchedEffect(isPlaying) {
        videoViewRef?.let { vv ->
            if (isPlaying) {
                if (!vv.isPlaying) {
                    vv.seekTo(currentPlayheadMs.toInt())
                    vv.start()
                }
            } else {
                if (vv.isPlaying) {
                    vv.pause()
                }
            }
        }
    }

    LaunchedEffect(currentPlayheadMs) {
        videoViewRef?.let { vv ->
            val diff = Math.abs(vv.currentPosition - currentPlayheadMs.toInt())
            if (diff > 800) {
                vv.seekTo(currentPlayheadMs.toInt())
            }
        }
    }

    val activeMatrix = remember(currentSegment?.colorGrade, colorAdjustments) {
        val base = currentSegment?.colorGrade?.colorFilterMatrix ?: floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        // Combine fine tuning
        val m = ColorMatrix(base)
        if (colorAdjustments.contrast != 1.0f) {
            val scale = colorAdjustments.contrast
            val translate = (-0.5f * scale + 0.5f) * 255f
            m.postConcat(ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        if (colorAdjustments.saturation != 1.0f) {
            val satMatrix = ColorMatrix().apply { setSaturation(colorAdjustments.saturation) }
            m.postConcat(satMatrix)
        }
        m
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val targetRatio = aspectRatioPreset.ratio

        Box(
            modifier = Modifier
                .width(containerWidth)
                .aspectRatio(targetRatio)
                .clip(RoundedCornerShape(12.dp))
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = shakeOffset.value
                }
                .clickable { onTogglePlayPause() },
            contentAlignment = Alignment.Center
        ) {
            // Video Playback Surface
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(videoSource.uri)
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            mp.isLooping = true
                            mp.setVolume(1.0f, 1.0f)
                            seekTo(currentPlayheadMs.toInt())
                            if (isPlaying) start()
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_player_surface")
            )

            // If thumbnail is available and video is paused at start, show crisp preview thumbnail
            if (!isPlaying && videoSource.thumbnailBitmap != null && currentPlayheadMs == 0L) {
                Image(
                    bitmap = videoSource.thumbnailBitmap.asImageBitmap(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Cinematic 2.39:1 Letterbox Scope Bars (if enabled)
            if (colorAdjustments.letterboxScope) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color.Black)
                        .align(Alignment.TopCenter)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color.Black)
                        .align(Alignment.BottomCenter)
                )
            }

            // Real-time Beat Flash FX Overlay
            AnimatedVisibility(
                visible = isBeatFlash && colorAdjustments.beatFlashEnabled,
                enter = fadeIn(tween(40)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }

            // Play / Pause central indicator (when paused)
            if (!isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = CyanAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Top Status Bar: Active Cut Label & Color Grade Tag
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${currentSegment?.label ?: "Shot 1"} • ${currentSegment?.colorGrade?.title ?: "Original"}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Bottom Right Timecode Badge
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                val currentSec = currentPlayheadMs / 1000.0
                val totalSec = videoSource.durationMs / 1000.0
                Text(
                    text = String.format("%04.1fs / %04.1fs", currentSec, totalSec),
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
                mediaPlayerRef?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
