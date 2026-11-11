package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.R
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.FocusSettings
import com.nightwise.timewise.data.profile.ProfileSummary
import com.nightwise.timewise.permissions.PermissionStatus
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.ProfileAvatar
import com.nightwise.timewise.ui.components.profileStyle
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.formatMinutesOfDay
import com.nightwise.timewise.ui.formatTime
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Slate

@Composable
fun SettingsScreen(
    settings: FocusSettings,
    permissions: PermissionStatus,
    lastBackgroundSave: Long?,
    profile: ProfileSummary?,
    onOpenProfile: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenBedtime: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Settings")

        ProfileRow(profile = profile, onClick = onOpenProfile)

        SectionTitle("General")
        SettingsRow(
            icon = Icons.Rounded.VerifiedUser,
            tint = Amber,
            tileColor = AmberTint,
            title = "Permissions",
            subtitle = "${permissions.grantedCount} of ${permissions.totalCount} allowed" +
                if (!permissions.usageAccess) " · Usage Access missing" else "",
            onClick = onOpenPermissions
        )
        SettingsRow(
            icon = Icons.Rounded.Bedtime,
            tint = Purple,
            tileColor = PurpleTint,
            title = "Bedtime",
            subtitle = if (settings.bedtimeEnabled) {
                "Reminder on · ${formatMinutesOfDay(context, settings.bedtimeMinutes)}"
            } else {
                "Reminder off"
            },
            onClick = onOpenBedtime
        )
        SettingsRow(
            icon = Icons.Rounded.CloudDone,
            tint = Forest,
            tileColor = Mint,
            title = "Daily history save",
            subtitle = lastBackgroundSave?.let {
                val today = FocusRepository.currentEpochDay()
                val day = when (FocusRepository.currentEpochDay(it)) {
                    today -> "today"
                    today - 1 -> "yesterday"
                    else -> "on ${relativeDayLabel(FocusRepository.currentEpochDay(it), today)}"
                }
                "Last saved $day at ${formatTime(context, it)}"
            } ?: "Runs every night after midnight",
            onClick = onOpenBatterySettings
        )
        Text(
            text = "Saves each day even if you don't open TimeWise. If it stops running, allow TimeWise to run in the background in battery settings.",
            color = Mist,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Mist, modifier = Modifier.size(12.dp))
            Text(text = "TimeWise 2.0 · Everything stays on your device", color = Mist, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProfileRow(profile: ProfileSummary?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(profile = profile?.profile, size = 48.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = "Your profile", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                text = profile?.profile?.title ?: "Not ready yet",
                color = profile?.let { profileStyle(it.profile).text } ?: Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = profile?.let { "${formatDuration(it.dailyAverageMillis)} a day, last 7 days" } ?: "Appears after a full day of tracking",
                color = Slate,
                fontSize = 12.sp
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    tileColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tint = tint, background = tileColor, size = 36.dp, iconSize = 20.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Slate, fontSize = 12.sp)
        }
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}
