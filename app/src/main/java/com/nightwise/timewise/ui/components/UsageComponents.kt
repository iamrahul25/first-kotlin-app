package com.nightwise.timewise.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.usage.AppInfo
import com.nightwise.timewise.data.usage.AppInfoCache
import com.nightwise.timewise.data.report.MonthAverage
import com.nightwise.timewise.data.usage.DayUsage
import com.nightwise.timewise.ui.dayOfMonthLabel
import com.nightwise.timewise.ui.dayOfWeekLabel
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.monthLabel
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberAppInfo(packageName: String): AppInfo? {
    val context = LocalContext.current
    val info by produceState<AppInfo?>(null, packageName) {
        value = withContext(Dispatchers.IO) { AppInfoCache.get(context, packageName) }
    }
    return info
}

@Composable
fun AppIcon(info: AppInfo?, size: Dp = 36.dp) {
    val icon = info?.icon
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4))
                .background(NeutralChip),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Android, contentDescription = null, tint = Mist, modifier = Modifier.size(size / 2))
        }
    }
}

/** App name, time and a bar scaled against the most used app. */
@Composable
fun AppUsageRow(
    packageName: String,
    millis: Long,
    maxMillis: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val info = rememberAppInfo(packageName)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(info)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info?.label ?: "",
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = formatDuration(millis), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        .fillMaxWidth((millis.toFloat() / maxMillis.coerceAtLeast(1L)).coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Forest)
                )
            }
        }
    }
}

/**
 * Daily screen time bars; tapping a bar selects that day. [compact] fits a month of thin
 * bars labelled by day of month instead of a week labelled by weekday.
 */
@Composable
fun UsageBarChart(
    days: List<DayUsage>,
    selectedDay: Long,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    chartHeight: Dp = 130.dp
) {
    val maxMillis = (days.maxOfOrNull { it.totalMillis } ?: 0L).coerceAtLeast(60 * 60_000L)
    val tracked = days.filter { it.totalMillis > 0 }
    val average = if (tracked.isEmpty()) 0L else tracked.sumOf { it.totalMillis } / tracked.size
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Daily average ${formatDuration(average)}", color = Slate, fontSize = 12.sp)
        if (compact) {
            days.firstOrNull { it.epochDay == selectedDay }?.let { selected ->
                Text(
                    text = "${relativeDayLabel(selected.epochDay)}: ${formatDuration(selected.totalMillis)}",
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = if (compact) Arrangement.spacedBy(2.dp) else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val selected = day.epochDay == selectedDay
                val label = when {
                    !compact -> dayOfWeekLabel(day.epochDay)
                    (days.lastIndex - index) % 7 == 0 -> dayOfMonthLabel(day.epochDay)
                    else -> ""
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button
                        ) { onSelectDay(day.epochDay) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)
                ) {
                    if (!compact) {
                        Text(
                            text = if (selected) formatDuration(day.totalMillis) else "",
                            color = Ink,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .then(if (compact) Modifier.fillMaxWidth() else Modifier.width(20.dp))
                            .weight(1f, fill = false)
                            .fillMaxHeight((day.totalMillis.toFloat() / maxMillis).coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(topStart = if (compact) 2.dp else 6.dp, topEnd = if (compact) 2.dp else 6.dp))
                            .background(if (selected) Forest else Mint)
                    )
                    Text(
                        text = label,
                        color = if (selected) Ink else Mist,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/** Average daily screen time per calendar month, as a long-term trend. */
@Composable
fun MonthlyTrendChart(months: List<MonthAverage>, modifier: Modifier = Modifier, chartHeight: Dp = 110.dp) {
    val maxMillis = (months.maxOfOrNull { it.dailyAverageMillis } ?: 0L).coerceAtLeast(60 * 60_000L)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        months.forEachIndexed { index, month ->
            val latest = index == months.lastIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)
            ) {
                Text(
                    text = formatDuration(month.dailyAverageMillis),
                    color = if (latest) Ink else Slate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .fillMaxHeight((month.dailyAverageMillis.toFloat() / maxMillis).coerceIn(0.03f, 1f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (latest) Forest else Mint)
                )
                Text(
                    text = monthLabel(month.year, month.month),
                    color = if (latest) Ink else Mist,
                    fontSize = 10.sp,
                    fontWeight = if (latest) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** Minutes used in each hour of the day, with the busiest hour called out. */
@Composable
fun HourlyTimeline(hourlyMinutes: List<Int>, currentHour: Int?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val maxMinutes = (hourlyMinutes.maxOrNull() ?: 0).coerceAtLeast(15)
    val busiest = hourlyMinutes.indices.maxByOrNull { hourlyMinutes[it] }?.takeIf { hourlyMinutes[it] > 0 }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = busiest?.let { "Busiest hour: ${hourLabel(it, is24h)}–${hourLabel((it + 1) % 24, is24h)} · ${hourlyMinutes[it]}m" }
                ?: "No screen time yet.",
            color = Slate,
            fontSize = 12.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            hourlyMinutes.forEachIndexed { hour, minutes ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight((minutes.toFloat() / maxMinutes).coerceIn(0.04f, 1f))
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(
                            when {
                                minutes == 0 -> StepperBg
                                hour == currentHour -> Forest
                                else -> Forest.copy(alpha = 0.55f)
                            }
                        )
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(0, 6, 12, 18).forEach { hour ->
                Text(text = hourLabel(hour, is24h), modifier = Modifier.weight(1f), color = Mist, fontSize = 10.sp)
            }
        }
    }
}

private fun hourLabel(hour: Int, is24h: Boolean): String = when {
    is24h -> hour.toString().padStart(2, '0')
    hour == 0 -> "12a"
    hour < 12 -> "${hour}a"
    hour == 12 -> "12p"
    else -> "${hour - 12}p"
}

@Composable
fun UsageAccessCard(onAllow: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = "Usage Access needed", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Allow Usage Access so TimeWise can show your screen time, apps and unlocks.",
                color = Slate,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        SmallActionButton(text = "Allow", onClick = onAllow)
    }
}
