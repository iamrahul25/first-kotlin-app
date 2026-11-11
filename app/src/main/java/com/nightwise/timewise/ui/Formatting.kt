package com.nightwise.timewise.ui

import android.content.Context
import android.text.format.DateFormat
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.ui.components.Trend
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

/** "3h 24m", "24m", "0m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** A ticking timer: "59:12" or "7:02:03", rounding up so it reads 0:00 only when time is up. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mmss = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    return if (hours > 0) "$hours:$mmss" else mmss
}

/** A limit in minutes: "45m", "1h", "1h 30m". */
fun formatLimitMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

/** Local clock time in the user's 12/24-hour preference. */
fun formatTime(context: Context, millis: Long): String = DateFormat.getTimeFormat(context).format(millis)

fun formatMinutesOfDay(context: Context, minutes: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
    }
    return formatTime(context, calendar.timeInMillis)
}

/** "11 Nov 2026" in local time. */
fun formatDate(millis: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(millis)

/** "Mon", "Tue", ... */
fun dayOfWeekLabel(epochDay: Long): String = utcFormat("EEE").format(FocusRepository.epochDayToUtcMillis(epochDay))

/** "9" */
fun dayOfMonthLabel(epochDay: Long): String = utcFormat("d").format(FocusRepository.epochDayToUtcMillis(epochDay))

/** "Nov" for a 0-based [month]. */
fun monthLabel(year: Int, month: Int): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month, 1)
    }
    return utcFormat("MMM").format(calendar.timeInMillis)
}

/** "4 – 10 Nov" or "12 Oct – 10 Nov". */
fun dateRangeLabel(firstDay: Long, lastDay: Long): String {
    val first = FocusRepository.epochDayToUtcMillis(firstDay)
    val last = FocusRepository.epochDayToUtcMillis(lastDay)
    val sameMonth = utcFormat("MMyyyy").format(first) == utcFormat("MMyyyy").format(last)
    val start = if (sameMonth) utcFormat("d").format(first) else utcFormat("d MMM").format(first)
    return "$start – ${utcFormat("d MMM").format(last)}"
}

/** "Today", "Yesterday" or "Mon, 9 Nov". */
fun relativeDayLabel(epochDay: Long, today: Long = FocusRepository.currentEpochDay()): String = when (epochDay) {
    today -> "Today"
    today - 1 -> "Yesterday"
    else -> utcFormat("EEE, d MMM").format(FocusRepository.epochDayToUtcMillis(epochDay))
}

/** Change against a previous period, where going down is the good direction. */
fun changeVsPrevious(current: Long, previous: Long): Pair<String, Trend> {
    if (previous <= 0L) return "—" to Trend.Neutral
    val percent = (abs(current - previous) * 100.0 / previous).roundToInt()
    return when {
        percent == 0 -> "0%" to Trend.Neutral
        current < previous -> "↓ $percent%" to Trend.Better
        else -> "↑ $percent%" to Trend.Worse
    }
}

private fun utcFormat(pattern: String) =
    SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
