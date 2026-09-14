package com.you.reelblocker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.you.reelblocker.service.AccessibilityUtils
import com.you.reelblocker.service.ServiceStateBus
import com.you.reelblocker.ui.theme.FirstAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncServiceState()

        setContent {
            FirstAppTheme {
                val state by ServiceStateBus.state.collectAsState()
                ReelBlockerApp(
                    state = state,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncServiceState()
    }

    private fun syncServiceState() {
        val granted = AccessibilityUtils.isAccessibilityServiceEnabled(this)
        ServiceStateBus.update {
            it.copy(isAccessibilityPermissionGranted = granted, isServiceRunning = granted)
        }
    }
}

@Composable
fun ReelBlockerApp(
    state: com.you.reelblocker.service.ServiceStatus,
    onOpenAccessibilitySettings: () -> Unit = {}
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Reel Blocker\nService running: ${state.isServiceRunning}\nAccessibility granted: ${state.isAccessibilityPermissionGranted}\nBlocked total: ${state.blockedTotal}",
            )

            if (!state.isAccessibilityPermissionGranted) {
                Button(onClick = onOpenAccessibilitySettings) {
                    Text("Enable accessibility service")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReelBlockerAppPreview() {
    FirstAppTheme {
        ReelBlockerApp(com.you.reelblocker.service.ServiceStatus())
    }
}
