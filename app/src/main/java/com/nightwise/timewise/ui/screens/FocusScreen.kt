package com.nightwise.timewise.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.challenge.ChallengeScheduler
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.challenge.ActiveChallenge
import com.nightwise.timewise.data.challenge.Badge
import com.nightwise.timewise.data.challenge.ChallengeLogic
import com.nightwise.timewise.data.challenge.ChallengeOutcome
import com.nightwise.timewise.data.challenge.ChallengeProgress
import com.nightwise.timewise.data.challenge.ChallengeRepository
import com.nightwise.timewise.data.challenge.ChallengeResult
import com.nightwise.timewise.data.challenge.ChallengeState
import com.nightwise.timewise.data.challenge.ChallengeType
import com.nightwise.timewise.data.friction.FrictionLogic
import com.nightwise.timewise.data.friction.FrictionRepository
import com.nightwise.timewise.data.friction.FrictionStep
import com.nightwise.timewise.data.limits.AppLimitRepository
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.FocusToggle
import com.nightwise.timewise.ui.components.LimitDialog
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.SmallActionButton
import androidx.compose.ui.graphics.vector.ImageVector
import com.nightwise.timewise.ui.components.OutlineButton
import com.nightwise.timewise.ui.components.PrimaryButton
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.UsageAccessCard
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.formatCountdown
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatLimitMinutes
import com.nightwise.timewise.ui.formatTime
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.RedTint
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TICK_MS = 1_000L
private const val PROGRESS_EVERY_TICKS = 20
private const val RECENT_RESULTS = 5

/** Runs the challenge lifecycle (polling, scoring, alarms) around [FocusScreen]. */
@Composable
fun FocusRoute(
    usageAccessGranted: Boolean,
    notificationsGranted: Boolean,
    accessibilityEnabled: Boolean,
    todayApps: List<AppUsage>,
    dailyAverages: Map<String, Long>,
    onAllowUsageAccess: () -> Unit,
    onAllowNotifications: () -> Unit,
    onEnableAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenLimitPicker: () -> Unit,
    onOpenFrictionSetup: () -> Unit,
    onOpenAwareness: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChallengeRepository.get(context) }
    val state by repository.state.collectAsState()
    val limitsRepository = remember { AppLimitRepository.get(context) }
    val limits by limitsRepository.state.collectAsState()
    val frictionContent by remember { FrictionRepository.get(context) }.content.collectAsState()
    val focusRepository = remember { FocusRepository.get(context) }
    val settings by focusRepository.settings.collectAsState()
    var editingLimit by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var progress by remember { mutableStateOf<ChallengeProgress?>(null) }
    var outcome by remember { mutableStateOf<ChallengeOutcome?>(null) }

    val active = state.active
    LaunchedEffect(active) {
        progress = null
        if (active == null) return@LaunchedEffect
        var tick = 0
        while (true) {
            now = System.currentTimeMillis()
            if (tick % PROGRESS_EVERY_TICKS == 0 || now >= active.endAt) {
                val latest = withContext(Dispatchers.IO) { repository.progress(active, now) }
                progress = latest
                val due = now >= active.endAt || !ChallengeLogic.isSuccess(active.type, latest.distractionMillis)
                if (due) {
                    val finished = withContext(Dispatchers.IO) { repository.evaluate(now) }
                    ChallengeScheduler.cancel(context)
                    outcome = finished
                    break
                }
            }
            tick++
            delay(TICK_MS)
        }
    }

    FocusScreen(
        state = state,
        now = now,
        progress = progress,
        usageAccessGranted = usageAccessGranted,
        notificationsGranted = notificationsGranted,
        onAllowUsageAccess = onAllowUsageAccess,
        onAllowNotifications = onAllowNotifications,
        onStart = { type ->
            val started = repository.start(type)
            ChallengeScheduler.schedule(context, started)
            if (!notificationsGranted) onAllowNotifications()
        },
        onGiveUp = {
            scope.launch {
                val ended = withContext(Dispatchers.IO) { repository.giveUp() }
                ChallengeScheduler.cancel(context)
                outcome = ended
            }
        },
        floatingTimer = {
            FloatingTimerRow(
                enabled = settings.floatingTimerEnabled,
                accessibilityEnabled = accessibilityEnabled,
                onToggle = { on -> focusRepository.updateSettings { it.copy(floatingTimerEnabled = on) } },
                onEnableAccessibility = onEnableAccessibility
            )
        },
        appLimits = {
            AppLimitsSection(
                state = limits,
                todayApps = todayApps,
                accessibilityEnabled = accessibilityEnabled,
                frictionReadySteps = FrictionStep.entries.count { FrictionLogic.isReady(it, frictionContent) },
                frictionTotalSteps = FrictionStep.entries.size,
                onToggleEnabled = limitsRepository::setEnabled,
                onAddLimit = onOpenLimitPicker,
                onEditLimit = { editingLimit = it },
                onEnableAccessibility = onEnableAccessibility,
                onOpenAppInfo = onOpenAppInfo,
                onOpenFrictionSetup = onOpenFrictionSetup,
                onOpenAwareness = onOpenAwareness
            )
        }
    )

    outcome?.let { OutcomeDialog(it, onDismiss = { outcome = null }) }
    editingLimit?.let { pkg ->
        LimitDialog(
            packageName = pkg,
            initialMinutes = limits.limitFor(pkg)?.minutes ?: LimitLogic.DEFAULT_LIMIT_MINUTES,
            averageMillis = dailyAverages[pkg],
            onSave = { limitsRepository.setLimit(pkg, it) },
            onRemove = { limitsRepository.removeLimit(pkg) },
            onDismiss = { editingLimit = null }
        )
    }
}

