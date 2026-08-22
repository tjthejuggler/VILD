package com.example.vild

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vild.data.AdviceItem
import com.example.vild.ui.advice.AdviceBanner
import com.example.vild.ui.advice.AdviceNotesDialog
import com.example.vild.ui.dream.DreamBackground
import com.example.vild.ui.dream.GlassCard
import com.example.vild.ui.dream.rememberTiltState
import com.example.vild.ui.realitycheck.RealityCheckCard
import com.example.vild.ui.settings.SettingsScreen
import com.example.vild.ui.stats.StatsScreen
import com.example.vild.ui.technique.TechniqueBanner
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.Mist
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.StarGold
import com.example.vild.ui.theme.VILDTheme
import com.example.vild.ui.theme.Void

/** The three screens of the app, floating over the same dream sky. */
private sealed interface Screen {
    data object Main : Screen
    data object Stats : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            VILDTheme {
                VildApp()
            }
        }
    }
}

@Composable
fun VildApp(vm: MainViewModel = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            // Dreams cross-fade into one another.
            fadeIn(tween(600)) togetherWith fadeOut(tween(300))
        },
        label = "screen",
    ) { current ->
        when (current) {
            Screen.Main -> MainScreen(
                vm = vm,
                onOpenStats = { screen = Screen.Stats },
                onOpenSettings = { screen = Screen.Settings },
            )
            Screen.Stats -> {
                val logs by vm.allLogs.collectAsState()
                val stats by vm.stats.collectAsState()
                StatsScreen(
                    logs = logs,
                    stats = stats,
                    onBack = { screen = Screen.Main },
                )
            }
            Screen.Settings -> SettingsScreen(
                vm = vm,
                onBack = { screen = Screen.Main },
            )
        }
    }
}

// ── Main (dream) screen ───────────────────────────────────────────────────────

/**
 * The main screen: today's reality check floats front and center the moment
 * the app opens, over a living accelerometer-parallaxed dream sky. Vibration
 * settings live on the secondary settings screen.
 */
@Composable
private fun MainScreen(
    vm: MainViewModel,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val todayLog by vm.todayLog.collectAsState()
    val stats by vm.stats.collectAsState()
    val activeMode by vm.activeMode.collectAsState()
    val adviceState by vm.adviceState.collectAsState()
    val techniqueState by vm.techniqueState.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()

    var notesAdvice by remember { mutableStateOf<AdviceItem?>(null) }

    val tilt = rememberTiltState()
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        DreamBackground(tilt = tilt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header: title + portals to stats & settings ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VILD",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MoonLavender,
                    )
                    Text(
                        text = "lucid · aware · dreaming",
                        style = MaterialTheme.typography.labelSmall,
                        color = Mist,
                    )
                }
                IconButton(onClick = onOpenStats) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Dream stats",
                        tint = StarGold,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MoonLavender,
                    )
                }
            }

            // ── ★ Today's reality check — the heart of the app ─────────────────
            RealityCheckCard(
                log = todayLog,
                readStreak = stats.currentReadStreak,
                doneStreak = stats.currentDoneStreak,
                onMarkRead = { vm.markRead() },
                onMarkDone = { vm.markDone() },
            )

            // ── Reality check ideas — how to actually test it ──────────────────
            if (techniqueState.techniques.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "REALITY CHECK IDEAS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Mist,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    TechniqueBanner(
                        techniques = techniqueState.techniques,
                        currentIndex = techniqueState.currentIndex,
                        onNext = { vm.nextRandomTechnique() },
                        onPrevious = { vm.previousTechnique() },
                    )
                }
            }

            // ── Advice whisper ─────────────────────────────────────────────────
            val currentSection = activeMode
            val adviceList = adviceState.adviceBySection[currentSection] ?: emptyList()
            val currentIndex = adviceState.currentIndex[currentSection] ?: 0
            if (adviceList.isNotEmpty()) {
                AdviceBanner(
                    section = currentSection,
                    adviceList = adviceList,
                    currentIndex = currentIndex,
                    onNext = { vm.nextRandomAdvice(currentSection) },
                    onPrevious = { vm.previousAdvice(currentSection) },
                    onTap = { item -> notesAdvice = item },
                )
            }

            notesAdvice?.let { item ->
                AdviceNotesDialog(
                    advice = item,
                    onSave = { notes -> vm.updateAdviceNotes(item.id, notes) },
                    onDismiss = { notesAdvice = null },
                )
            }

            // ── Day / Night ────────────────────────────────────────────────────
            DreamDayNightToggle(activeMode = activeMode, onToggle = { vm.toggleMode() })

            // ── Sync status (subtle whisper) ───────────────────────────────────
            if (syncStatus.lastSyncTimestamp != 0L) {
                val secondsAgo =
                    ((System.currentTimeMillis() - syncStatus.lastSyncTimestamp) / 1_000).toInt()
                Text(
                    text = if (syncStatus.lastSyncSuccess) {
                        "✧ watch synced ${secondsAgo}s ago"
                    } else {
                        "✧ watch sync failed"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (syncStatus.lastSyncSuccess) Mist else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Footer ─────────────────────────────────────────────────────────
            Text(
                text = "sleep deep · dream aware",
                style = MaterialTheme.typography.labelSmall,
                color = Mist.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Dream-styled Day/Night toggle: a glass pill with a sliding celestial body.
 * Sun ☀ for day, moon ☾ for night.
 */
@Composable
private fun DreamDayNightToggle(activeMode: String, onToggle: () -> Unit) {
    val isDay = activeMode == "day"

    GlassCard(cornerRadius = 50.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ToggleHalf(
                label = "☀  Day",
                active = isDay,
                activeColor = StarGold,
                onClick = { if (!isDay) onToggle() },
                modifier = Modifier.weight(1f),
            )
            ToggleHalf(
                label = "☾  Night",
                active = !isDay,
                activeColor = AuroraTeal,
                onClick = { if (isDay) onToggle() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToggleHalf(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (active) {
        Brush.linearGradient(listOf(activeColor.copy(alpha = 0.28f), activeColor.copy(alpha = 0.10f)))
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .background(container, RoundedCornerShape(50))
                .padding(vertical = 10.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) activeColor else Mist,
            )
        }
    }
}
