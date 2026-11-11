package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.R
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.badges.BadgeRecord
import com.nightwise.timewise.data.badges.UsageBadge
import com.nightwise.timewise.data.report.MonthAverage
import com.nightwise.timewise.data.report.ReportBuilder
import com.nightwise.timewise.data.usage.AppInsights
import com.nightwise.timewise.data.usage.AppPeriod
import com.nightwise.timewise.data.usage.DayUsage
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.ui.changeVsPrevious
import com.nightwise.timewise.ui.components.AppUsageRow
import com.nightwise.timewise.ui.components.BadgesEntryCard
import com.nightwise.timewise.ui.components.ChangePill
import com.nightwise.timewise.ui.components.HourlyTimeline
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.MonthlyTrendChart
import com.nightwise.timewise.ui.components.UsageAccessCard
import com.nightwise.timewise.ui.components.UsageBarChart
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.RedTint
import com.nightwise.timewise.ui.theme.Slate

private const val STATS_TOP_APPS = 8

@Composable
fun StatsScreen(
    usage: UsageSnapshot?,
    usageAccessGranted: Boolean,
    onAllowUsageAccess: () -> Unit,
    onOpenApps: (Long) -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenAppInsights: () -> Unit,
    badgeRecords: Map<UsageBadge, BadgeRecord>,
    onOpenReports: () -> Unit,
    onOpenBadges: () -> Unit
) {
    var selectedDay by rememberSaveable { mutableLongStateOf(FocusRepository.currentEpochDay()) }
    var showMonth by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Your Stats")

        if (!usageAccessGranted) {
            UsageAccessCard(onAllow = onAllowUsageAccess)
            return@Column
        }
        if (usage == null) {
            Text(text = "Loading…", color = Slate, fontSize = 12.sp)
            return@Column
        }

        SegmentedToggle(
            options = listOf("Week", "Month"),
            selectedIndex = if (showMonth) 1 else 0,
            onSelect = { index ->
                showMonth = index == 1
                if (usage.day(selectedDay) == null) selectedDay = usage.today.epochDay
            }
        )

        val periodDays = if (showMonth) usage.last30Days else usage.thisWeek
        val previousDays = if (showMonth) usage.previous30Days else usage.lastWeek
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val current = periodDays.sumOf { it.totalMillis }
            val previous = previousDays.sumOf { it.totalMillis }
            Text(text = if (showMonth) "Last 30 days" else "Last 7 days", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = formatDuration(current), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                val (change, trend) = changeVsPrevious(current, previous)
                ChangePill(text = change, trend = trend)
                Text(text = if (showMonth) "vs previous 30 days" else "vs last week", color = Mist, fontSize = 11.sp)
            }
            UsageBarChart(
                days = periodDays,
                selectedDay = selectedDay,
                onSelectDay = { selectedDay = it },
                compact = showMonth
            )
        }

        if (showMonth) {
            SectionTitle("Monthly Trend")
            MonthlyTrendCard(ReportBuilder.monthlyAverages(usage.history).takeLast(6))
        } else {
            SectionTitle("Today vs Yesterday")
            TodayVsYesterday(today = usage.today, yesterday = usage.yesterday)
        }

        AppInsightsEntry(usage = usage, onClick = onOpenAppInsights)
        ReportsEntry(onClick = onOpenReports)
        BadgesEntryCard(records = badgeRecords, onClick = onOpenBadges)

        val day = usage.day(selectedDay) ?: usage.today
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(relativeDayLabel(day.epochDay), modifier = Modifier.weight(1f))
            if (day.apps.size > STATS_TOP_APPS) {
                Text(
                    text = "See all",
                    color = Forest,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenApps(day.epochDay) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        DayBreakdown(day, onOpenApp)
    }
}

@Composable
private fun AppInsightsEntry(usage: UsageSnapshot, onClick: () -> Unit) {
    val top = remember(usage) {
        AppInsights.ranking(usage.history, AppPeriod.WEEK, usage.today.epochDay).apps.firstOrNull()
    }
    val info = top?.let { rememberAppInfo(it.packageName) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(background = RedTint, bordered = false, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Leaderboard, contentDescription = null, tint = Red, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "App insights", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (top != null && info != null) "Top time-waster this week: ${info.label}, ${formatDuration(top.totalMillis)}"
                else "Which apps waste the most of your time",
                color = Slate,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "Open app insights",
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SegmentedToggle(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeutralChip)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Text(
                text = option,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) CardWhite else NeutralChip)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                color = if (selected) Ink else Slate,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MonthlyTrendCard(months: List<MonthAverage>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Average screen time per day, by month", color = Slate, fontSize = 12.sp)
        if (months.size < 2) {
            Text(
                text = "Your trend appears as TimeWise saves your history. Android only keeps about 10 days, so it grows from here.",
                color = Mist,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        if (months.isNotEmpty()) MonthlyTrendChart(months)
    }
}

@Composable
private fun ReportsEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(background = Mint, bordered = false, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Summarize, contentDescription = null, tint = Forest, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Weekly & monthly reports", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "Your recap, ready to share", color = Slate, fontSize = 11.sp)
        }
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "Open reports",
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TodayVsYesterday(today: DayUsage, yesterday: DayUsage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompareColumn("Today", today, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(56.dp)
                .background(CardBorder)
        )
        CompareColumn("Yesterday", yesterday, modifier = Modifier
            .weight(1f)
            .padding(start = 14.dp))
    }
}

@Composable
private fun CompareColumn(label: String, day: DayUsage, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = label, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = formatDuration(day.totalMillis), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = day.unlocks?.let { "$it unlocks" } ?: "Unlocks not supported",
            color = Mist,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DayBreakdown(day: DayUsage, onOpenApp: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "${formatDuration(day.totalMillis)} screen time" +
                (day.unlocks?.let { " · $it unlocks" } ?: ""),
            color = Slate,
            fontSize = 12.sp
        )
        if (day.hourlyMinutes.any { it > 0 }) {
            HourlyTimeline(hourlyMinutes = day.hourlyMinutes, currentHour = null)
        }
        if (day.apps.isEmpty()) {
            Text(text = "No per-app details for this day.", color = Mist, fontSize = 12.sp)
        } else {
            val max = day.apps.first().millis
            day.apps.take(STATS_TOP_APPS).forEach { app ->
                AppUsageRow(
                    packageName = app.packageName,
                    millis = app.millis,
                    maxMillis = max,
                    onClick = { onOpenApp(app.packageName) }
                )
            }
        }
    }
}
