package com.nightwise.scrollguard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.scrollguard.data.FocusSettings
import com.nightwise.scrollguard.data.Platform
import com.nightwise.scrollguard.ui.components.FocusToggle
import com.nightwise.scrollguard.ui.components.OutlineButton
import com.nightwise.scrollguard.ui.components.ScreenHeader
import com.nightwise.scrollguard.ui.components.focusCard
import com.nightwise.scrollguard.ui.largeIcon
import com.nightwise.scrollguard.ui.theme.Ink
import com.nightwise.scrollguard.ui.theme.NeutralChip
import com.nightwise.scrollguard.ui.theme.Slate

@Composable
fun BlockedPlatformsScreen(
    settings: FocusSettings,
    onBack: () -> Unit,
    onPlatformToggle: (Platform, Boolean) -> Unit,
    onAddPlatforms: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Blocked Platforms", onBack = onBack)
        Text(
            text = "Short videos and reels will be blocked\nor limited on these platforms.",
            color = Slate,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Platform.entries.forEach { platform ->
                PlatformRow(
                    platform = platform,
                    checked = settings.isBlocked(platform),
                    onCheckedChange = { onPlatformToggle(platform, it) }
                )
            }
        }

        OutlineButton(onClick = onAddPlatforms) {
            Text(text = "+", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "Add more platforms (optional)", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PlatformRow(
    platform: Platform,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val contentAlpha = if (platform.supported) 1f else 0.55f
        Image(
            painter = painterResource(platform.largeIcon()),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .alpha(contentAlpha)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = platform.label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = platform.subtitle, color = Slate, fontSize = 12.sp)
        }
        if (platform.supported) {
            FocusToggle(checked = checked, onCheckedChange = onCheckedChange)
        } else {
            Text(
                text = "Soon",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeutralChip)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                color = Slate,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
