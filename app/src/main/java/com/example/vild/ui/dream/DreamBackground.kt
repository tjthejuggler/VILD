package com.example.vild.ui.dream

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.DeepIndigo
import com.example.vild.ui.theme.DreamPink
import com.example.vild.ui.theme.Indigo
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.StarGold
import com.example.vild.ui.theme.Violet
import com.example.vild.ui.theme.Void
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,        // 0..1 across width
    val y: Float,        // 0..1 across height
    val radius: Float,   // px
    val phase: Float,    // twinkle offset
    val speed: Float,    // twinkle speed
    val depth: Float,    // parallax factor
    val color: Color,
)

/**
 * The living background of the whole app: a deep void gradient, drifting
 * nebula orbs, and a twinkling starfield — all subtly parallaxed by the
 * device accelerometer and a slow breathing glow.
 *
 * Layers move at different depths so tilting the phone makes the dream
 * world shift like looking through water.
 */
@Composable
fun DreamBackground(
    tilt: TiltState,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dream")

    // Slow cosmic drift for the orbs (very long, reversed loops).
    val driftA by infiniteTransition.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftA",
    )
    val driftB by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(34_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftB",
    )
    val driftC by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(42_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftC",
    )
    // Breathing glow (the dream inhales… exhales…).
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(7_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "breath",
    )
    // Global time for star twinkle.
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )

    val stars = remember {
        List(70) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = 0.8f + Random.nextFloat() * 1.8f,
                phase = Random.nextFloat() * (2f * Math.PI.toFloat()),
                speed = 0.5f + Random.nextFloat() * 1.5f,
                depth = 6f + Random.nextFloat() * 14f,
                color = when (Random.nextInt(10)) {
                    0 -> StarGold
                    1 -> AuroraTeal
                    2 -> DreamPink
                    else -> MoonLavender
                },
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Void, DeepIndigo, Indigo, Void),
                    start = Offset.Zero,
                    end = Offset(900f, 1900f),
                ),
            ),
    ) {
        // ── Nebula orbs (radial gradients, accelerometer parallax + drift) ─────
        NebulaOrb(
            size = 420.dp,
            color = Violet,
            alpha = 0.30f,
            offsetX = tilt.x * -34f + driftA,
            offsetY = tilt.y * -34f + driftB,
            alignment = { androidx.compose.ui.Alignment.TopStart },
        )
        NebulaOrb(
            size = 520.dp,
            color = AuroraTeal,
            alpha = 0.16f,
            offsetX = tilt.x * -58f + driftB,
            offsetY = tilt.y * -58f + driftC,
            alignment = { androidx.compose.ui.Alignment.BottomEnd },
        )
        NebulaOrb(
            size = 360.dp,
            color = DreamPink,
            alpha = 0.18f,
            offsetX = tilt.x * -22f + driftC,
            offsetY = tilt.y * -22f + driftA,
            alignment = { androidx.compose.ui.Alignment.Center },
        )

        // ── Breathing central glow ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(androidx.compose.ui.Alignment.Center)
                    .scale(breath)
                    .alpha(0.22f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MoonLavender.copy(alpha = 0.55f), Color.Transparent),
                        ),
                    ),
            )
        }

        // ── Twinkling starfield with parallax ─────────────────────────────────
        val density = LocalDensity.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            stars.forEach { star ->
                val twinkle = 0.35f + 0.65f * (
                    0.5f + 0.5f * sin(time * star.speed + star.phase)
                    )
                drawCircle(
                    color = star.color.copy(alpha = twinkle * 0.8f),
                    radius = star.radius,
                    center = Offset(
                        star.x * w + tilt.x * -star.depth * density.density,
                        star.y * h + tilt.y * -star.depth * density.density,
                    ),
                )
            }
        }
    }
}

/** One soft nebula orb: a clipped radial gradient floating in the void. */
@Composable
private fun NebulaOrb(
    size: Dp,
    color: Color,
    alpha: Float,
    offsetX: Float,
    offsetY: Float,
    alignment: () -> androidx.compose.ui.Alignment,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .align(alignment())
                .alpha(alpha)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.75f), color.copy(alpha = 0.0f)),
                    ),
                ),
        )
    }
}
