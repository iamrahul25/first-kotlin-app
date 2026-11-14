package com.nightwise.timewise.ui.screens

import android.app.TimePickerDialog
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.FocusSettings
import com.nightwise.timewise.data.SleepEntry
import com.nightwise.timewise.ui.components.FocusToggle
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.SmallActionButton
import com.nightwise.timewise.ui.components.UsageAccessCard
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatMinutesOfDay
import com.nightwise.timewise.ui.formatTime
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Blue
import com.nightwise.timewise.ui.theme.BlueTint
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Slate

private val REMINDER_LEADS = listOf(0, 15, 30)

@Composable
fun BedtimeScreen(
    settings: FocusSettings,
    sleepLog: List<SleepEntry>?,
    notificationsGranted: Boolean,
    usageAccessGranted: Boolean,
    onSettingsChange: (FocusSettings) -> Unit,
    onAllowNotifications: () -> Unit,
    onAllowUsageAccess: () -> Unit
) {
    val context = LocalContext.current
    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initial / 60,
            initial % 60,
            DateFormat.is24HourFormat(context)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Bedtime")

        SectionTitle("Schedule")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            TimeRow(
                icon = Icons.Rounded.Bedtime,
                tint = Purple,
                tileColor = PurpleTint,
                title = "Bedtime",
                value = formatMinutesOfDay(context, settings.bedtimeMinutes),
                onClick = { pickTime(settings.bedtimeMinutes) { onSettingsChange(settings.copy(bedtimeMinutes = it)) } }
            )
            TimeRow(
                icon = Icons.Rounded.WbSunny,
                tint = Amber,
                tileColor = AmberTint,
                title = "Wake up",
                value = formatMinutesOfDay(context, settings.wakeMinutes),
                onClick = { pickTime(settings.wakeMinutes) { onSettingsChange(settings.copy(wakeMinutes = it)) } }
            )
            Text(
                text = "Sleep goal ${formatDuration(settings.sleepGoalMinutes * 60_000L)}",
                color = Slate,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        SectionTitle("Reminder")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(Icons.Rounded.NotificationsActive, tint = Blue, background = BlueTint, size = 36.dp, iconSize = 20.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = "Bedtime reminder", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "A nudge to put the phone down", color = Slate, fontSize = 12.sp)
                }
                FocusToggle(
                    checked = settings.bedtimeEnabled,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(bedtimeEnabled = it))
                        if (it && !notificationsGranted) onAllowNotifications()
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REMINDER_LEADS.forEach { lead ->
                    LeadChip(
                        text = if (lead == 0) "At bedtime" else "$lead min before",
                        selected = settings.reminderLeadMinutes == lead,
                        enabled = settings.bedtimeEnabled,
                        onClick = { onSettingsChange(settings.copy(reminderLeadMinutes = lead)) }
                    )
                }
            }
            if (settings.bedtimeEnabled && !notificationsGranted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberTint)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Notifications are off, so the reminder can't show.",
                        modifier = Modifier.weight(1f),
                        color = Ink,
                        fontSize = 12.sp
                    )
                    SmallActionButton(text = "Allow", onClick = onAllowNotifications)
                }
            }
        }

        SectionTitle("Sleep Log")
        SleepLogCard(
            sleepLog = sleepLog,
            goalMinutes = settings.sleepGoalMinutes,
            usageAccessGranted = usageAccessGranted,
            onAllowUsageAccess = onAllowUsageAccess
        )
    }
}

@Composable
private fun TimeRow(
    icon: ImageVector,
    tint: Color,
    tileColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tint = tint, background = tileColor, size = 36.dp, iconSize = 20.dp)
        Text(text = title, modifier = Modifier.weight(1f), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = Forest, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LeadChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected && enabled) Forest else NeutralChip)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = when {
            selected && enabled -> CardWhite
            enabled -> Ink
            else -> Mist
        },
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SleepLogCard(
    sleepLog: List<SleepEntry>?,
    goalMinutes: Int,
    usageAccessGranted: Boolean,
    onAllowUsageAccess: () -> Unit
) {
    if (!usageAccessGranted) {
        UsageAccessCard(onAllow = onAllowUsageAccess)
        return
    }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P ->
                Text(text = "The sleep log needs Android 9 or newer.", color = Slate, fontSize = 12.sp)
            sleepLog == null -> Text(text = "Loading…", color = Slate, fontSize = 12.sp)
            sleepLog.isEmpty() ->
                Text(text = "No nights recorded yet. Check back tomorrow morning.", color = Slate, fontSize = 12.sp)
            else -> {
                val average = sleepLog.sumOf { it.durationMillis } / sleepLog.size
                Text(
                    text = "Average ${formatDuration(average)} a night, based on when your phone was put down and picked up.",
                    color = Slate,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                sleepLog.forEach { entry ->
                    val metGoal = entry.durationMillis >= goalMinutes * 60_000L
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = relativeDayLabel(entry.wakeEpochDay), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${formatTime(context, entry.phoneDownAt)} → ${formatTime(context, entry.pickedUpAt)}",
                                color = Slate,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = formatDuration(entry.durationMillis),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (metGoal) Mint else AmberTint)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            color = if (metGoal) Forest else Amber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
