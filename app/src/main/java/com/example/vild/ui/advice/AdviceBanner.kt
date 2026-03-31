package com.example.vild.ui.advice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vild.data.AdviceItem
import com.example.vild.ui.theme.Grey15

/**
 * A thin banner that shows a random piece of advice for the given [section].
 * Swipe left → next random advice, swipe right → previous advice.
 * Hidden when no advice exists for the section.
 *
 * Shows up to 5 visible lines; longer text is silently scrollable with no
 * visible scrollbar so the UI stays clean.
 */
@Composable
fun AdviceBanner(
    section: String,
    adviceList: List<AdviceItem>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (adviceList.isEmpty()) return

    val advice = adviceList.getOrNull(currentIndex) ?: return

    // Track swipe direction for animation
    var swipeDirection by remember { mutableIntStateOf(0) }

    // 5 lines × 16sp lineHeight ≈ 80dp content + 12dp vertical padding = ~92dp max
    val maxBannerHeight = 92.dp

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

        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = maxBannerHeight)
                .background(Grey15.copy(alpha = 0.85f))
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 16.sp,
                ),
                color = Color(0xFFD0D0D0),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
