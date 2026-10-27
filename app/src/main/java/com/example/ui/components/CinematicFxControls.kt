package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioPreset
import com.example.model.ColorAdjustments
import com.example.model.ColorGradePreset
import com.example.model.TransitionType
import com.example.model.VideoSegment
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun CinematicFxControls(
    selectedSegment: VideoSegment?,
    selectedSegmentIndex: Int,
    colorAdjustments: ColorAdjustments,
    aspectRatioPreset: AspectRatioPreset,
    onSelectColorGrade: (Int, ColorGradePreset) -> Unit,
    onSelectTransition: (Int, TransitionType) -> Unit,
    onSelectAspectRatio: (AspectRatioPreset) -> Unit,
    onUpdateAdjustments: (ColorAdjustments) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Section 1: Color Grading LUTs
        Text(
            text = "COLOR GRADE (APPLIED TO CUT ${selectedSegmentIndex + 1}):",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ColorGradePreset.values()) { preset ->
                val isSelected = (selectedSegment?.colorGrade == preset)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DarkSurfaceHigh else DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CyanAccent else Color.DarkGray
                    ),
                    modifier = Modifier.clickable {
                        onSelectColorGrade(selectedSegmentIndex, preset)
                    }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = preset.title,
                            color = if (isSelected) CyanAccent else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = preset.description.take(22) + "...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 2: Transitions for Selected Cut
        Text(
            text = "TRANSITION TO NEXT CUT:",
            color = AmberGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TransitionType.values()) { trans ->
                val isSelected = (selectedSegment?.transition == trans)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTransition(selectedSegmentIndex, trans) },
                    label = { Text(trans.title, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberGold,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceHigh,
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 3: Aspect Ratio & Cinematic Letterbox Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "2.39:1 Letterbox Bars",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = colorAdjustments.letterboxScope,
                onCheckedChange = {
                    onUpdateAdjustments(colorAdjustments.copy(letterboxScope = it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyanAccent,
                    checkedTrackColor = CyanAccent.copy(alpha = 0.4f)
                ),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Aspect Ratio Selector Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AspectRatioPreset.values()) { ratio ->
                val isSelected = (aspectRatioPreset == ratio)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectAspectRatio(ratio) },
                    label = { Text(ratio.title, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceHigh,
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 4: Fine-Tuning Sliders (Contrast & Saturation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Contrast: ${String.format("%.1fx", colorAdjustments.contrast)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Slider(
                    value = colorAdjustments.contrast,
                    onValueChange = { onUpdateAdjustments(colorAdjustments.copy(contrast = it)) },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saturation: ${String.format("%.1fx", colorAdjustments.saturation)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Slider(
                    value = colorAdjustments.saturation,
                    onValueChange = { onUpdateAdjustments(colorAdjustments.copy(saturation = it)) },
                    valueRange = 0.2f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberGold,
                        activeTrackColor = AmberGold
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}
