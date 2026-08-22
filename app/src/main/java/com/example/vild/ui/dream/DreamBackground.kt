package com.example.vild.ui.dream

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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

/** Slide-speed multiplier at full lean (phone on its side). */
private const val SLIDE_GAIN = 2.55f

/** New stars that must enter the sky before it adopts their predicted color. */
private const val COLOR_CHARGE_NEEDED = 40

/** Crossfade when the sky adopts the predicted color. */
private const val HUE_FADE_MS = 3_000

/** How fast the sky bleeds to black when it shatters. */
private const val BLACKOUT_FADE_MS = 900

/** Shake envelope above which the phone counts as "being shaken". */
private const val SHAKE_TRIGGER = 0.55f

/** How long (s) the shake must persist before the sky shatters. */
private const val SHAKE_HOLD_SECONDS = 0.30f

/** Shake envelope below which the shatter trigger re-arms. */
private const val SHAKE_REARM = 0.30f

/** Outward speed of exploding stars, px per second. */
private const val BURST_SPEED = 950f

/** Edge entries per second at full lean while the sky regathers. */
private const val REGATHER_RATE = 7f

/**
 * Lifecycle of the starfield.
 *  - [STREAM] — the normal, continuous field: stars that slide off respawn
 *    from the uphill edge, each one charging the next sky color.
 *  - [BURST] — a shake shattered the sky: every star flies outward and is
 *    removed once it leaves the screen.
 *  - [REGATHER] — the field is empty; new stars drift back in through the
 *    uphill edge (faster the steeper the phone), rebuilding the charge that
 *    will relight the sky.
 */
private enum class FieldPhase { STREAM, BURST, REGATHER }

