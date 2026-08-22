package com.example.vild.ui.dream

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.DreamGreen
import com.example.vild.ui.theme.DreamPink
import com.example.vild.ui.theme.EmberOrange
import com.example.vild.ui.theme.GlacierCyan
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.RoseCoral
import com.example.vild.ui.theme.SkyBlue
import com.example.vild.ui.theme.StarGold
import com.example.vild.ui.theme.Void
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** How far (px) past an edge a star may travel before it respawns. */
private const val STAR_MARGIN = 10f

/** How many stars live in the dream sky. */
private const val STAR_COUNT = 70

/** Tilt magnitude below which the phone counts as "flat". */
private const val FLAT_THRESHOLD = 0.04f

/** Hue crossfade period — the sky never stops melting into a new color. */
private const val HUE_PERIOD_MS = 11_000

/**
 * A star sliding across the dream sky. Each star travels along a heading;
 * the phone's angle decides that heading — stars slide "downhill" along
 * whatever way the device is leaning, like dust on glass. Speed rises with
 * the steepness of the lean; on a flat phone they keep drifting gently in
 * whatever direction they last had.
 */
private class DriftingStar(
    var x: Float,
    var y: Float,
    var heading: Float,
    val baseSpeed: Float,
    val radius: Float,
    val phase: Float,
    val twinkleSpeed: Float,
    val depth: Float,
    val color: Color,
)

/** Scatter a brand-new star somewhere inside the screen. */
private fun newStar(w: Float, h: Float) = DriftingStar(
    x = Random.nextFloat() * w,
    y = Random.nextFloat() * h,
    heading = Random.nextFloat() * (2f * Math.PI.toFloat()),
    baseSpeed = 10f + Random.nextFloat() * 26f,
    radius = 1.1f + Random.nextFloat() * 2.0f,
    phase = Random.nextFloat() * (2f * Math.PI.toFloat()),
    twinkleSpeed = 0.5f + Random.nextFloat() * 1.5f,
    depth = 0.5f + Random.nextFloat() * 1.0f,
    color = when (Random.nextInt(10)) {
        0 -> StarGold
        1 -> AuroraTeal
        2 -> DreamPink
        else -> MoonLavender
    },
)

/** Shortest signed angle from [from] to [to], in [-π, π]. */
private fun angleDelta(from: Float, to: Float): Float {
    val tau = 2f * Math.PI.toFloat()
    var d = (to - from) % tau
    if (d > Math.PI.toFloat()) d -= tau
    if (d < -Math.PI.toFloat()) d += tau
    return d
}

/**
 * A star slid off the screen — a new one enters from the uphill edge (the
 * edge it is travelling *away from*), so the whole field reads as one
 * continuous stream sliding down the phone's lean.
 */
private fun respawnBehind(star: DriftingStar, w: Float, h: Float) {
    val c = cos(star.heading)
    val s = sin(star.heading)
    if (abs(c) >= abs(s)) {
        if (c > 0f) {
            star.x = -STAR_MARGIN
            star.y = Random.nextFloat() * h
        } else {
            star.x = w + STAR_MARGIN
            star.y = Random.nextFloat() * h
        }
    } else {
        if (s > 0f) {
            star.y = -STAR_MARGIN
            star.x = Random.nextFloat() * w
        } else {
            star.y = h + STAR_MARGIN
            star.x = Random.nextFloat() * w
        }
    }
}

