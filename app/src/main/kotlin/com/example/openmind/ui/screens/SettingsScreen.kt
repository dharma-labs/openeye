package com.example.openmind.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.openmind.BuildConfig
import com.example.openmind.data.BlockEvent
import com.example.openmind.data.Settings
import com.example.openmind.ui.PermissionState
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.OMSwitch
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.ToggleRow
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.SurfaceTwo
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary

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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
    ) {
        item { ScreenHeader(title = "settings", subtitle = "no account. no cloud.", onBack = onBack) }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    ToggleRow(
                        title = "shielding",
                        subtitle = "runs all the others.",
                        checked = settings.masterEnabled,
                        onCheckedChange = onMaster,
                    )
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    ToggleRow(
                        title = "strict mode",
                        subtitle = "no way through.",
                        checked = settings.strictMode,
                        onCheckedChange = onStrict,
                    )
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    ToggleRow(
                        title = "count the scroll time",
                        subtitle = "counts reels and shorts, on this phone.",
                        checked = settings.trackingEnabled,
                        onCheckedChange = onTracking,
                    )
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    ToggleRow(
                        title = "corner timer",
                        subtitle = "today's screen time, top left. off inside feeds.",
                        checked = settings.ambientTimer,
                        onCheckedChange = onAmbientTimer,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("appearance") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("system" to "system", "dark" to "dark", "light" to "light").forEach { (label, value) ->
                        val selected = settings.themeMode == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) Accent else SurfaceOne, RoundedCornerShape(100.dp))
                                .border(1.dp, if (selected) Accent else Hairline, RoundedCornerShape(100.dp))
                                .clickable { onThemeMode(value) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) com.example.openmind.ui.theme.Ink else TextSecondary,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenShields),
                padding = PaddingValues(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("daily limits", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text("shields", style = MaterialTheme.typography.labelMedium, color = Accent)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("permissions") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(4.dp)) {
                Column {
                    PermissionRow(
                        title = "screen access",
                        subtitle = "recognises and closes feed surfaces.",
                        granted = permissions.accessibility,
                        onClick = onOpenAccessibility,
                    )
                    Divider(modifier = Modifier.padding(horizontal = 14.dp))
                    PermissionRow(
                        title = "usage access",
                        subtitle = "reads screen time for shielded apps.",
                        granted = permissions.usageAccess,
                        onClick = onOpenUsage,
                    )
                    Divider(modifier = Modifier.padding(horizontal = 14.dp))
                    PermissionRow(
                        title = "notifications",
                        subtitle = "focus and quiet-hours reminders.",
                        granted = permissions.notifications,
                        onClick = onOpenNotifications,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("privacy") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    Text("nothing leaves this phone.", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "no account, no tracking. your rules and history stay here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (showDisclosure) "hide the detail" else "what can it actually see?",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        modifier = Modifier.clickable { showDisclosure = !showDisclosure },
                    )
                    if (showDisclosure) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Window, content and scroll events are filtered by package. Other apps' window changes are used only to remove overlays. For Instagram, YouTube, Facebook, TikTok and Snapchat it reads a bounded tree (max 400 nodes), finds tab labels such as Reels, Explore, Shorts, For You or Spotlight, measures the item with getBoundsInScreen and covers it with a touch-blocking accessibility overlay. If the surface opens another way, it presses Back and shows the block screen. It never reads editable text, passwords or messages, and never stores page content.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("data") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("local history", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "${events.size} events kept for 30 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                            )
                        }
                        Pill("on device", color = TextSecondary)
                    }
                    Spacer(Modifier.height(14.dp))
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
                    )
                    Spacer(Modifier.height(8.dp))
                    GhostButton(text = "clear all local data", onClick = { confirmClear = true }, color = com.example.openmind.ui.theme.Danger)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("about") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Openeye",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "counts short-video time, covers the buttons that pull you in",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "build by Dharmendra",
                        style = MaterialTheme.typography.titleMedium,
                        color = Accent,
                    )
                    Spacer(Modifier.height(14.dp))
                    Divider()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "no account, no ads, no internet permission. everything stays on this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "the accessibility service is used to see which feed is open and to cover a button. it never reads what you type.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "widget: pick \"openeye today\" from your launcher's widget picker",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "revisit setup",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        modifier = Modifier.clickable(onClick = onShowOnboarding),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
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
                }) { Text("clear", color = com.example.openmind.ui.theme.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("keep", color = TextSecondary) }
            },
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Spacer(Modifier.width(10.dp))
        Pill(
            text = if (granted) "allowed" else "enable",
            color = if (granted) Accent else TextSecondary,
            filled = granted,
        )
    }
}