package com.nightwise.scrollguard.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.scrollguard.R
import com.nightwise.scrollguard.data.FocusSettings
import com.nightwise.scrollguard.ui.components.FocusToggle
import com.nightwise.scrollguard.ui.components.PrimaryButton
import com.nightwise.scrollguard.ui.components.ScreenHeader
import com.nightwise.scrollguard.ui.components.focusCard
import com.nightwise.scrollguard.ui.formatLimitMinutes
import com.nightwise.scrollguard.ui.theme.Ink
import com.nightwise.scrollguard.ui.theme.MintSoft
import com.nightwise.scrollguard.ui.theme.Slate
import com.nightwise.scrollguard.ui.theme.StepperBg

private const val TIMER_STEP_MINUTES = 15
private const val TIMER_MIN_MINUTES = 15
private const val TIMER_MAX_MINUTES = 8 * 60
private const val REELS_STEP = 5
private const val REELS_MIN = 5
private const val REELS_MAX = 500

@Composable
fun SetLimitsScreen(
    settings: FocusSettings,
    onBack: () -> Unit,
    onSave: (FocusSettings) -> Unit
) {
    var timerEnabled by rememberSaveable { mutableStateOf(settings.dailyTimerEnabled) }
    var timerMinutes by rememberSaveable { mutableIntStateOf(settings.dailyTimerMinutes) }
    var reelsEnabled by rememberSaveable { mutableStateOf(settings.reelsLimitEnabled) }
    var reelsLimit by rememberSaveable { mutableIntStateOf(settings.reelsLimit) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(title = "Set Limits", onBack = onBack)

            LimitSettingCard(
                icon = R.drawable.ic_daily_timer,
                title = "Daily Timer",
                tag = "(Total)",
                description = "Maximum time you can spend on blocked platforms per day.",
                enabled = timerEnabled,
                onEnabledChange = { timerEnabled = it },
                value = formatLimitMinutes(timerMinutes),
                onDecrease = { timerMinutes = (timerMinutes - TIMER_STEP_MINUTES).coerceAtLeast(TIMER_MIN_MINUTES) },
                onIncrease = { timerMinutes = (timerMinutes + TIMER_STEP_MINUTES).coerceAtMost(TIMER_MAX_MINUTES) }
            )

            LimitSettingCard(
                icon = R.drawable.ic_reels_limit,
                title = "Reels Limit",
                tag = "(Per Day)",
                description = "Maximum number of reels/short videos you can scroll per day.",
                enabled = reelsEnabled,
                onEnabledChange = { reelsEnabled = it },
                value = reelsLimit.toString(),
                onDecrease = { reelsLimit = (reelsLimit - REELS_STEP).coerceAtLeast(REELS_MIN) },
                onIncrease = { reelsLimit = (reelsLimit + REELS_STEP).coerceAtMost(REELS_MAX) }
            )

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
                    Text(text = "Tip", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (timerEnabled || reelsEnabled) {
                            "Set realistic limits. Start small and increase as you build the habit."
                        } else {
                            "With both limits off, short videos are blocked as soon as they open."
                        },
                        color = Slate,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        PrimaryButton(
            text = "Save",
            onClick = {
                onSave(
                    settings.copy(
                        dailyTimerEnabled = timerEnabled,
                        dailyTimerMinutes = timerMinutes,
                        reelsLimitEnabled = reelsEnabled,
                        reelsLimit = reelsLimit
                    )
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun LimitSettingCard(
    @DrawableRes icon: Int,
    title: String,
    tag: String,
    description: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(text = title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = tag, color = Slate, fontSize = 12.sp, modifier = Modifier.padding(bottom = 1.dp))
                }
                Text(text = description, color = Slate, fontSize = 12.sp, lineHeight = 17.sp)
            }
            FocusToggle(checked = enabled, onCheckedChange = onEnabledChange)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .clip(RoundedCornerShape(12.dp))
                .background(StepperBg)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton(R.drawable.ic_stepper_decrease, "Decrease", enabled, onDecrease)
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            StepperButton(R.drawable.ic_stepper_increase, "Increase", enabled, onIncrease)
        }
    }
}

@Composable
private fun StepperButton(@DrawableRes icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    Image(
        painter = painterResource(icon),
        contentDescription = label,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    )
}
