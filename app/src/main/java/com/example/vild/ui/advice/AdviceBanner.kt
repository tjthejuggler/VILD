package com.example.vild.ui.advice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.vild.data.AdviceItem
import com.example.vild.ui.dream.GlassCard
import com.example.vild.ui.theme.DreamPink
import com.example.vild.ui.theme.Mist

/**
 * A glass banner whispering a random piece of advice for the given [section].
 * A sibling of the technique banner — same glass skin, different soul: the
 * ✦ glyph and rose accents belong to the advice voice.
 *
 * Arrows step through advice (‹ previous · next ›); swiping works too.
 * Tap the card → opens the notes dialog for the shown advice.
 * Hidden when no advice exists for the section. Longer text scrolls silently.
 */
@Composable
fun AdviceBanner(
    section: String,
    adviceList: List<AdviceItem>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTap: (AdviceItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (adviceList.isEmpty()) return

    val advice = adviceList.getOrNull(currentIndex) ?: return

    // Track swipe direction for animation
    var swipeDirection by remember { mutableIntStateOf(0) }

    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        AnimatedContent(
            targetState = advice.id to advice.text,
            transitionSpec = {
                if (swipeDirection >= 0) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "advice_banner",
        ) { (_, text) ->
            var dragTotal by remember { mutableFloatStateOf(0f) }

            @OptIn(ExperimentalFoundationApi::class)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onTap(advice) },
                    )
                    .pointerInput(section) {
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
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArrowGlyph(
                    glyph = "‹",
                    tint = DreamPink,
                    onClick = {
                        swipeDirection = -1
                        onPrevious()
                    },
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 92.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "✦  advice  ✦",
                        style = MaterialTheme.typography.labelSmall,
                        color = DreamPink.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 16.sp,
                        ),
                        color = Mist,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }

                ArrowGlyph(
                    glyph = "›",
                    tint = DreamPink,
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
