package com.nightwise.timewise.ui.screens

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
import androidx.compose.material.icons.rounded.EmojiEvents
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
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.OutlineButton
import com.nightwise.timewise.ui.components.PrimaryButton
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.UsageAccessCard
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatTime
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
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
    onAllowUsageAccess: () -> Unit,
    onAllowNotifications: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChallengeRepository.get(context) }
    val state by repository.state.collectAsState()
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
        }
    )

    outcome?.let { OutcomeDialog(it, onDismiss = { outcome = null }) }
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
    onGiveUp: () -> Unit
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
        } else {
            SectionTitle("Challenges")
            ChallengeType.entries.forEach { type -> ChallengeCard(type, onStart = { onStart(type) }) }
        }

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
    val remaining = (active.endAt - now).coerceAtLeast(0L)
    val elapsedFraction = 1f - remaining.toFloat() / active.type.durationMillis
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
            text = "Put your phone down. More than ${formatDuration(active.type.graceMillis)} in other apps ends the challenge.",
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

@Composable
private fun ChallengeCard(type: ChallengeType, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = type.label, color = if (type.available) Ink else Mist, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Stay off your phone for ${formatDuration(type.durationMillis)}. Up to ${formatDuration(type.graceMillis)} in other apps is OK.",
                    color = Slate,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            if (!type.available) {
                Text(
                    text = "Soon",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeutralChip)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Slate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (type.available) PrimaryButton(text = "Start", onClick = onStart, showArrow = true)
    }
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

/** "59:12" or "1:02:03". */
private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mmss = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    return if (hours > 0) "$hours:$mmss" else mmss
}
