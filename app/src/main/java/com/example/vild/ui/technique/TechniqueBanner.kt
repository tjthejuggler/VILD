package com.example.vild.ui.technique

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vild.data.TechniqueItem
import com.example.vild.ui.dream.GlassCard
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.MoonLavender

/**
 * A glass banner showing a random reality check technique — an idea for
 * *how* to test whether you're dreaming. The ✧ glyph and aurora accents
 * are its voice; the advice banner is its rose-tinted sibling.
 *
 * Arrows step through techniques (‹ previous · next ›); swiping works too.
 * Up to 5 visible lines; longer text scrolls silently.
 */
@Composable
fun TechniqueBanner(
    techniques: List<TechniqueItem>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (techniques.isEmpty()) return

    val technique = techniques.getOrNull(currentIndex) ?: return

    // Track swipe direction for animation
    var swipeDirection by remember { mutableIntStateOf(0) }

    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        AnimatedContent(
            targetState = technique.id to technique.text,
            transitionSpec = {
                if (swipeDirection >= 0) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "technique_banner",
        ) { (_, text) ->
            var dragTotal by remember { mutableFloatStateOf(0f) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragTotal > 80f) {
                                    // Swiped right → previous
                                    swipeDirection = -1
                                    onPrevious()
                                } else if (dragTotal < -80f) {
                                    // Swiped left → next random
                                    swipeDirection = 1
                                    onNext()
                                }
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f },
                        ) { _, dragAmount ->
                            dragTotal += dragAmount
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArrowGlyph(
                    glyph = "‹",
                    tint = AuroraTeal,
                    onClick = {
                        swipeDirection = -1
                        onPrevious()
                    },
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 110.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "✧  reality check idea  ✧",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuroraTeal.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp,
                        ),
                        color = MoonLavender,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }

                ArrowGlyph(
                    glyph = "›",
                    tint = AuroraTeal,
                    onClick = {
                        swipeDirection = 1
                        onNext()
                    },
                )
            }
        }
    }
}

/** A soft tappable arrow — ‹ goes back, › goes forward. */
@Composable
private fun ArrowGlyph(
    glyph: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Text(
        text = glyph,
        style = MaterialTheme.typography.titleLarge,
        color = tint.copy(alpha = 0.55f),
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
