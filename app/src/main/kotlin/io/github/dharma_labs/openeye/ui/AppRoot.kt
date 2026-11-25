package io.github.dharma_labs.openeye.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dharma_labs.openeye.ui.components.EyeMark
import io.github.dharma_labs.openeye.ui.components.NavGlyph
import io.github.dharma_labs.openeye.ui.screens.FocusScreen
import io.github.dharma_labs.openeye.ui.screens.InsightsScreen
import io.github.dharma_labs.openeye.ui.screens.OnboardingScreen
import io.github.dharma_labs.openeye.ui.screens.PlanScreen
import io.github.dharma_labs.openeye.ui.screens.ScheduleScreen
import io.github.dharma_labs.openeye.ui.screens.SettingsScreen
import io.github.dharma_labs.openeye.ui.screens.ShieldsScreen
import io.github.dharma_labs.openeye.ui.screens.TodayScreen
import io.github.dharma_labs.openeye.domain.AttentionStats
import io.github.dharma_labs.openeye.ui.theme.Accent
import io.github.dharma_labs.openeye.ui.theme.AccentSoft
import io.github.dharma_labs.openeye.ui.theme.EyeElevation
import io.github.dharma_labs.openeye.ui.theme.EyeMotion
import io.github.dharma_labs.openeye.ui.theme.EyeShape
import io.github.dharma_labs.openeye.ui.theme.Hairline
import io.github.dharma_labs.openeye.ui.theme.Ink
import io.github.dharma_labs.openeye.ui.theme.Shadow
import io.github.dharma_labs.openeye.ui.theme.SurfaceOne
import io.github.dharma_labs.openeye.ui.theme.TextPrimary
import io.github.dharma_labs.openeye.ui.theme.TextSecondary
import io.github.dharma_labs.openeye.ui.theme.TextTertiary
import java.time.LocalDate
import kotlinx.coroutines.delay

private data class NavItem(val key: String, val label: String, val glyph: String)

private val navItems = listOf(
    NavItem("today", "today", "today"),
    NavItem("shields", "shields", "shields"),
    NavItem("focus", "focus", "focus"),
    NavItem("numbers", "numbers", "insights"),
)

private val navKeys = navItems.map { it.key }

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val watch by viewModel.watch.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val todayFeedMillis = remember(watch) {
        AttentionStats.forDay(watch, LocalDate.now().toEpochDay()).millis
    }

    var splashElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Long enough to read the mark and the credit, short enough not to be a wait. The real
        // work is already done by then - this window is brand, not loading.
        delay(1_100L)
        splashElapsed = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.refreshUsage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { viewModel.refreshUsage() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, Ink))),
    ) {
        if (!ready || !splashElapsed) {
            Splash()
            return@Box
        }
        if (!settings.onboardingComplete) {
            OnboardingScreen(
                permissions = permissions,
                contract = settings.contract,
                onSetStopRule = { minutes -> viewModel.signContract(minutes) },
                onSetUsualFeeling = viewModel::setUsualFeeling,
                onEnableAccessibility = { AppViewModel.openAccessibilitySettings(context) },
                onEnableUsage = { AppViewModel.openUsageSettings(context) },
                onEnableNotifications = { AppViewModel.openNotificationSettings(context) },
                onFinish = { viewModel.completeOnboarding() },
            )
            return@Box
        }

        var tab by rememberSaveable { mutableStateOf("today") }
        var overlay by rememberSaveable { mutableStateOf<String?>(null) }
        BackHandler(enabled = overlay != null) { overlay = null }

        if (overlay != null) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            ) {
                when (overlay) {
                    "plans" -> PlanScreen(
                        contract = settings.contract,
                        plans = settings.plans,
                        todayMillis = todayFeedMillis,
                        onSetRule = { minutes -> viewModel.signContract(minutes) },
                        onAddPlan = viewModel::addPlan,
                        onRemovePlan = viewModel::removePlan,
                        onTogglePlan = viewModel::setPlanEnabled,
                        onBack = { overlay = null },
                    )
                    "schedule" -> ScheduleScreen(
                        settings = settings,
                        onBack = { overlay = null },
                        onUpdate = viewModel::updateSchedule,
                    )
                    "settings" -> SettingsScreen(
                        settings = settings,
                        events = events,
                        permissions = permissions,
                        onMaster = viewModel::setMaster,
                        onStrict = viewModel::setStrict,
                        onThemeMode = viewModel::setThemeMode,
                        onTracking = viewModel::setTrackingEnabled,
                        onAmbientTimer = viewModel::setAmbientTimer,
                        onOpenAccessibility = { AppViewModel.openAccessibilitySettings(context) },
                        onOpenUsage = { AppViewModel.openUsageSettings(context) },
                        onOpenNotifications = { AppViewModel.openNotificationSettings(context) },
                        onExport = { viewModel.exportText() },
                        onClear = viewModel::clearAll,
                        onShowOnboarding = { overlay = "onboarding" },
                        onOpenShields = { overlay = null; tab = "shields" },
                        onBack = { overlay = null },
                    )
                    "onboarding" -> OnboardingScreen(
                        permissions = permissions,
                        contract = settings.contract,
                        onSetStopRule = { minutes -> viewModel.signContract(minutes) },
                        onSetUsualFeeling = viewModel::setUsualFeeling,
                        onEnableAccessibility = { AppViewModel.openAccessibilitySettings(context) },
                        onEnableUsage = { AppViewModel.openUsageSettings(context) },
                        onEnableNotifications = { AppViewModel.openNotificationSettings(context) },
                        onFinish = { viewModel.completeOnboarding(); overlay = null },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            // Direction follows where the thumb came from, so a tab never reads as a
                            // page reload. The slide is a twelfth of the screen: enough to feel aimed,
                            // not enough to make the numbers swim.
                            val forward = navKeys.indexOf(targetState) >= navKeys.indexOf(initialState)
                            val dir = if (forward) 1 else -1
                            (fadeIn(tween(EyeMotion.quick)) +
                                slideInHorizontally(tween<IntOffset>(EyeMotion.normal, easing = EyeMotion.Standard)) {
                                    dir * it / 12
                                })
                                .togetherWith(
                                    fadeOut(tween(EyeMotion.instant)) +
                                        slideOutHorizontally(tween<IntOffset>(EyeMotion.normal, easing = EyeMotion.Standard)) {
                                            -dir * it / 12
                                        },
                                )
                        },
                        label = "tab",
                    ) { current ->
                        when (current) {
                            "today" -> TodayScreen(
                                settings = settings,
                                events = events,
                                watch = watch,
                                permissions = permissions,
                                onOpenFocus = { tab = "focus" },
                                onOpenShields = { tab = "shields" },
                                onOpenSchedule = { overlay = "schedule" },
                                onOpenSettings = { overlay = "settings" },
                                onOpenInsights = { tab = "numbers" },
                                onOpenPlan = { overlay = "plans" },
                                onEnableAccessibility = { AppViewModel.openAccessibilitySettings(context) },
                                onLogFeeling = { feeling -> viewModel.logFeeling(feeling, todayFeedMillis, "today") },
                                onDismissLapse = { viewModel.markLapseHandled(LocalDate.now().toEpochDay()) },
                                onStartReentry = {
                                    viewModel.startFocus(10)
                                    tab = "focus"
                                },
                            )
                            "numbers" -> InsightsScreen(
                                settings = settings,
                                events = events,
                                watch = watch,
                                usage = usage,
                                onGrantUsage = { AppViewModel.openUsageSettings(context) },
                                onRefresh = viewModel::refreshUsage,
                                onOpenPlan = { overlay = "plans" },
                                onOpenSettings = { overlay = "settings" },
                            )
                            "shields" -> ShieldsScreen(
                                settings = settings,
                                permissions = permissions,
                                onTogglePlatform = viewModel::setPlatformEnabled,
                                onSurfaceMode = viewModel::setSurfaceMode,
                                onStrictChange = viewModel::setStrict,
                                onOpenSchedule = { overlay = "schedule" },
                                onLimitChange = { id, minutes ->
                                    viewModel.setDailyLimit(id, minutes)
                                    if (minutes > 0 && !permissions.usageAccess) {
                                        AppViewModel.openUsageSettings(context)
                                    }
                                },
                                onGrantUsage = { AppViewModel.openUsageSettings(context) },
                                onNudgeChange = viewModel::setNudgeMinutes,
                                onLongVideosChange = viewModel::setCountLongVideos,
                                onOpenSettings = { overlay = "settings" },
                            )
                            else -> FocusScreen(
                                settings = settings,
                                onBack = { tab = "today" },
                                onOpenSettings = { overlay = "settings" },
                                onStart = { minutes -> viewModel.startFocus(minutes) },
                                onStop = viewModel::stopFocus,
                            )
                        }
                    }
                }
                BottomBar(current = tab, onSelect = { tab = it })
            }
        }
    }
}

