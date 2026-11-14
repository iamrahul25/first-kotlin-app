package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.R
import com.nightwise.timewise.permissions.PermissionStatus
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.PrimaryButton
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SmallActionButton
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.MintSoft
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Slate

class PermissionActions(
    val requestUsageAccess: () -> Unit,
    val requestNotifications: () -> Unit,
    val requestAccessibility: () -> Unit
)

@Composable
fun PermissionsScreen(
    status: PermissionStatus,
    actions: PermissionActions,
    isOnboarding: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = if (isOnboarding) 32.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(
                title = if (isOnboarding) "Allow Permissions" else "Permissions",
                onBack = if (isOnboarding) null else onBack
            )
            Text(
                text = "TimeWise reads Android's screen time data to show where your time goes. Everything stays on your device.",
                color = Slate,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionRow(
                    icon = Icons.Rounded.QueryStats,
                    tint = Purple,
                    tileColor = PurpleTint,
                    title = "Usage Access",
                    tag = "(Required)",
                    description = "Shows your total screen time, time in each app and how often you unlock.",
                    granted = status.usageAccess,
                    onAllow = actions.requestUsageAccess
                )
                PermissionRow(
                    icon = Icons.Rounded.Notifications,
                    tint = Amber,
                    tileColor = AmberTint,
                    title = "Notifications",
                    tag = "(Recommended)",
                    description = "Sends your bedtime reminder.",
                    granted = status.notifications,
                    onAllow = actions.requestNotifications
                )
                PermissionRow(
                    icon = Icons.Rounded.Accessibility,
                    tint = Forest,
                    tileColor = Mint,
                    title = "Accessibility",
                    tag = "(For app limits)",
                    description = "Closes an app when its daily limit runs out. TimeWise only sees which app is open, never what's on screen.",
                    granted = status.accessibility,
                    onAllow = actions.requestAccessibility
                )
            }

            if (!status.usageAccess) UsageAccessTip()
        }

        if (isOnboarding) {
            PrimaryButton(
                text = if (status.usageAccess) "Continue" else "Skip for now",
                onClick = onContinue,
                showArrow = status.usageAccess,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    tint: Color,
    tileColor: Color,
    title: String,
    tag: String,
    description: String,
    granted: Boolean,
    onAllow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard(onClick = if (granted) null else onAllow)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tint = tint, background = tileColor)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text = title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = tag, color = Slate, fontSize = 12.sp, modifier = Modifier.padding(bottom = 1.dp))
            }
            Text(text = description, color = Slate, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (granted) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Mint)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Forest, modifier = Modifier.size(14.dp))
                Text(text = "Allowed", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            SmallActionButton(text = "Allow", onClick = onAllow)
        }
    }
}

@Composable
private fun UsageAccessTip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MintSoft)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(R.drawable.ic_bulb),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = "How to allow", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Tap Allow, find TimeWise in the list, turn on \"Permit usage access\", then come back.",
                color = Slate,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
