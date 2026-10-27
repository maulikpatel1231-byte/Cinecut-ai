package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransitionType
import com.example.model.VideoSegment
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun TimelineStrip(
    segments: List<VideoSegment>,
    selectedSegmentIndex: Int,
    currentPlayheadMs: Long,
    totalDurationMs: Long,
    onSelectSegment: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onChangeTransition: (Int, TransitionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        // Timeline Header: Scrubber Bar & Quick Split Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TIMELINE",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${segments.size} cuts)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Quick "Split at Playhead" Action Button
            Button(
                onClick = onSplitAtPlayhead,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("split_at_playhead_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Split",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Split Here", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Playhead Scrubber Slider
        Slider(
            value = currentPlayheadMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..totalDurationMs.coerceAtLeast(1000L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = DarkSurfaceHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag("playhead_slider")
        )

        // Visual Multi-segment Clip Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEachIndexed { index, segment ->
                val isSelected = (index == selectedSegmentIndex)
                val durationSec = segment.durationMs / 1000.0
                val clipWidth = ((segment.durationMs / 1000.0) * 45.0).coerceIn(80.0, 220.0).dp

                // Clip Card
                Box(
                    modifier = Modifier
                        .width(clipWidth)
                        .height(58.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) DarkSurfaceHigh else DarkSurfaceVariant)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CyanAccent else Color.DarkGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectSegment(index) }
                        .padding(6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = segment.label,
                                color = if (isSelected) CyanAccent else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (segment.speed != 1.0f) {
                                Text(
                                    text = "${segment.speed}x",
                                    color = AmberGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%.1fs", durationSec),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = segment.colorGrade.title.take(6),
                                    color = AmberGold,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Transition Badge between clips (if not the last one)
                if (index < segments.size - 1) {
                    val nextTransition = segment.transition
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (nextTransition != TransitionType.NONE) AmberGold.copy(alpha = 0.25f)
                                else DarkSurfaceHigh
                            )
                            .border(
                                1.dp,
                                if (nextTransition != TransitionType.NONE) AmberGold else Color.Gray,
                                CircleShape
                            )
                            .clickable {
                                // Cycle transition type on click
                                val allTypes = TransitionType.values()
                                val nextType = allTypes[(nextTransition.ordinal + 1) % allTypes.size]
                                onChangeTransition(index, nextType)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (nextTransition) {
                                TransitionType.FLASH_WHITE -> Icons.Default.FlashOn
                                TransitionType.WHIP_PAN -> Icons.Default.MotionPhotosAuto
                                TransitionType.ZOOM_IN -> Icons.Default.Speed
                                else -> Icons.Default.CallSplit
                            },
                            contentDescription = nextTransition.title,
                            tint = if (nextTransition != TransitionType.NONE) AmberGold else Color.LightGray,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
