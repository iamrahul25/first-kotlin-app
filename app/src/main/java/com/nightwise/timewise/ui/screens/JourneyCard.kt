package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.journey.BaselineSource
import com.nightwise.timewise.data.journey.JourneyProgress
import com.nightwise.timewise.data.journey.JourneyState
import com.nightwise.timewise.data.journey.Verdict
import com.nightwise.timewise.ui.components.ChangePill
import com.nightwise.timewise.ui.components.Trend
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.formatDate
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatTime
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.RedTint
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg

/** Whether TimeWise is helping: usage now against usage before install, plus setup milestones. */
@Composable
fun JourneyCard(journey: JourneyState, progress: JourneyProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Is TimeWise helping?", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                text = "Day ${progress.daysWithApp + 1}",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Mint)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Forest,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        VerdictBanner(progress)

        val baseline = progress.baseline
        if (baseline != null) {
            BeforeNow(before = baseline.dailyAverageMillis, now = progress.recentAverageMillis, change = progress.changePercent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val saved = progress.savedMillis
                JourneyStat(
                    label = if (saved >= 0) "Time saved" else "Extra screen time",
                    value = if (progress.recentAverageMillis == null) "—" else formatDuration(kotlin.math.abs(saved)),
                    valueColor = if (progress.recentAverageMillis == null) Ink else if (saved >= 0) Forest else Red,
                    modifier = Modifier.weight(1f)
                )
                JourneyStat(
                    label = "Unlocks a day",
                    value = when {
                        baseline.unlocksPerDay == null -> "—"
                        progress.recentUnlocksPerDay == null -> "${baseline.unlocksPerDay} before"
                        else -> "${baseline.unlocksPerDay} → ${progress.recentUnlocksPerDay}"
                    },
                    valueColor = Ink,
                    modifier = Modifier.weight(1f)
                )
            }
            if (progress.weeklyAverages.size >= 2) WeeklyChart(progress.weeklyAverages, baseline.dailyAverageMillis)
        }

        Milestones(journey)

        Text(
            text = when (baseline?.source) {
                BaselineSource.WEEK_BEFORE_INSTALL ->
                    "\"Before\" is your average over the ${baseline.days} days before you installed TimeWise, from Android's own records."
                BaselineSource.FIRST_WEEK ->
                    "\"Before\" is your first full week with TimeWise, since Android had no usage from before you installed."
                null -> "Android keeps about 10 days of usage. If none is from before you installed, your first week becomes the starting point."
            },
            color = Mist,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun VerdictBanner(progress: JourneyProgress) {
    val change = progress.changePercent ?: 0
    val (icon, tint, background) = when (progress.verdict) {
        Verdict.WORKING -> Triple(Icons.AutoMirrored.Rounded.TrendingDown, Forest, Mint)
        Verdict.STEADY -> Triple(Icons.AutoMirrored.Rounded.TrendingFlat, Amber, AmberTint)
        Verdict.WORSE -> Triple(Icons.AutoMirrored.Rounded.TrendingUp, Red, RedTint)
        Verdict.TOO_EARLY -> Triple(Icons.Rounded.QueryStats, Slate, NeutralChip)
        Verdict.MEASURING -> Triple(Icons.Rounded.HourglassTop, Slate, NeutralChip)
    }
    val (title, body) = when (progress.verdict) {
        Verdict.WORKING -> "It's working" to
            "You're on your phone ${-change}% less than before TimeWise. That's ${formatDuration(progress.savedMillis)} back in your life so far."
        Verdict.STEADY -> "No real change yet" to
            "Your usage is about the same as before. Try a daily focus challenge or turn on the bedtime reminder."
        Verdict.WORSE -> "Usage is up $change%" to
            "You're using your phone more than before TimeWise. Fight back with one 1-hour focus challenge a day."
        Verdict.TOO_EARLY -> "First result is on its way" to
            "Your starting point is saved: ${formatDuration(progress.baseline?.dailyAverageMillis ?: 0L)} a day. " +
            "Check back in a few days to see if it's going down."
        Verdict.MEASURING -> "Measuring your starting point" to
            "Your first full week sets the baseline. Day ${progress.baselineDaysCollected} of 7 collected."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = body, color = Ink, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun BeforeNow(before: Long, now: Long?, change: Int?) {
    val max = maxOf(before, now ?: 0L).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ComparisonBar(label = "Before TimeWise", millis = before, fraction = before.toFloat() / max, color = Mist)
        if (now != null) {
            ComparisonBar(
                label = "Last 7 days",
                millis = now,
                fraction = now.toFloat() / max,
                color = if (now <= before) Forest else Red,
                trailing = change?.let { percent ->
                    {
                        ChangePill(
                            text = when {
                                percent < 0 -> "↓ ${-percent}%"
                                percent > 0 -> "↑ $percent%"
                                else -> "0%"
                            },
                            trend = when {
                                percent < 0 -> Trend.Better
                                percent > 0 -> Trend.Worse
                                else -> Trend.Neutral
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ComparisonBar(
    label: String,
    millis: Long,
    fraction: Float,
    color: Color,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = label, color = Slate, fontSize = 12.sp, modifier = Modifier.weight(1f))
            trailing?.invoke()
            Text(text = "${formatDuration(millis)} a day", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(StepperBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun JourneyStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(StepperBg)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = Slate, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Weekly averages since install, with the dashed baseline to beat. */
@Composable
private fun WeeklyChart(weeks: List<Long>, baseline: Long) {
    val max = (weeks + baseline).max().coerceAtLeast(1L) * 1.15f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "Daily average by week", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .drawBehind {
                    val y = size.height * (1f - baseline / max)
                    drawLine(
                        color = Mist,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weeks.forEach { average ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(average / max)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (average <= baseline) Forest else Red)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weeks.indices.forEach { index ->
                Text(
                    text = "W${index + 1}",
                    modifier = Modifier.weight(1f),
                    color = Mist,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(14.dp).height(2.dp).background(Mist))
            Text(text = "Before TimeWise", color = Mist, fontSize = 10.sp)
        }
    }
}

@Composable
private fun Milestones(journey: JourneyState) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StepperBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Milestones", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        MilestoneRow(
            icon = Icons.Rounded.Download,
            title = "Installed TimeWise",
            detail = "${formatDate(journey.installedAt)}, ${formatTime(context, journey.installedAt)}"
        )
        MilestoneRow(
            icon = Icons.Rounded.Insights,
            title = "Usage access on",
            detail = journey.usageAccessSince?.let { "Since ${formatDate(it)}" }
        )
        MilestoneRow(
            icon = Icons.Rounded.Notifications,
            title = "Notifications on",
            detail = journey.notificationsSince?.let { "Since ${formatDate(it)}" },
            divider = false
        )
    }
}

/** A null [detail] means the step isn't done yet. */
@Composable
private fun MilestoneRow(icon: ImageVector, title: String, detail: String?, divider: Boolean = true) {
    val done = detail != null
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (done) Mint else NeutralChip, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (done) Forest else Mist, modifier = Modifier.size(16.dp))
        }
        Text(text = title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(text = detail ?: "Not allowed yet", color = if (done) Slate else Mist, fontSize = 12.sp)
    }
    if (divider) {
        Box(
            modifier = Modifier
                .padding(start = 38.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder)
        )
    }
}