/**
 * The living background of the whole app.
 *
 *  - The sky color never settles: it constantly cross-fades through a wide
 *    spectrum of dream hues — teal, green, sky blue, gold, pink, coral,
 *    cyan, ember, lavender — so no single color dominates the dream.
 *  - Stars slide along the phone's angle, like dust gliding down a pane of
 *    glass however it is tilted. Steeper lean → faster slide; new stars
 *    keep entering from the uphill edge so the sky replenishes itself.
 *  - Nebula orbs and a breathing glow keep the depth, parallaxed by tilt.
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

    // ── The ever-melting sky hue — the full dream spectrum, none dominant ────
    val dreamPalette = remember {
        listOf(
            AuroraTeal, DreamGreen, SkyBlue, StarGold, DreamPink,
            RoseCoral, GlacierCyan, EmberOrange, MoonLavender,
        )
    }
    var hueIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(dreamPalette) {
        while (true) {
            delay(HUE_PERIOD_MS.toLong())
            // Pick a *different* hue each time — never the one we just left.
            hueIndex = (hueIndex + 1 + Random.nextInt(dreamPalette.size - 1)) % dreamPalette.size
        }
    }
    val dreamHue by animateColorAsState(
        targetValue = dreamPalette[hueIndex],
        animationSpec = tween(HUE_PERIOD_MS, easing = LinearEasing),
        label = "dreamHue",
    )

    // ── Sliding starfield ─────────────────────────────────────────────────────
    val currentTilt by rememberUpdatedState(tilt)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    val stars = remember { Array<DriftingStar?>(STAR_COUNT) { null } }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat()
            if (w > 0f && h > 0f) {
                val tx = currentTilt.x
                val ty = currentTilt.y
                val lean = sqrt(tx * tx + ty * ty)
                // Downhill = the direction gravity pulls along the screen.
                val downhill: Float? =
                    if (lean > FLAT_THRESHOLD) atan2(ty, tx) else null

                for (i in 0 until STAR_COUNT) {
                    val star = stars[i] ?: newStar(w, h).also { stars[i] = it }
                    if (downhill != null) {
                        // Steer smoothly along the shortest arc toward downhill.
                        star.heading += angleDelta(star.heading, downhill) *
                            (3f * dt).coerceAtMost(1f)
                    }
                    // Steeper phone → faster slide; flat phone → gentle drift.
                    val speed = star.baseSpeed * (0.35f + (lean * 2.4f).coerceAtMost(2.2f)) * star.depth
                    star.x += cos(star.heading) * speed * dt
                    star.y += sin(star.heading) * speed * dt
                    if (star.x < -STAR_MARGIN || star.x > w + STAR_MARGIN ||
                        star.y < -STAR_MARGIN || star.y > h + STAR_MARGIN
                    ) {
                        respawnBehind(star, w, h)
                    }
                }
            }
            frame++
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Void,
                        lerp(Void, dreamHue, 0.55f),
                        lerp(Void, dreamHue, 0.40f),
                        Void,
                    ),
                ),
            ),
    ) {
        // ── Nebula orbs (radial gradients, tilt parallax + drift) ─────────────
        NebulaOrb(
            size = 420.dp,
            color = dreamHue,
            alpha = 0.30f,
            offsetX = currentTilt.x * -34f + driftA,
            offsetY = currentTilt.y * -34f + driftB,
            alignment = { androidx.compose.ui.Alignment.TopStart },
        )
        NebulaOrb(
            size = 520.dp,
            color = GlacierCyan,
            alpha = 0.13f,
            offsetX = currentTilt.x * -58f + driftB,
            offsetY = currentTilt.y * -58f + driftA,
            alignment = { androidx.compose.ui.Alignment.BottomEnd },
        )
        NebulaOrb(
            size = 360.dp,
            color = DreamPink,
            alpha = 0.15f,
            offsetX = currentTilt.x * -22f + driftA,
            offsetY = currentTilt.y * -22f + driftB,
            alignment = { androidx.compose.ui.Alignment.Center },
        )

        // ── Breathing central glow, tinted by the current dream hue ───────────
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(androidx.compose.ui.Alignment.Center)
                .scale(breath)
                .alpha(0.22f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dreamHue.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // ── The starfield: sliding along the phone's lean, self-replenishing ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it },
        ) {
            frame // read state → the canvas repaints every animation frame
            stars.forEach { star ->
                if (star == null) return@forEach
                val twinkle = 0.35f + 0.65f * (
                    0.5f + 0.5f * sin(time * star.twinkleSpeed + star.phase)
                    )
                drawCircle(
                    color = star.color.copy(alpha = twinkle * 0.85f),
                    radius = star.radius,
                    center = Offset(star.x, star.y),
                )
            }
        }
    }
}

/** One soft nebula orb: a clipped radial gradient floating in the void. */
@Composable
private fun NebulaOrb(
    size: androidx.compose.ui.unit.Dp,
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
