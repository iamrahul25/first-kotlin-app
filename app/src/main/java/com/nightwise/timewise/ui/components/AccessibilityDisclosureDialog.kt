package com.nightwise.timewise.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

/**
 * Prominent disclosure shown before the Accessibility settings open, as Google Play requires for
 * apps that use the AccessibilityService API without being accessibility tools. It can only be
 * closed with an explicit choice.
 */
@Composable
fun AccessibilityDisclosureDialog(onAgree: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Text(
                text = "Accessibility service disclosure",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "TimeWise uses Android's Accessibility service for one purpose: to enforce the " +
                        "daily app limits you set and to show a floating timer during focus challenges.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(text = "What it does", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "• Notices which app is in front of you, so it can count time against that app's limit.\n" +
                        "• When an app's limit is reached, closes it and shows your extra-time steps.\n" +
                        "• Draws the small floating timer while a focus challenge is running.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(text = "What it does not do", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "• It cannot read what is on your screen, what you type, or your passwords.\n" +
                        "• It does not collect or share any personal or sensitive data. Everything stays on " +
                        "your phone, and TimeWise has no internet access.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "If you agree, you'll be taken to Accessibility settings to switch TimeWise on. " +
                        "You can turn it off there at any time.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) { Text("Agree", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("Decline") }
        }
    )
}
