package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoSegment
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CineRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun SplitTrimControls(
    selectedSegment: VideoSegment?,
    selectedSegmentIndex: Int,
    totalSegments: Int,
    totalDurationMs: Long,
    onTrim: (Int, Long, Long) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onSpeedChange: (Int, Float) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedSegment == null) return

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Cut Header & Quick Reorder / Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EDITING CUT ${selectedSegmentIndex + 1} OF $totalSegments",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${selectedSegment.label} • ${String.format("%.2fs", selectedSegment.durationMs / 1000.0)}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Move Left
                IconButton(
                    onClick = { onReorder(selectedSegmentIndex, (selectedSegmentIndex - 1).coerceAtLeast(0)) },
                    enabled = selectedSegmentIndex > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Move Left",
                        tint = if (selectedSegmentIndex > 0) Color.White else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Move Right
                IconButton(
                    onClick = { onReorder(selectedSegmentIndex, (selectedSegmentIndex + 1).coerceAtMost(totalSegments - 1)) },
                    enabled = selectedSegmentIndex < totalSegments - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Move Right",
                        tint = if (selectedSegmentIndex < totalSegments - 1) Color.White else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Delete Cut
                IconButton(
                    onClick = { onDelete(selectedSegmentIndex) },
                    enabled = totalSegments > 1,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_cut_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Cut",
                        tint = if (totalSegments > 1) CineRed else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Trim In / Out Range Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "In Point: ${String.format("%.2fs", selectedSegment.startMs / 1000.0)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Out Point: ${String.format("%.2fs", selectedSegment.endMs / 1000.0)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        RangeSlider(
            value = (selectedSegment.startMs.toFloat())..(selectedSegment.endMs.toFloat()),
            onValueChange = { range ->
                onTrim(selectedSegmentIndex, range.start.toLong(), range.endInclusive.toLong())
            },
            valueRange = 0f..totalDurationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = DarkSurfaceHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .testTag("trim_range_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Speed Ramping Options
        Text(
            text = "PLAYBACK SPEED:",
            color = AmberGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(speedOptions) { spd ->
                val isSelected = (selectedSegment.speed == spd)
                val label = when (spd) {
                    0.5f -> "0.5x Slow"
                    0.75f -> "0.75x Drama"
                    1.0f -> "1.0x Normal"
                    1.5f -> "1.5x Fast"
                    2.0f -> "2.0x Hyper"
                    else -> "${spd}x"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onSpeedChange(selectedSegmentIndex, spd) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberGold,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceHigh,
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}
