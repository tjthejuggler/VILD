package com.example.vild.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vild.data.AdviceItem
import com.example.vild.ui.advice.AdviceDialog
import com.example.vild.ui.advice.AdviceSection
import com.example.vild.ui.theme.Grey15
import com.example.vild.ui.theme.Grey20
import com.example.vild.ui.theme.Grey60

/**
 * Settings screen for VILD. Currently contains only the Advice management section.
 * Shown as a full-screen overlay toggled from the main screen's gear icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    adviceBySection: Map<String, List<AdviceItem>>,
    onAddAdvice: (String, String) -> Unit,
    onUpdateAdvice: (AdviceItem, String) -> Unit,
    onDeleteAdvice: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var openAdviceSection by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Grey15,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Advice ──────────────────────────────────────────────────────
            item {
                AdviceSettingsCard(
                    adviceBySection = adviceBySection,
                    onOpenSection = { openAdviceSection = it },
                )
            }
        }
    }

    // ── Advice dialog ─────────────────────────────────────────────────────────
    openAdviceSection?.let { section ->
        AdviceDialog(
            section = section,
            adviceList = adviceBySection[section] ?: emptyList(),
            onAdd = { text -> onAddAdvice(section, text) },
            onUpdate = { item, text -> onUpdateAdvice(item, text) },
            onDelete = { id -> onDeleteAdvice(id) },
            onDismiss = { openAdviceSection = null },
        )
    }
}

// ── Advice settings card ──────────────────────────────────────────────────────

@Composable
private fun AdviceSettingsCard(
    adviceBySection: Map<String, List<AdviceItem>>,
    onOpenSection: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Grey15)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Advice",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Text(
                "Add personal reminders or tips that appear at the top of the main screen. " +
                    "Day advice shows in Day mode, Night advice shows in Night mode.",
                style = MaterialTheme.typography.bodySmall,
                color = Grey60,
            )

            HorizontalDivider(color = Grey20)

            AdviceSection.all.forEach { section ->
                val count = adviceBySection[section]?.size ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            AdviceSection.label(section),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                        Text(
                            if (count == 0) "No advice set"
                            else "$count piece${if (count != 1) "s" else ""} of advice",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (count > 0) Color(0xFF4CAF50) else Grey60,
                        )
                    }
                    OutlinedButton(
                        onClick = { onOpenSection(section) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) {
                        Text(
                            if (count > 0) "Manage" else "Add",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
