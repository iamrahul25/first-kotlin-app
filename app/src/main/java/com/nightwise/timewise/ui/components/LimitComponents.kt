package com.nightwise.timewise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.limits.AppLimit
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatLimitMinutes
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg

/** Sets or changes one app's daily limit. [onRemove] is null for an app without one yet. */
@Composable
fun LimitDialog(
    packageName: String,
    initialMinutes: Int,
    onSave: (Int) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val info = rememberAppInfo(packageName)
    var minutes by remember(packageName) { mutableIntStateOf(initialMinutes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(info, size = 40.dp) },
        title = { Text(info?.label ?: "Daily limit", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Time allowed each day. When it runs out the app closes, and you can earn ${LimitLogic.EXTRA_MINUTES} more minutes by going through your steps.",
                    color = Slate,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepperButton(Icons.Rounded.Remove, "Less", enabled = minutes > LimitLogic.MIN_LIMIT_MINUTES) {
                        minutes = LimitLogic.stepDown(minutes)
                    }
                    Text(
                        text = formatLimitMinutes(minutes),
                        color = Ink,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StepperButton(Icons.Rounded.Add, "More", enabled = minutes < LimitLogic.MAX_LIMIT_MINUTES) {
                        minutes = LimitLogic.stepUp(minutes)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    LimitLogic.PRESET_MINUTES.forEach { preset ->
                        val selected = preset == minutes
                        Text(
                            text = formatLimitMinutes(preset),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Forest else NeutralChip)
                                .clickable { minutes = preset }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (selected) Mint else Slate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(minutes); onDismiss() }) { Text("Save", color = Forest) }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = { onRemove(); onDismiss() }) { Text("Remove", color = Red) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Slate) }
            }
        }
    )
}

@Composable
private fun StepperButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) Mint else StepperBg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) Forest else Slate, modifier = Modifier.size(20.dp))
    }
}

/** An app with a limit: today's use against what's allowed, extra time included. */
@Composable
fun AppLimitRow(limit: AppLimit, usedMillis: Long, extraMinutes: Int, onClick: () -> Unit) {
    val info = rememberAppInfo(limit.packageName)
    val allowance = LimitLogic.allowanceMillis(limit.minutes, extraMinutes)
    val fraction = (usedMillis.toFloat() / allowance).coerceIn(0f, 1f)
    val exhausted = LimitLogic.isExhausted(usedMillis, limit.minutes, extraMinutes)
    val barColor = when {
        exhausted -> Red
        fraction >= 0.8f -> Amber
        else -> Forest
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
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
                Text(
                    text = buildString {
                        append("${formatDuration(usedMillis)} / ${formatLimitMinutes(limit.minutes)}")
                        if (extraMinutes > 0) append(" +${extraMinutes}m")
                    },
                    color = if (exhausted) Red else Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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
                        .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
        }
    }
}