/**
 * Four places, one bar.
 *
 * The bar floats so the list can scroll under it, and the active tab gets a soft pill rather than an
 * underline: a pill is a shape the eye already reads as "you are here", an underline is something you
 * have to look at the bottom of to interpret. Labels stay visible on every tab - hiding the ones you
 * are not on saves pixels and costs recall.
 */
@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .shadow(
                elevation = EyeElevation.floating,
                shape = RoundedCornerShape(EyeShape.card),
                ambientColor = Shadow,
                spotColor = Shadow,
                clip = false,
            )
            .background(SurfaceOne, RoundedCornerShape(EyeShape.card))
            .border(1.dp, Hairline.copy(alpha = 0.7f), RoundedCornerShape(EyeShape.card))
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        navItems.forEach { item ->
            val selected = item.key == current
            val pill by animateColorAsState(
                targetValue = if (selected) AccentSoft else Color.Transparent,
                animationSpec = tween(EyeMotion.normal, easing = EyeMotion.Standard),
                label = "navpill",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = { onSelect(item.key) })
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 29.dp)
                            .background(pill, RoundedCornerShape(EyeShape.chip)),
                    )
                    NavGlyph(
                        type = item.glyph,
                        tint = if (selected) Accent else TextTertiary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Accent else TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun Splash() {
    val shimmer = rememberInfiniteTransition(label = "loading")
    // Read in the layout phase so the sweep relayouts the track instead of recomposing Splash.
    val sweep = shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Restart),
        label = "sweep",
    )
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // Most of the splash window, so the credit settles and is readable before it clears.
        enter.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(listOf(Accent.copy(alpha = 0.10f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(enter.value),
        ) {
            EyeMark(modifier = Modifier.size(84.dp), color = Accent)
            Spacer(Modifier.height(22.dp))
            Text("openeye", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(2.dp)
                    .background(TextPrimary.copy(alpha = 0.08f), RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.34f)
                        .align(Alignment.CenterStart)
                        .offset { IntOffset((96.dp * sweep.value * 0.9f).roundToPx(), 0) }
                        .width(32.dp)
                        .height(2.dp)
                        .background(Accent, RoundedCornerShape(2.dp)),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 34.dp),
        ) {
            Text(
                text = "by Dharmendra",
                style = MaterialTheme.typography.titleMedium,
                color = Accent,
                letterSpacing = 0.08.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "your attention, back on your side.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
    }
}