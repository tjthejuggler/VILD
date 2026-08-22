package com.example.vild.ui.dream

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.GlassSurface
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.Violet

/**
 * A translucent "glass" card floating over the dream background.
 * Gradient border catches the nebula light; the surface stays dark and
 * see-through so orbs and stars drift behind the content.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = GlassSurface,
            contentColor = MoonLavender,
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MoonLavender.copy(alpha = 0.35f),
                    Violet.copy(alpha = 0.15f),
                    AuroraTeal.copy(alpha = 0.25f),
                ),
            ),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

/** Soft glow color used for "confirmed" states. */
val ConfirmedGlow = AuroraTeal

/** Muted color for unconfirmed / waiting states. */
val WaitingMist = Color(0x669D94C7)
