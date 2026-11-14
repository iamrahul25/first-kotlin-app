package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.R
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.challenge.ChallengeResult
import com.nightwise.timewise.data.report.PeriodReport
import com.nightwise.timewise.data.report.ReportBuilder
import com.nightwise.timewise.data.report.ReportPeriod
import com.nightwise.timewise.data.usage.AppInfoCache
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.report.ReportSharer
import com.nightwise.timewise.ui.changeVsPrevious
import com.nightwise.timewise.ui.components.AppUsageRow
import com.nightwise.timewise.ui.components.ChangePill
import com.nightwise.timewise.ui.components.PrimaryButton
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.dateRangeLabel
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Cream
import com.nightwise.timewise.ui.theme.ForestDeep
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Slate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(
    usage: UsageSnapshot?,
    challengeHistory: List<ChallengeResult>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureLayer = rememberGraphicsLayer()
    var period by rememberSaveable { mutableStateOf(ReportPeriod.WEEK) }
    var sharing by remember { mutableStateOf(false) }

    val report = remember(usage, challengeHistory, period) {
        usage?.let {
            ReportBuilder.build(period, it.history, challengeHistory, it.sleepLog, FocusRepository.currentEpochDay())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(title = "Reports", onBack = onBack)
            PeriodToggle(selected = period, onSelect = { period = it })

            when {
                report == null -> Text(text = "Loading…", color = Slate, fontSize = 12.sp)
                else -> Box(
                    modifier = Modifier.drawWithContent {
                        captureLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(captureLayer)
                    }
                ) {
                    ReportCard(report)
                }
            }
        }

        PrimaryButton(
            text = if (sharing) "Preparing…" else "Share report",
            enabled = report?.hasData == true && !sharing,
            onClick = {
                val current = report ?: return@PrimaryButton
                sharing = true
                scope.launch {
                    try {
                        val bitmap = captureLayer.toImageBitmap().asAndroidBitmap()
                        val (uri, text) = withContext(Dispatchers.IO) {
                            val uri = runCatching { ReportSharer.save(context, bitmap) }.getOrNull()
                            val labels = current.topApps.take(3).map { AppInfoCache.get(context, it.packageName).label }
                            uri to shareText(current, labels)
                        }
                        ReportSharer.share(context, uri, text)
                    } finally {
                        sharing = false
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun PeriodToggle(selected: ReportPeriod, onSelect: (ReportPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeutralChip)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ReportPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Text(
                text = if (period == ReportPeriod.WEEK) "Weekly" else "Monthly",
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) CardWhite else NeutralChip)
                    .clickable { onSelect(period) }
                    .padding(vertical = 8.dp),
                color = if (isSelected) Ink else Slate,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The shareable recap. Drawn on a cream backdrop so the exported image has no transparent corners. */
@Composable
private fun ReportCard(report: PeriodReport) {
    Box(
        modifier = Modifier
            .background(Cream)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = report.period.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = dateRangeLabel(report.firstDay, report.lastDay), color = Slate, fontSize = 12.sp)
                }
                Text(text = "TimeWise", color = ForestDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (!report.hasData) {
                Text(
                    text = "Not enough history yet. TimeWise saves a little more every day, so check back tomorrow.",
                    color = Slate,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                return@Column
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Total screen time", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(text = formatDuration(report.totalMillis), color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (change, trend) = changeVsPrevious(report.dailyAverageMillis, report.previousDailyAverageMillis)
                    ChangePill(text = change, trend = trend)
                    Text(
                        text = "${formatDuration(report.dailyAverageMillis)} a day vs ${report.period.previousLabel}",
                        color = Slate,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportStat(
                    label = "Unlocks a day",
                    value = report.unlocks?.let { (it / report.trackedDays.coerceAtLeast(1)).toString() } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                ReportStat(
                    label = "Focus wins",
                    value = if (report.focusAttempts == 0) "—" else "${report.focusWins} of ${report.focusAttempts}",
                    modifier = Modifier.weight(1f)
                )
                ReportStat(
                    label = "Avg sleep",
                    value = report.averageSleepMillis?.let(::formatDuration) ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }

            if (report.topApps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Top apps", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val max = report.topApps.first().millis
                    report.topApps.forEach { app ->
                        AppUsageRow(packageName = app.packageName, millis = app.millis, maxMillis = max)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                report.busiestDay?.let {
                    Text(text = "Busiest day: ${relativeDayLabel(it.epochDay)} · ${formatDuration(it.totalMillis)}", color = Slate, fontSize = 12.sp)
                }
                report.calmestDay?.let {
                    Text(text = "Calmest day: ${relativeDayLabel(it.epochDay)} · ${formatDuration(it.totalMillis)}", color = Slate, fontSize = 12.sp)
                }
                if (report.trackedDays < report.period.days) {
                    Text(
                        text = "Based on ${report.trackedDays} of ${report.period.days} days with data.",
                        color = Mist,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Cream)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = Slate, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun shareText(report: PeriodReport, topAppLabels: List<String>): String = buildString {
    val (change, _) = changeVsPrevious(report.dailyAverageMillis, report.previousDailyAverageMillis)
    append("My TimeWise ${report.period.title.lowercase()} (${dateRangeLabel(report.firstDay, report.lastDay)}): ")
    append("${formatDuration(report.totalMillis)} of screen time, ${formatDuration(report.dailyAverageMillis)} a day")
    if (report.previousDailyAverageMillis > 0) append(" ($change vs ${report.period.previousLabel})")
    append('.')
    if (topAppLabels.isNotEmpty()) append(" Top apps: ${topAppLabels.joinToString()}.")
    if (report.focusWins > 0) append(" Focus challenges won: ${report.focusWins}.")
}
