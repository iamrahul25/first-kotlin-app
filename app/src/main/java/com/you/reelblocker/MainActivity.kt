package com.you.reelblocker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.you.reelblocker.service.AccessibilityUtils
import com.you.reelblocker.service.ServiceStateBus
import com.you.reelblocker.service.ServiceStatus
import com.you.reelblocker.ui.theme.ActiveShieldGradient
import com.you.reelblocker.ui.theme.AmberBg
import com.you.reelblocker.ui.theme.AmberWarning
import com.you.reelblocker.ui.theme.CyanPrimary
import com.you.reelblocker.ui.theme.CyanSecondary
import com.you.reelblocker.ui.theme.DarkBg
import com.you.reelblocker.ui.theme.DarkCard
import com.you.reelblocker.ui.theme.DarkCardBorder
import com.you.reelblocker.ui.theme.DarkSurface
import com.you.reelblocker.ui.theme.EmeraldActive
import com.you.reelblocker.ui.theme.EmeraldBg
import com.you.reelblocker.ui.theme.FirstAppTheme
import com.you.reelblocker.ui.theme.PrimaryGradient
import com.you.reelblocker.ui.theme.TextMuted
import com.you.reelblocker.ui.theme.TextPrimary
import com.you.reelblocker.ui.theme.TextSecondary
import com.you.reelblocker.ui.theme.VioletAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncServiceState()

        setContent {
            FirstAppTheme {
                val state by ServiceStateBus.state.collectAsState()
                val context = LocalContext.current

                HomeScreen(
                    state = state,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenAppSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    },
                    onResetStats = {
                        ServiceStateBus.update { it.copy(blockedToday = 0, blockedTotal = 0) }
                        Toast.makeText(context, "Statistics reset", Toast.LENGTH_SHORT).show()
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
fun HomeScreen(
    state: ServiceStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onResetStats: () -> Unit
) {
    val isShieldActive = state.isAccessibilityPermissionGranted

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            HeaderSection(isShieldActive = isShieldActive)

            Spacer(modifier = Modifier.height(24.dp))

            // Main Hero Shield Card
            HeroShieldCard(
                isShieldActive = isShieldActive,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Impact & Statistics Section
            StatsSection(
                blockedToday = state.blockedToday,
                blockedTotal = state.blockedTotal,
                onResetStats = onResetStats
            )

            Spacer(modifier = Modifier.height(24.dp))

            // How It Works / Value Highlights
            FeaturesSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Background Keep-Alive & Battery Info Card
            BatteryTipCard(onOpenAppSettings = onOpenAppSettings)

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun HeaderSection(isShieldActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Logo",
                        tint = DarkBg,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Reel Blocker",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "Reclaim your attention & focus",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 46.dp, top = 2.dp)
            )
        }

        // Status badge
        StatusPill(isShieldActive = isShieldActive)
    }
}

@Composable
fun StatusPill(isShieldActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = if (isShieldActive) EmeraldBg else AmberBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (isShieldActive) EmeraldActive.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alphaAnim)
                    .clip(CircleShape)
                    .background(if (isShieldActive) EmeraldActive else AmberWarning)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isShieldActive) "ACTIVE" else "OFF",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isShieldActive) EmeraldActive else AmberWarning,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun HeroShieldCard(
    isShieldActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = CyanPrimary, spotColor = VioletAccent),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(
            1.dp,
            if (isShieldActive) CyanPrimary.copy(alpha = 0.4f) else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Shield Visual
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isShieldActive) {
                    // Pulsing ambient ring
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(haloScale)
                            .alpha(haloAlpha)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                    )
                }

                // Inner shield orb
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (isShieldActive) ActiveShieldGradient else Brush.linearGradient(
                                listOf(Color(0xFF334155), Color(0xFF1E293B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isShieldActive) Icons.Default.Security else Icons.Default.Shield,
                        contentDescription = "Shield Status",
                        tint = if (isShieldActive) Color.White else TextMuted,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isShieldActive) "Protection is Live" else "Protection is Paused",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isShieldActive)
                    "YouTube Shorts and Instagram Reels will be automatically dismissed when opened."
                else
                    "Grant Accessibility permission so Reel Blocker can intercept distraction loops.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!isShieldActive) {
                // High-visibility Call to Action Button
                Button(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = DarkBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enable in Accessibility Settings",
                        color = DarkBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Action button when active: Settings shortcut
                OutlinedButton(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Manage Accessibility Service",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatsSection(
    blockedToday: Int,
    blockedTotal: Int,
    onResetStats: () -> Unit
) {
    val estimatedMinutesSaved = (blockedTotal * 45) / 60

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Focus Metrics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onResetStats() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Stats",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Today",
                value = "$blockedToday",
                subtitle = "Shorts blocked",
                icon = Icons.Default.VideocamOff,
                accentColor = CyanPrimary
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = "All Time",
                value = "$blockedTotal",
                subtitle = "Total blocked",
                icon = Icons.Default.CheckCircle,
                accentColor = VioletAccent
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = "Saved",
                value = if (estimatedMinutesSaved > 0) "${estimatedMinutesSaved}m" else "<1m",
                subtitle = "Time reclaimed",
                icon = Icons.Default.Timer,
                accentColor = EmeraldActive
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FeaturesSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "How Reel Blocker Works",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            FeatureRow(
                icon = Icons.Default.FlashOn,
                title = "Instant Interception",
                description = "Detects YouTube Shorts and Instagram Reels, then navigates back before autoplay grabs your attention."
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(
                icon = Icons.Default.Lock,
                title = "100% On-Device & Private",
                description = "No network tracking or external telemetry. Your activity never leaves your phone."
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureRow(
                icon = Icons.Default.Shield,
                title = "Zero Battery Drain",
                description = "Dormant by default. Only checks supported app windows when their content changes."
            )
        }
    }
}

@Composable
fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun BatteryTipCard(onOpenAppSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AmberBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep Service Uninterrupted",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Allow Reel Blocker to run in background without battery restrictions.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenAppSettings() }
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "App Settings",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