@Composable
fun FocusScreen(
    state: ChallengeState,
    now: Long,
    progress: ChallengeProgress?,
    usageAccessGranted: Boolean,
    notificationsGranted: Boolean,
    onAllowUsageAccess: () -> Unit,
    onAllowNotifications: () -> Unit,
    onStart: (ChallengeType) -> Unit,
    onGiveUp: () -> Unit,
    floatingTimer: @Composable () -> Unit = {},
    appLimits: @Composable () -> Unit = {}
) {
    val today = FocusRepository.currentEpochDay(now)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Focus")

        if (!usageAccessGranted) {
            UsageAccessCard(onAllow = onAllowUsageAccess)
            return@Column
        }

        val active = state.active
        if (active != null) {
            ActiveChallengeCard(active, now, progress, onGiveUp)
            floatingTimer()
            if (!notificationsGranted) {
                Text(
                    text = "Turn on notifications to hear when the challenge ends.",
                    color = Amber,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusCard(background = AmberTint, bordered = false, onClick = onAllowNotifications)
                        .padding(12.dp)
                )
            }
        }
        SectionTitle("Challenges")
        val badgesLeft = ChallengeLogic.earnedBadges(state.history).size < Badge.entries.size
        ChallengeType.entries.forEach { type ->
            ChallengeCard(type, active, now, showBadgeHint = badgesLeft, onStart = { onStart(type) })
        }

        SectionTitle("App limits")
        appLimits()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryStat(
                label = "Current streak",
                value = ChallengeLogic.currentStreak(state.history, today).let { if (it == 1) "1 day" else "$it days" },
                modifier = Modifier.weight(1f)
            )
            SummaryStat(
                label = "Completed",
                value = state.history.count { it.success }.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle("Badges")
        BadgeGrid(earned = ChallengeLogic.earnedBadges(state.history))

        if (state.history.isNotEmpty()) {
            SectionTitle("Recent")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusCard()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.history.take(RECENT_RESULTS).forEach { ResultRow(it, today) }
            }
        }
    }
}

@Composable
private fun ActiveChallengeCard(active: ActiveChallenge, now: Long, progress: ChallengeProgress?, onGiveUp: () -> Unit) {
    val context = LocalContext.current
    val remaining = active.remainingMillis(now)
    val elapsedFraction = active.elapsedFraction(now)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = active.type.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { elapsedFraction.coerceIn(0f, 1f) },
                modifier = Modifier.size(170.dp),
                color = Forest,
                trackColor = StepperBg,
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = formatCountdown(remaining), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(text = "left", color = Slate, fontSize = 12.sp)
            }
        }
        Text(
            text = "Ends ${endLabel(context, active.endAt, now)}",
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Put your phone down. More than ${formatLimitMinutes(active.type.graceMinutes)} in other apps ends the challenge.",
            color = Slate,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
        DistractionMeter(progress, active.type.graceMillis)
        OutlineButton(onClick = onGiveUp) {
            Text(text = "Give up", color = Red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Turns the floating timer over other apps on or off; it's drawn by the accessibility service. */
@Composable
private fun FloatingTimerRow(
    enabled: Boolean,
    accessibilityEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEnableAccessibility: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Floating timer", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (accessibilityEnabled) {
                    "Time left and other-app time, shown over every app. Drag to move, tap to open TimeWise."
                } else {
                    "Turn on TimeWise in Accessibility to show the timer over other apps."
                },
                color = Slate,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (accessibilityEnabled) {
            FocusToggle(checked = enabled, onCheckedChange = onToggle)
        } else {
            SmallActionButton(text = "Allow", onClick = onEnableAccessibility)
        }
    }
}

@Composable
private fun DistractionMeter(progress: ChallengeProgress?, graceMillis: Long) {
    val used = progress?.distractionMillis ?: 0L
    val fraction = (used.toFloat() / graceMillis).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Other apps", modifier = Modifier.weight(1f), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (progress == null) "Checking…" else "${formatDuration(used)} of ${formatDuration(graceMillis)}",
                color = Slate,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(StepperBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (fraction >= 0.8f) Red else Forest)
            )
        }
        progress?.topApp?.let { top ->
            val info = rememberAppInfo(top.packageName)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(info, size = 18.dp)
                Text(text = "Most of it in ${info?.label ?: "another app"}", color = Mist, fontSize = 11.sp)
            }
        }
    }
}

