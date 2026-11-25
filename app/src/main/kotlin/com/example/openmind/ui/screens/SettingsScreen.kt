package com.example.openmind.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.openmind.BuildConfig
import com.example.openmind.data.BlockEvent
import com.example.openmind.data.Settings
import com.example.openmind.ui.PermissionState
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.EyeIcon
import com.example.openmind.ui.components.EyeMark
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.IconTile
import com.example.openmind.ui.components.NavRow
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.Revealed
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SegmentedControl
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.ToggleRow
import com.example.openmind.ui.components.eyePress
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.Danger
import com.example.openmind.ui.theme.EyeShape
import com.example.openmind.ui.theme.EyeSpace
import com.example.openmind.ui.theme.MinTouch
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary

/**
 * Settings is the one screen where a person is in a deliberate mood, so it can be dense.
 *
 * What it must never be is ambiguous: every switch says what it governs, every permission says
 * whether it is already given, and the privacy text is the literal truth rather than a reassurance.
 * The order is the order of consequence - what runs, what it may look at, what it keeps.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    events: List<BlockEvent>,
    permissions: PermissionState,
    onMaster: (Boolean) -> Unit,
    onStrict: (Boolean) -> Unit,
    onThemeMode: (String) -> Unit,
    onTracking: (Boolean) -> Unit,
    onAmbientTimer: (Boolean) -> Unit,
    onOpenShields: () -> Unit,
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onExport: () -> String,
    onClear: () -> Unit,
    onShowOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var showDisclosure by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = EyeSpace.screen,
            top = EyeSpace.lg,
            end = EyeSpace.screen,
            bottom = EyeSpace.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(EyeSpace.between),
    ) {
        item { ScreenHeader(title = "settings", subtitle = "no account. no cloud.", onBack = onBack) }

        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = EyeSpace.lg, vertical = EyeSpace.sm)) {
                Column {
                    ToggleRow(
                        title = "shielding",
                        subtitle = "runs all the others.",
                        checked = settings.masterEnabled,
                        onCheckedChange = onMaster,
                        icon = "shield",
                    )
                    SettingDivider()
                    ToggleRow(
                        title = "strict mode",
                        subtitle = "no way through.",
                        checked = settings.strictMode,
                        onCheckedChange = onStrict,
                        icon = "lock",
                    )
                    SettingDivider()
                    ToggleRow(
                        title = "count the scroll time",
                        subtitle = "reels and shorts, on this phone.",
                        checked = settings.trackingEnabled,
                        onCheckedChange = onTracking,
                        icon = "chart",
                    )
                    SettingDivider()
                    ToggleRow(
                        title = "corner timer",
                        subtitle = "today's screen time, top left. off inside feeds.",
                        checked = settings.ambientTimer,
                        onCheckedChange = onAmbientTimer,
                        icon = "clock",
                    )
                }
            }
        }

        item { SectionLabel("appearance") }
        item {
            SegmentedControl(
                options = listOf("system", "dark", "light"),
                selectedIndex = listOf("system", "dark", "light").indexOf(settings.themeMode).coerceAtLeast(0),
                onSelect = { onThemeMode(listOf("system", "dark", "light")[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(0.dp)) {
                NavRow(
                    title = "daily limits",
                    onClick = onOpenShields,
                    subtitle = "set per app in shields",
                    icon = "target",
                )
            }
        }

        item { SectionLabel("permissions") }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = EyeSpace.md, vertical = EyeSpace.xs)) {
                Column {
                    PermissionRow(
                        icon = "shield",
                        title = "screen access",
                        subtitle = "recognises and closes feed surfaces.",
                        granted = permissions.accessibility,
                        onClick = onOpenAccessibility,
                    )
                    PermissionRow(
                        icon = "chart",
                        title = "usage access",
                        subtitle = "reads screen time for shielded apps.",
                        granted = permissions.usageAccess,
                        onClick = onOpenUsage,
                    )
                    PermissionRow(
                        icon = "bell",
                        title = "notifications",
                        subtitle = "focus and quiet-hours reminders.",
                        granted = permissions.notifications,
                        onClick = onOpenNotifications,
                    )
                }
            }
        }

        item { SectionLabel("privacy") }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
                Column {
                    Text("nothing leaves this phone.", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(EyeSpace.xs))
                    Text(
                        "no account, no tracking. your rules and history stay here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(EyeSpace.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouch)
                            .eyePress(onClick = { showDisclosure = !showDisclosure }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "what can it actually see?",
                            style = MaterialTheme.typography.labelLarge,
                            color = Accent,
                            modifier = Modifier.weight(1f),
                        )
                        EyeIcon(
                            type = if (showDisclosure) "chevron_up" else "chevron",
                            tint = Accent,
                            size = 16.dp,
                        )
                    }
                    Revealed(expanded = showDisclosure) {
                        Spacer(Modifier.height(EyeSpace.sm))
                        Text(
                            "Window, content and scroll events are filtered by package. Other apps' window changes are used only to remove overlays. For Instagram, YouTube, Facebook, TikTok and Snapchat it reads a bounded tree (max 400 nodes), finds tab labels such as Reels, Explore, Shorts, For You or Spotlight, measures the item with getBoundsInScreen and covers it with a touch-blocking accessibility overlay. If the surface opens another way, it presses Back and shows the block screen. It never reads editable text, passwords or messages, and never stores page content.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        item { SectionLabel("data") }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("local history", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${events.size} events kept for 30 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                            )
                        }
                        Pill("on device", color = AccentDim)
                    }
                    Spacer(Modifier.height(EyeSpace.md))
                    GhostButton(
                        text = "share a summary",
                        onClick = {
                            val text = onExport()
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Openeye summary")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(share, "share summary"))
                        },
                        icon = "share",
                    )
                    Spacer(Modifier.height(EyeSpace.sm))
                    GhostButton(
                        text = "clear all local data",
                        onClick = { confirmClear = true },
                        color = Danger,
                        icon = "close",
                    )
                }
            }
        }

        item { SectionLabel("about") }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EyeMark(modifier = Modifier.size(26.dp), color = Accent)
                        Spacer(Modifier.width(EyeSpace.md))
                        Text("openeye", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                        )
                    }
                    Spacer(Modifier.height(EyeSpace.md))
                    Text(
                        "counts short-video time, covers the buttons that pull you in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(EyeSpace.md))
                    Text(
                        "build by Dharmendra",
                        style = MaterialTheme.typography.titleMedium,
                        color = Accent,
                    )
                    Spacer(Modifier.height(EyeSpace.lg))
                    Divider()
                    Spacer(Modifier.height(EyeSpace.lg))
                    AboutLine("no account, no ads, no internet permission.")
                    AboutLine("the accessibility service sees which feed is open and covers a button. it never reads what you type.")
                    AboutLine("widget: pick \"openeye today\" from your launcher's widget picker.")
                    Spacer(Modifier.height(EyeSpace.md))
                    Text(
                        "build ${BuildConfig.VERSION_CODE} · revisit setup",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouch)
                            .eyePress(onClick = onShowOnboarding),
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = SurfaceOne,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("this clears everything.") },
            text = { Text("shields, schedule, focus and history all go. no undo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text("clear", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("keep", color = TextSecondary) }
            },
        )
    }
}

@Composable
private fun SettingDivider() {
    Spacer(Modifier.height(EyeSpace.xxs))
    Divider(modifier = Modifier.padding(start = 46.dp))
    Spacer(Modifier.height(EyeSpace.xxs))
}

@Composable
private fun AboutLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("·", color = AccentDim, modifier = Modifier.width(EyeSpace.lg))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
    }
    Spacer(Modifier.height(EyeSpace.xs))
}

@Composable
private fun PermissionRow(
    icon: String,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouch)
            .clip(RoundedCornerShape(EyeShape.field))
            .eyePress(onClick = onClick)
            .padding(horizontal = EyeSpace.sm, vertical = EyeSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(type = icon, tint = if (granted) Accent else TextTertiary, size = 36.dp)
        Spacer(Modifier.width(EyeSpace.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Spacer(Modifier.width(EyeSpace.sm))
        Pill(
            text = if (granted) "allowed" else "enable",
            color = if (granted) Accent else TextSecondary,
            filled = granted,
        )
        Spacer(Modifier.width(EyeSpace.xs))
        EyeIcon("chevron", tint = TextTertiary, size = 15.dp)
    }
}
