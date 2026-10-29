package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import `is`.xyz.mpv.Utils
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

@Composable
fun MiddlePlayerControls(
    // previous
    hasPrevious: Boolean,
    onSkipPrevious: () -> Unit,

    // middle
    isStopped: Boolean,
    isLoading: Boolean,
    isLoadingEpisode: Boolean,
    controlsShown: Boolean,
    areControlsLocked: Boolean,
    showLoadingCircle: Boolean,
    paused: Boolean,
    gestureSeekAmount: Pair<Int, Int>?,
    onPlayPauseClick: () -> Unit,

    // next
    hasNext: Boolean,
    onSkipNext: () -> Unit,

    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = enter,
            exit = exit,
        ) {
            if (gestureSeekAmount == null) {
                // ANZ -->
                SkipCircleButton(
                    icon = Icons.Rounded.SkipPrevious,
                    onClick = onSkipPrevious,
                    enabled = hasPrevious,
                )
                // ANZ <--
            }
        }

        // ANZ -->
        when {
            (isLoading || isLoadingEpisode) && showLoadingCircle -> {
                Box(Modifier.size(PLAY_SIZE), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        color = rememberGlassAccent().first,
                        strokeWidth = 3.dp,
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                }
            }
            isStopped -> {
                Spacer(Modifier.size(PLAY_SIZE))
            }
            gestureSeekAmount != null -> {
                Text(
                    stringResource(
                        MR.strings.player_gesture_seek_indicator,
                        if (gestureSeekAmount.second >= 0) '+' else '-',
                        Utils.prettyTime(abs(gestureSeekAmount.second)),
                        Utils.prettyTime(gestureSeekAmount.first + gestureSeekAmount.second),
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(Color.Black, blurRadius = 5f),
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val icon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
                AnimatedVisibility(
                    visible = controlsShown && !areControlsLocked,
                    enter = enter,
                    exit = exit,
                ) {
                    val (accentTop, accentBase) = rememberGlassAccent()
                    GlassCircleButton(
                        onClick = onPlayPauseClick,
                        diameter = PLAY_SIZE,
                        hero = true,
                    ) {
                        Image(
                            painter = rememberAnimatedVectorPainter(icon, !paused),
                            modifier = Modifier
                                .size(44.dp)
                                .accentGradientFill(accentTop, accentBase),
                            colorFilter = ColorFilter.tint(Color.White),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        // ANZ <--

        AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = enter,
            exit = exit,
        ) {
            if (gestureSeekAmount == null) {
                // ANZ -->
                SkipCircleButton(
                    icon = Icons.Rounded.SkipNext,
                    onClick = onSkipNext,
                    enabled = hasNext,
                )
                // ANZ <--
            }
        }
    }
}

// ANZ -->
private val PLAY_SIZE = 80.dp
private val SKIP_SIZE = 54.dp

/**
 * Accent tones taken from the app theme (light top, saturated base), the same idea as the liquid fill
 * of the volume / brightness bars: pale at the top, full color at the bottom.
 */
@Composable
private fun rememberGlassAccent(): Pair<Color, Color> {
    val primary = MaterialTheme.colorScheme.primary
    return remember(primary) { lerp(primary, Color.White, 0.72f) to primary }
}

/** Paints whatever is drawn by this composable (an icon) with a vertical accent gradient. */
private fun Modifier.accentGradientFill(top: Color, bottom: Color): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to lerp(top, Color.White, 0.45f),
                0.45f to top,
                1f to bottom,
            ),
            blendMode = BlendMode.SrcIn,
        )
    }

@Composable
private fun SkipCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val (accentTop, accentBase) = rememberGlassAccent()
    val k = if (enabled) 1f else 0.35f
    GlassCircleButton(
        onClick = onClick,
        diameter = SKIP_SIZE,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(30.dp)
                .accentGradientFill(accentTop.copy(alpha = k), accentBase.copy(alpha = k)),
        )
    }
}

/**
 * Domed dark glass, built only from cheap draw ops (no blur):
 *  - a near-black body that is a touch lighter at the upper-left, like a lit convex surface;
 *  - an accent glow pooling at the bottom and a soft one behind the icon;
 *  - a glossy highlight across the upper half, a specular arc and a tiny bright dot;
 *  - an inner edge shadow for depth and a two-line rim (bright top-left, accent bottom-right);
 *  - springy press (it also lights up a little more) plus a light haptic tick.
 */
@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    diameter: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hero: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glass_button_scale",
    )
    // read in the draw phase only, so pressing never recomposes the button
    val press = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "glass_button_press",
    )
    val haptic = LocalHapticFeedback.current
    val clickEvent = LocalPlayerButtonsClickEvent.current
    val (accentTop, accentBase) = rememberGlassAccent()
    val k = if (enabled) 1f else 0.45f
    val pool = if (hero) 0.42f else 0.30f
    val rimAccent = if (hero) 0.85f else 0.60f

    Box(
        modifier = modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .drawBehind {
                val w = size.width
                val h = size.height
                val d = size.minDimension
                val pr = press.value

                // body: deep, but lighter towards the upper-left so it reads as a convex surface
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2C2C32).copy(alpha = 0.88f * k),
                            Color(0xFF09090B).copy(alpha = 0.92f * k),
                        ),
                        center = Offset(w * 0.36f, h * 0.28f),
                        radius = d * 0.95f,
                    ),
                )
                // accent pooling at the bottom
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentBase.copy(alpha = ((pool + 0.20f * pr) * k).coerceIn(0f, 1f)),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.5f, h * 1.02f),
                        radius = d * 0.80f,
                    ),
                )
                // soft accent behind the icon
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentBase.copy(alpha = ((0.10f + 0.18f * pr) * k).coerceIn(0f, 1f)),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = d * 0.44f,
                    ),
                )
                // inner edge shadow: darkens the rim so the middle feels raised
                drawRect(
                    brush = Brush.radialGradient(
                        0.60f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.42f * k),
                        center = center,
                        radius = d * 0.5f,
                    ),
                )
                // gloss across the upper half
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.28f + 0.06f * pr) * k),
                            Color.White.copy(alpha = 0.02f * k),
                        ),
                        startY = d * 0.05f,
                        endY = d * 0.52f,
                    ),
                    topLeft = Offset(d * 0.13f, d * 0.05f),
                    size = Size(w - d * 0.26f, d * 0.47f),
                )
                // specular arc and a tiny bright dot, top-left
                val inset = d * 0.14f
                drawArc(
                    color = Color.White.copy(alpha = 0.50f * k),
                    startAngle = 200f,
                    sweepAngle = 56f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(w - inset * 2, h - inset * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f * k),
                    radius = 1.6.dp.toPx(),
                    center = Offset(w * 0.27f, h * 0.2f),
                )
                // rim: bright on the upper-left, accent on the lower-right, plus a faint inner line
                val rimWidth = (if (hero) 1.5.dp else 1.2.dp).toPx()
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.78f * k),
                            Color.White.copy(alpha = 0.07f * k),
                            accentTop.copy(alpha = (rimAccent * k).coerceIn(0f, 1f)),
                        ),
                        start = Offset.Zero,
                        end = Offset(w, h),
                    ),
                    radius = (d - rimWidth) / 2f,
                    style = Stroke(width = rimWidth),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.09f * k),
                    radius = d / 2f - rimWidth - 2.5.dp.toPx(),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = accentTop),
                enabled = enabled,
            ) {
                clickEvent()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
// ANZ <--
