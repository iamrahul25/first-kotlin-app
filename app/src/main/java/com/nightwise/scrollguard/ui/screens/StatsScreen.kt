package com.nightwise.scrollguard.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.scrollguard.R
import com.nightwise.scrollguard.data.FocusRepository
import com.nightwise.scrollguard.data.FocusStats
import com.nightwise.scrollguard.ui.components.ChangePill
import com.nightwise.scrollguard.ui.components.ScreenHeader
import com.nightwise.scrollguard.ui.components.SectionTitle
import com.nightwise.scrollguard.ui.components.Trend
import com.nightwise.scrollguard.ui.components.focusCard
import com.nightwise.scrollguard.ui.formatDuration
import com.nightwise.scrollguard.ui.theme.Forest
import com.nightwise.scrollguard.ui.theme.Ink
import com.nightwise.scrollguard.ui.theme.Mint
import com.nightwise.scrollguard.ui.theme.Mist
import com.nightwise.scrollguard.ui.theme.Slate
import com.nightwise.scrollguard.ui.weeklyChange
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun StatsScreen(stats: FocusStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Your Stats")

        SectionTitle("This Week")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val (timeChange, timeTrend) = weeklyChange(stats.thisWeekSeconds, stats.lastWeekSeconds)
            StatCard(
                icon = R.drawable.ic_stat_time,
                label = "Total Time Wasted",
                value = formatDuration(stats.thisWeekSeconds),
                change = timeChange,
                trend = timeTrend,
                modifier = Modifier.weight(1f)
            )
            val (reelsChange, reelsTrend) = weeklyChange(stats.thisWeekReels.toLong(), stats.lastWeekReels.toLong())
            StatCard(
                icon = R.drawable.ic_stat_reels,
                label = "Reels Scrolled",
                value = stats.thisWeekReels.toString(),
                change = reelsChange,
                trend = reelsTrend,
                modifier = Modifier.weight(1f)
            )
        }

        SectionTitle("Last 7 Days")
        WeekChart(stats)
    }
}

@Composable
private fun StatCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    change: String,
    trend: Trend,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            ChangePill(text = change, trend = trend)
            Text(text = "(vs last week)", color = Mist, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WeekChart(stats: FocusStats) {
    val week = stats.thisWeek
    val maxSeconds = (week.maxOfOrNull { it.seconds } ?: 0L).coerceAtLeast(60L)
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatDuration(stats.thisWeekSeconds),
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "  on short videos",
                color = Slate,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            week.forEachIndexed { index, day ->
                val isToday = index == week.lastIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .weight(1f, fill = false)
                            .fillMaxHeight((day.seconds.toFloat() / maxSeconds).coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isToday) Forest else Mint)
                    )
                    Text(
                        text = dayFormat.format(FocusRepository.epochDayToUtcMillis(day.epochDay)),
                        color = if (isToday) Ink else Mist,
                        fontSize = 10.sp,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