/** Icon for each challenge, so the list is easy to scan. */
private fun challengeIcon(type: ChallengeType): ImageVector = when (type) {
    ChallengeType.ONE_HOUR -> Icons.Rounded.Timer
    ChallengeType.EIGHT_HOURS -> Icons.Rounded.WbSunny
    ChallengeType.ONE_DAY -> Icons.Rounded.CalendarToday
}

/**
 * One challenge as a compact row: icon, title, a short line, and a Start button. Running ones
 * show the countdown below; another one running greys this one out.
 */
@Composable
private fun ChallengeCard(type: ChallengeType, active: ActiveChallenge?, now: Long, showBadgeHint: Boolean, onStart: () -> Unit) {
    val context = LocalContext.current
    val running = active?.type == type
    val blocked = active != null && !running
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(background = if (running) Mint else CardWhite, bordered = !running)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(
                icon = challengeIcon(type),
                tint = if (blocked) Mist else if (running) Color.White else Forest,
                background = if (running) Forest else Mint,
                size = 36.dp,
                iconSize = 18.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = type.label, color = if (blocked) Mist else Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Allow ${formatLimitMinutes(type.graceMinutes)} outside apps",
                    color = Slate,
                    fontSize = 12.sp
                )
                if (showBadgeHint && !blocked) {
                    Row(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberTint)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(12.dp))
                        Text(text = "Earn a badge", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            when {
                running -> Text(
                    text = "Running",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Forest)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                !blocked -> SmallActionButton(text = "Start", onClick = onStart)
            }
        }
        when {
            active != null && running -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Time left", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(text = formatCountdown(active.remainingMillis(now)), color = Forest, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "Ends ${endLabel(context, active.endAt, now)}", color = Slate, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(active.elapsedFraction(now).coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Forest)
                    )
                }
            }
            blocked -> Text(
                text = "One challenge at a time. Finish or give up the ${active.type.label} first.",
                color = Mist,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            else -> Unit
        }
    }
}

/** "at 7:42 PM", or "tomorrow at 7:42 PM" for challenges that run past midnight. */
private fun endLabel(context: Context, endAt: Long, now: Long): String {
    val days = FocusRepository.currentEpochDay(endAt) - FocusRepository.currentEpochDay(now)
    val time = formatTime(context, endAt)
    return if (days > 0) "tomorrow at $time" else "at $time"
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BadgeGrid(earned: Set<Badge>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Badge.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { badge -> BadgeTile(badge, badge in earned, Modifier.weight(1f)) }
                if (row.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: Badge, earned: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (earned) Mint else NeutralChip),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (earned) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (earned) Forest else Mist,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(text = badge.title, color = if (earned) Ink else Slate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(text = badge.description, color = Mist, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ResultRow(result: ChallengeResult, today: Long) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = result.type.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${relativeDayLabel(result.epochDay, today)} · ${formatTime(context, result.startAt)}",
                color = Slate,
                fontSize = 12.sp
            )
        }
        Text(
            text = if (result.success) "Completed" else "Failed",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (result.success) Mint else RedTint)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            color = if (result.success) Forest else Red,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OutcomeDialog(outcome: ChallengeOutcome, onDismiss: () -> Unit) {
    val success = outcome.result.success
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = Forest) } },
        icon = {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = if (success) Forest else Mist
            )
        },
        title = { Text(if (success) "Challenge complete!" else "Challenge failed") },
        text = {
            val details = if (success) {
                "You stayed focused for ${formatDuration(outcome.result.type.durationMillis)}."
            } else {
                "${formatDuration(outcome.progress.distractionMillis)} in other apps. Every attempt builds the habit, so try again."
            }
            val badges = if (outcome.newBadges.isEmpty()) "" else
                "\n\nNew badge: ${outcome.newBadges.joinToString { it.title }}"
            Text(details + badges)
        }
    )
}
