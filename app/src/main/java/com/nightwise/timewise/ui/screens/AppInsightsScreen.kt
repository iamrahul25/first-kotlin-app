package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.usage.AppDailyPoint
import com.nightwise.timewise.data.usage.AppDetail
import com.nightwise.timewise.data.usage.AppInsights
import com.nightwise.timewise.data.usage.AppPeriod
import com.nightwise.timewise.data.usage.AppRanking
import com.nightwise.timewise.data.usage.AppTotal
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.ui.changeVsPrevious
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.ChangePill
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.dayOfMonthLabel
import com.nightwise.timewise.ui.dayOfWeekLabel
import com.nightwise.timewise.ui.formatDate
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg
import kotlin.math.roundToInt

private const val TOP_APPS = 20
private const val DAY_MS = 24L * 60 * 60 * 1000
private val WasteGradient = Brush.linearGradient(listOf(Color(0xFF1C0A0A), Color(0xFF7F1D1D)))
private val Gold = Color(0xFFC98A0B)
private val Silver = Color(0xFF7D8896)
private val Bronze = Color(0xFF9C5221)

/** Which apps take the most time over a week, a month or everything on record. */
@Composable
fun AppInsightsScreen(usage: UsageSnapshot?, onOpenApp: (String) -> Unit, onBack: () -> Unit) {
    var period by rememberSaveable { mutableStateOf(AppPeriod.WEEK) }
    val ranking = remember(usage, period) {
        usage?.let { AppInsights.ranking(it.history, period, it.today.epochDay) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "App insights", onBack = onBack)
        PeriodToggle(
            options = AppPeriod.entries.map { it.label },
            selectedIndex = period.ordinal,
            onSelect = { period = AppPeriod.entries[it] }
        )

        when {
            ranking == null -> Text(text = "Loading…", color = Slate, fontSize = 12.sp)
            ranking.apps.isEmpty() -> Text(text = "No app usage recorded for this period yet.", color = Slate, fontSize = 13.sp)
            else -> {
                TopWasterHero(ranking, onOpenApp)
                SectionTitle("Biggest time-wasters")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusCard()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val max = ranking.apps.first().totalMillis
                    ranking.apps.take(TOP_APPS).forEachIndexed { index, app ->
                        RankedAppRow(
                            rank = index + 1,
                            app = app,
                            maxMillis = max,
                            trackedDays = ranking.trackedDays,
                            onClick = { onOpenApp(app.packageName) }
                        )
                    }
                }
                Text(
                    text = "Tap an app for its daily graph and totals. " +
                        "Days older than 2 weeks keep each day's top 25 apps.",
                    color = Mist,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TopWasterHero(ranking: AppRanking, onOpenApp: (String) -> Unit) {
    val top = ranking.apps.first()
    val info = rememberAppInfo(top.packageName)
    val share = if (ranking.screenTimeMillis > 0) top.totalMillis * 100f / ranking.screenTimeMillis else 0f
    val periodLabel = when (ranking.period) {
        AppPeriod.WEEK -> "this week"
        AppPeriod.MONTH -> "this month"
        AppPeriod.ALL -> "so far"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WasteGradient)
            .clickable { onOpenApp(top.packageName) }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "#1 TIME-WASTER ${periodLabel.uppercase()}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(info, size = 48.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = info?.label ?: "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = formatDuration(top.totalMillis), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = buildString {
                append("${share.roundToInt()}% of all your screen time $periodLabel")
                val days = top.totalMillis.toFloat() / DAY_MS
                if (days >= 1f) append(", or ${"%.1f".format(days)} full days of your life")
                append('.')
            },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            HeroStat("Screen time", formatDuration(ranking.screenTimeMillis), Modifier.weight(1f))
            HeroStat("Apps used", ranking.apps.size.toString(), Modifier.weight(1f))
            HeroStat("Top 3 share", "${(ranking.apps.take(3).sumOf { it.totalMillis } * 100f / ranking.screenTimeMillis.coerceAtLeast(1)).roundToInt()}%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun RankedAppRow(rank: Int, app: AppTotal, maxMillis: Long, trackedDays: Int, onClick: () -> Unit) {
    val info = rememberAppInfo(app.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val medal = when (rank) {
            1 -> Gold
            2 -> Silver
            3 -> Bronze
            else -> null
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(medal ?: NeutralChip, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = rank.toString(), color = if (medal != null) Color.White else Slate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        AppIcon(info, size = 34.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = info?.label ?: "",
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                app.previousMillis?.let { previous ->
                    val (change, trend) = changeVsPrevious(app.totalMillis, previous)
                    if (previous > 0) ChangePill(text = change, trend = trend)
                }
                Text(text = formatDuration(app.totalMillis), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StepperBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((app.totalMillis.toFloat() / maxMillis.coerceAtLeast(1L)).coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (rank <= 3) Red else Forest)
                )
            }
            Text(
                text = "${formatDuration(app.totalMillis / trackedDays.coerceAtLeast(1))} a day · used ${app.activeDays} of $trackedDays days",
                color = Mist,
                fontSize = 11.sp
            )
        }
    }
}

/** Everything about one app: totals, average, daily graph and what it adds up to. */
@Composable
fun AppDetailScreen(usage: UsageSnapshot?, packageName: String, onBack: () -> Unit) {
    val info = rememberAppInfo(packageName)
    val detail = remember(usage, packageName) {
        usage?.let { AppInsights.detail(it.history, packageName, it.today.epochDay) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = info?.label ?: "App", onBack = onBack)
        if (detail == null) {
            Text(text = "Loading…", color = Slate, fontSize = 12.sp)
            return@Column
        }

        DetailHero(detail, info?.label)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PeriodStat("Today", detail.todayMillis, modifier = Modifier.weight(1f))
            PeriodStat("This week", detail.weekMillis, previous = detail.previousWeekMillis, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PeriodStat("Last 30 days", detail.monthMillis, modifier = Modifier.weight(1f))
            PeriodStat("Daily average", detail.dailyAverageMillis, modifier = Modifier.weight(1f))
        }

        SectionTitle("Daily usage")
        DailyChartCard(detail.daily)

        SectionTitle("Habits")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HabitRow("Days opened", "${detail.activeDays} of ${detail.trackedDays} days")
            HabitRow(
                "Busiest day",
                detail.busiestDay?.let { "${relativeDayLabel(it.epochDay)} · ${formatDuration(it.millis)}" } ?: "—"
            )
            HabitRow("Share of screen time", "${(detail.shareOfScreenTime * 100).roundToInt()}%")
            val yearly = detail.dailyAverageMillis * 365
            HabitRow("At this pace", if (yearly >= DAY_MS) "${yearly / DAY_MS} full days a year" else "${formatDuration(yearly)} a year")
        }

        Text(
            text = "\"So far\" counts every day on record" +
                (detail.sinceDay?.let { ", since ${formatDate(FocusRepository.startOfDayMillis(it))}" } ?: "") +
                ". The daily average includes days you didn't open the app.",
            color = Mist,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailHero(detail: AppDetail, label: String?) {
    val info = rememberAppInfo(detail.packageName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WasteGradient)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(info, size = 54.dp)
        }
        Text(
            text = "TIME LOST TO ${(label ?: "this app").uppercase()} SO FAR",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(text = formatDuration(detail.allTimeMillis), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        val days = detail.allTimeMillis.toFloat() / DAY_MS
        Text(
            text = if (days >= 1f) "That's ${"%.1f".format(days)} full days of your life."
            else "${formatDuration(detail.dailyAverageMillis)} a day on average.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PeriodStat(label: String, millis: Long, modifier: Modifier = Modifier, previous: Long? = null) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = formatDuration(millis), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (previous != null && previous > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val (change, trend) = changeVsPrevious(millis, previous)
                ChangePill(text = change, trend = trend)
                Text(text = "vs last week", color = Mist, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DailyChartCard(daily: List<AppDailyPoint>) {
    var showMonth by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableLongStateOf(daily.last().epochDay) }
    val points = if (showMonth) daily else daily.takeLast(7)
    val selected = points.firstOrNull { it.epochDay == selectedDay } ?: points.last()
    val average = points.sumOf { it.millis } / points.size
    val max = (points.maxOf { it.millis }.coerceAtLeast(average)).coerceAtLeast(15 * 60_000L) * 1.1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PeriodToggle(
            options = listOf("7 days", "30 days"),
            selectedIndex = if (showMonth) 1 else 0,
            onSelect = { showMonth = it == 1 }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${relativeDayLabel(selected.epochDay)}: ${formatDuration(selected.millis)}",
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(text = "Avg ${formatDuration(average)}", color = Slate, fontSize = 12.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .drawBehind {
                    val y = size.height * (1f - average / max)
                    drawLine(
                        color = Mist,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(if (showMonth) 2.dp else 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { point ->
                val isSelected = point.epochDay == selected.epochDay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button
                        ) { selectedDay = point.epochDay },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((point.millis / max).coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                when {
                                    isSelected -> Ink
                                    point.millis > average -> Red.copy(alpha = 0.75f)
                                    else -> Forest.copy(alpha = 0.75f)
                                }
                            )
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (showMonth) 2.dp else 8.dp)) {
            points.forEachIndexed { index, point ->
                Text(
                    text = when {
                        !showMonth -> dayOfWeekLabel(point.epochDay)
                        (points.lastIndex - index) % 7 == 0 -> dayOfMonthLabel(point.epochDay)
                        else -> ""
                    },
                    modifier = Modifier.weight(1f),
                    color = if (point.epochDay == selected.epochDay) Ink else Mist,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Text(text = "Red bars are above your average. Dashed line is the average.", color = Mist, fontSize = 10.sp)
    }
}

@Composable
private fun HabitRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = Slate, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PeriodToggle(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
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