/**
 * A star sliding across the dream sky. Each star travels along a heading;
 * the phone's angle decides that heading — stars slide "downhill" along
 * whatever way the device is leaning, like dust on glass. Speed rises with
 * the steepness of the lean; on a flat phone the sky is still.
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
    var color: Color,
    var exploding: Boolean = false,
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
 *  - Stars slide along the phone's lean like dust on a pane of glass:
 *    steeper lean → faster slide, flat phone → stillness. Every star that
 *    newly enters the sky is tinted with the color the sky will wear NEXT,
 *    so fresh arrivals literally predict the next hue. Once enough new
 *    stars have drifted through, the sky melts into that color. A phone
 *    lying flat admits no new stars — and the color never changes.
 *  - Shake the phone rapidly and the sky shatters: stars explode outward
 *    and the sky bleeds to black. It stays dark — starless, colorless —
 *    until enough new stars drift back in to predict (and summon) the
 *    next color.
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

    // ── The sky's color — summoned by the stars that drift in ────────────────
    val dreamPalette = remember {
        listOf(
            AuroraTeal, DreamGreen, SkyBlue, StarGold, DreamPink,
            RoseCoral, GlacierCyan, EmberOrange, MoonLavender,
        )
    }
    // Which palette color the sky wears right now, and whether it is lit
    // at all (a shattered sky is pitch black until stars regather).
    var hueIndex by remember { mutableIntStateOf(0) }
    var skyLit by remember { mutableStateOf(true) }
    val dreamHue by animateColorAsState(
        targetValue = if (skyLit) dreamPalette[hueIndex] else Void,
        animationSpec = tween(
            if (skyLit) HUE_FADE_MS else BLACKOUT_FADE_MS,
            easing = LinearEasing,
        ),
        label = "dreamHue",
    )
    // Orbs and glow fade out with the shattered sky so black means black.
    val orbGlow by animateFloatAsState(
        targetValue = if (skyLit) 1f else 0f,
        animationSpec = tween(BLACKOUT_FADE_MS, easing = LinearEasing),
        label = "orbGlow",
    )

    // ── Sliding starfield ─────────────────────────────────────────────────────
    val currentTilt by rememberUpdatedState(tilt)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    val stars = remember { Array<DriftingStar?>(STAR_COUNT) { null } }

    LaunchedEffect(Unit) {
        var last = 0L
        // The color the arriving stars predict — the sky's next outfit.
        var nextHue = 1 + Random.nextInt(dreamPalette.size - 1)
        var colorCharge = 0
        var shakeHold = 0f
        var shakeArmed = true
        var phase = FieldPhase.STREAM
        var fieldSeeded = false

        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat()
            if (w > 0f && h > 0f) {
                // First frame with a real canvas: scatter the initial field.
                // (STREAM keeps every slot alive from here on — only REGATHER
                // refills slots, and only one star at a time.)
                if (!fieldSeeded) {
                    for (i in 0 until STAR_COUNT) stars[i] = newStar(w, h)
                    fieldSeeded = true
                }
                val tx = currentTilt.x
                val ty = currentTilt.y
                val lean = sqrt(tx * tx + ty * ty)
                // Downhill = the direction gravity pulls along the screen.
                val downhill: Float? =
                    if (lean > FLAT_THRESHOLD) atan2(ty, tx) else null

                // ── Rapid shaking shatters the sky ─────────────────────────
                val shake = currentTilt.shake
                if (shakeArmed && shake > SHAKE_TRIGGER) {
                    shakeHold += dt
                    if (shakeHold >= SHAKE_HOLD_SECONDS) {
                        val cx = w / 2f
                        val cy = h / 2f
                        for (i in 0 until STAR_COUNT) {
                            val star = stars[i] ?: continue
                            star.heading = atan2(star.y - cy, star.x - cx)
                            star.exploding = true
                        }
                        skyLit = false
                        colorCharge = 0
                        phase = FieldPhase.BURST
                        shakeArmed = false
                        shakeHold = 0f
                    }
                } else {
                    shakeHold = 0f
                    if (shake < SHAKE_REARM) shakeArmed = true
                }

                // ── While regathering: fresh stars drift back in through the
                //    uphill edge, faster the steeper the phone. Each arrival
                //    is tinted with the color it predicts. ──────────────────
                if (phase == FieldPhase.REGATHER && downhill != null &&
                    Random.nextFloat() < REGATHER_RATE * lean * dt
                ) {
                    val slot = Random.nextInt(STAR_COUNT)
                    if (stars[slot] == null) {
                        val s = newStar(w, h)
                        s.heading = downhill
                        respawnBehind(s, w, h)
                        s.color = dreamPalette[nextHue]
                        stars[slot] = s
                        colorCharge++
                    }
                }

                var alive = 0
                for (i in 0 until STAR_COUNT) {
                    val star = stars[i] ?: continue
                    alive++

                    if (star.exploding) {
                        star.x += cos(star.heading) * BURST_SPEED * dt
                        star.y += sin(star.heading) * BURST_SPEED * dt
                        if (star.x < -STAR_MARGIN || star.x > w + STAR_MARGIN ||
                            star.y < -STAR_MARGIN || star.y > h + STAR_MARGIN
                        ) {
                            stars[i] = null
                            alive--
                        }
                        continue
                    }

                    if (downhill != null) {
                        // Steer smoothly along the shortest arc toward downhill.
                        star.heading += angleDelta(star.heading, downhill) *
                            (3f * dt).coerceAtMost(1f)
                    }
                    // Steeper phone → faster slide; flat phone → stillness.
                    val speed = star.baseSpeed * (lean * SLIDE_GAIN) * star.depth
                    star.x += cos(star.heading) * speed * dt
                    star.y += sin(star.heading) * speed * dt
                    if (star.x < -STAR_MARGIN || star.x > w + STAR_MARGIN ||
                        star.y < -STAR_MARGIN || star.y > h + STAR_MARGIN
                    ) {
                        if (phase == FieldPhase.BURST) {
                            stars[i] = null
                            alive--
                        } else {
                            respawnBehind(star, w, h)
                            // Every newborn star carries the color the sky
                            // will wear next — the prediction mechanic.
                            star.color = dreamPalette[nextHue]
                            colorCharge++
                        }
                    }
                }

                if (phase == FieldPhase.BURST && alive == 0) phase = FieldPhase.REGATHER
                if (phase == FieldPhase.REGATHER && alive == STAR_COUNT) phase = FieldPhase.STREAM

                // ── Enough new stars arrived: the sky adopts their color ────
                if (colorCharge >= COLOR_CHARGE_NEEDED) {
                    hueIndex = nextHue
                    nextHue = (nextHue + 1 + Random.nextInt(dreamPalette.size - 1)) %
                        dreamPalette.size
                    colorCharge = 0
                    skyLit = true
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
            alpha = 0.30f * orbGlow,
            offsetX = currentTilt.x * -34f + driftA,
            offsetY = currentTilt.y * -34f + driftB,
            alignment = { androidx.compose.ui.Alignment.TopStart },
        )
        NebulaOrb(
            size = 520.dp,
            color = GlacierCyan,
            alpha = 0.13f * orbGlow,
            offsetX = currentTilt.x * -58f + driftB,
            offsetY = currentTilt.y * -58f + driftA,
            alignment = { androidx.compose.ui.Alignment.BottomEnd },
        )
        NebulaOrb(
            size = 360.dp,
            color = DreamPink,
            alpha = 0.15f * orbGlow,
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
                .alpha(0.22f * orbGlow)
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
