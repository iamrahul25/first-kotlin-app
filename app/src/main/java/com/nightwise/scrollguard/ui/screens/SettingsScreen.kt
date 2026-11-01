package com.nightwise.scrollguard.ui.screens

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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.scrollguard.R
import com.nightwise.scrollguard.data.FocusSettings
import com.nightwise.scrollguard.permissions.PermissionStatus
import com.nightwise.scrollguard.ui.components.FocusToggle
import com.nightwise.scrollguard.ui.components.IconTile
import com.nightwise.scrollguard.ui.components.ProtectionState
import com.nightwise.scrollguard.ui.components.ScreenHeader
import com.nightwise.scrollguard.ui.components.SectionTitle
import com.nightwise.scrollguard.ui.components.focusCard
import com.nightwise.scrollguard.ui.theme.Amber
import com.nightwise.scrollguard.ui.theme.AmberTint
import com.nightwise.scrollguard.ui.theme.Blue
import com.nightwise.scrollguard.ui.theme.BlueTint
import com.nightwise.scrollguard.ui.theme.Forest
import com.nightwise.scrollguard.ui.theme.Ink
import com.nightwise.scrollguard.ui.theme.Mint
import com.nightwise.scrollguard.ui.theme.Mist
import com.nightwise.scrollguard.ui.theme.Purple
import com.nightwise.scrollguard.ui.theme.PurpleTint
import com.nightwise.scrollguard.ui.theme.Slate

@Composable
fun SettingsScreen(
    settings: FocusSettings,
    protectionState: ProtectionState,
    permissions: PermissionStatus,
    onProtectionChange: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenPlatforms: () -> Unit,
    onOpenLimits: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Settings")

        SectionTitle("Protection")
        SettingsRow(
            icon = Icons.Rounded.Shield,
            tint = Forest,
            tileColor = Mint,
            title = "Protection",
            subtitle = when (protectionState) {
                ProtectionState.Live -> "Live: short videos are being blocked"
                ProtectionState.Paused -> "Paused: nothing is blocked"
                ProtectionState.NeedsPermission -> "Needs Accessibility access to run"
            },
            trailing = { FocusToggle(checked = settings.protectionEnabled, onCheckedChange = onProtectionChange) }
        )
        SettingsRow(
            icon = Icons.Rounded.VerifiedUser,
            tint = Amber,
            tileColor = AmberTint,
            title = "Permissions",
            subtitle = "${permissions.grantedCount} of ${permissions.totalCount} allowed" +
                if (!permissions.accessibility) " · Accessibility missing" else "",
            onClick = onOpenPermissions
        )

        SectionTitle("Blocking")
        SettingsRow(
            icon = Icons.Rounded.Apps,
            tint = Blue,
            tileColor = BlueTint,
            title = "Blocked Platforms",
            subtitle = "Choose which apps to block",
            onClick = onOpenPlatforms
        )
        SettingsRow(
            icon = Icons.Rounded.Timer,
            tint = Purple,
            tileColor = PurpleTint,
            title = "Set Limits",
            subtitle = if (settings.hasAnyLimit) "Daily timer and reels limit" else "Off: blocks instantly",
            onClick = onOpenLimits
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = Mist,
                modifier = Modifier.size(12.dp)
            )
            Text(text = "FocusBlock 1.0 · Everything stays on your device", color = Mist, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    tileColor: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
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
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
