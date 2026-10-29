/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding
import java.text.NumberFormat
import kotlin.math.roundToInt

// ANZ -->
// Soft "liquid glass" accents: cool sky blue for volume, warm amber for brightness.
private val VolumeTop = Color(0xFF8BE9FD)
private val VolumeBottom = Color(0xFF3B9DF5)
private val BrightnessTop = Color(0xFFFFE29A)
private val BrightnessBottom = Color(0xFFFFA64D)
// ANZ <--

private fun percentage(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    return ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

private fun percentage(value: Int, range: ClosedRange<Int>): Float {
    return ((value - range.start).toFloat() / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

@Composable
fun VerticalSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    overflowValue: Float? = null,
    overflowRange: ClosedFloatingPointRange<Float>? = null,
    accentTop: Color = BrightnessTop,
    accentBottom: Color = BrightnessBottom,
) {
    require(range.contains(value)) { "Value must be within the provided range" }
    VerticalSliderInternal(
        percentage = percentage(value, range),
        overflowPercentage = overflowValue?.let { ov -> overflowRange?.let { or -> percentage(ov, or) } },
        modifier = modifier,
        accentTop = accentTop,
        accentBottom = accentBottom,
    )
}

@Composable
fun VerticalSlider(
    value: Int,
    range: ClosedRange<Int>,
    modifier: Modifier = Modifier,
    overflowValue: Int? = null,
    overflowRange: ClosedRange<Int>? = null,
    accentTop: Color = VolumeTop,
    accentBottom: Color = VolumeBottom,
) {
    require(range.contains(value)) { "Value must be within the provided range" }
    VerticalSliderInternal(
        percentage = percentage(value, range),
        overflowPercentage = overflowValue?.let { ov -> overflowRange?.let { or -> percentage(ov, or) } },
        modifier = modifier,
        accentTop = accentTop,
        accentBottom = accentBottom,
    )
}

// ANZ -->
/**
 * A glass tube with coloured "liquid" inside: a soft coloured glow behind the glass, a translucent
 * capsule with a light edge, an inset rounded fill with a soft vertical colour gradient, a bright
 * meniscus line on top of the liquid, a specular highlight on the glass and a faint reflection band.
 */
@Composable
private fun VerticalSliderInternal(
    percentage: Float,
    modifier: Modifier = Modifier,
    overflowPercentage: Float? = null,
    accentTop: Color,
    accentBottom: Color,
) {
    Box(
        modifier = modifier.height(136.dp).aspectRatio(0.5f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // soft coloured halo behind the glass tube
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.85f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(accentTop.copy(alpha = 0.20f), accentBottom.copy(alpha = 0.20f)),
                    ),
                )
                .blur(16.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.62f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.07f)),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f)),
                    ),
                    shape = CircleShape,
                )
                .padding(3.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val targetHeight by animateFloatAsState(percentage, label = "vsliderheight")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(targetHeight)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(accentTop, accentBottom))),
                contentAlignment = Alignment.TopCenter,
            ) {
                // meniscus: a bright line on the surface of the liquid
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth(fraction = 0.55f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f)),
                )
                // faint glow just under the meniscus
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(fraction = 0.7f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
                        .blur(3.dp),
                )
            }
            if (overflowPercentage != null) {
                val overflowHeight by animateFloatAsState(
                    targetValue = overflowPercentage,
                    label = "vslideroverflowheight",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(overflowHeight)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFF8A80).copy(alpha = 0.9f), Color(0xFFE5484D).copy(alpha = 0.9f)),
                            ),
                        ),
                )
            }
            // specular highlight on the glass
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 3.dp, top = 12.dp)
                    .width(3.dp)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.30f)),
            )
            // faint reflection band across the glass, a little below the highlight
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 5.dp, top = 46.dp)
                    .width(2.dp)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
            )
        }
    }
}

/** Percentage / level text above the slider, tinted with the slider accent. */
@Composable
private fun LevelLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
            shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
        ),
        color = color,
        textAlign = TextAlign.Center,
    )
}
// ANZ <--

@Composable
fun BrightnessSlider(
    brightness: Float,
    positiveRange: ClosedFloatingPointRange<Float>,
    negativeRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        // ANZ -->
        LevelLabel(text = (brightness * 100).toInt().toString(), color = BrightnessTop)
        VerticalSlider(
            value = brightness.coerceIn(0f, 1f),
            range = positiveRange,
            overflowRange = negativeRange,
            overflowValue = (-brightness).coerceIn(0f..0.75f),
            accentTop = BrightnessTop,
            accentBottom = BrightnessBottom,
        )
        // ANZ <--
    }
}

@Composable
fun VolumeSlider(
    volume: Int,
    mpvVolume: Int,
    range: ClosedRange<Int>,
    boostRange: ClosedRange<Int>?,
    modifier: Modifier = Modifier,
    displayAsPercentage: Boolean = false,
) {
    val percentage = (percentage(volume, range) * 100).roundToInt()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        val boostVolume = mpvVolume - 100
        val (deviceVolumeString, boostVolumeString) = getVolumeSliderText(
            volume,
            boostVolume,
            percentage,
            displayAsPercentage,
        )
        // ANZ -->
        LevelLabel(text = deviceVolumeString, color = VolumeTop)
        Box {
            VerticalSlider(
                value = if (displayAsPercentage) percentage else volume,
                range = if (displayAsPercentage) 0..100 else range,
                overflowValue = boostVolume,
                overflowRange = boostRange,
                accentTop = VolumeTop,
                accentBottom = VolumeBottom,
            )

            Text(
                text = boostVolumeString,
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(1f, 1f),
                        blurRadius = 4f,
                    ),
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        // ANZ <--
    }
}

val getVolumeSliderText: @Composable (Int, Int, Int, Boolean) -> Pair<String, String> =
    { volume, boostVolume, percentageInt, displayAsPercentage ->
        val percentFormat = remember { NumberFormat.getPercentInstance() }
        val integerFormat = remember { NumberFormat.getIntegerInstance() }
        val percentage = percentageInt / 100f

        val deviceVolumeString = if (displayAsPercentage) {
            percentFormat.format(percentage)
        } else {
            integerFormat.format(volume)
        }

        val boostVolumeString = when (boostVolume) {
            0 -> ""
            in 0..1000 -> "+${integerFormat.format(boostVolume)}"
            in -100..-1 -> "-${integerFormat.format(-boostVolume)}"
            else -> integerFormat.format(boostVolume)
        }

        Pair(deviceVolumeString, boostVolumeString)
    }
