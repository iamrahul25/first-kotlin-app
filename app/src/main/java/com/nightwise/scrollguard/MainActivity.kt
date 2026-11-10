package com.nightwise.scrollguard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.nightwise.scrollguard.data.FocusRepository
import com.nightwise.scrollguard.data.FocusSettings
import com.nightwise.scrollguard.data.FocusStats
import com.nightwise.scrollguard.data.Platform
import com.nightwise.scrollguard.permissions.PermissionStatus
import com.nightwise.scrollguard.permissions.PermissionUtils
import com.nightwise.scrollguard.ui.components.FocusBottomNav
import com.nightwise.scrollguard.ui.components.ProtectionState
import com.nightwise.scrollguard.ui.components.Tab
import com.nightwise.scrollguard.ui.screens.BlockedPlatformsScreen
import com.nightwise.scrollguard.ui.screens.HomeScreen
import com.nightwise.scrollguard.ui.screens.PermissionActions
import com.nightwise.scrollguard.ui.screens.PermissionsScreen
import com.nightwise.scrollguard.ui.screens.SetLimitsScreen
import com.nightwise.scrollguard.ui.screens.SettingsScreen
import com.nightwise.scrollguard.ui.screens.SplashScreen
import com.nightwise.scrollguard.ui.screens.StatsScreen
import com.nightwise.scrollguard.ui.theme.Cream
import com.nightwise.scrollguard.ui.theme.FocusBlockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Route { Splash, Onboarding, Main, Permissions, Limits }

private val RouteStackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map { it.name } },
    restore = { names -> names.map(Route::valueOf).toMutableStateList() }
)

class MainActivity : ComponentActivity() {

    private lateinit var repository: FocusRepository
    private var permissionStatus by mutableStateOf(PermissionStatus())
    private var resumeCount by mutableIntStateOf(0)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            refreshPermissions()
            val permanentlyDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            if (!granted && permanentlyDenied) PermissionUtils.openNotificationSettings(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        repository = FocusRepository.get(this)
        refreshPermissions()

        val actions = PermissionActions(
            requestAccessibility = { PermissionUtils.openAccessibilitySettings(this) },
            requestNotifications = ::requestNotifications,
            requestUsageAccess = { PermissionUtils.openUsageAccessSettings(this) },
            requestBattery = { PermissionUtils.requestIgnoreBatteryOptimizations(this) },
            openAppInfo = { PermissionUtils.openAppDetails(this) }
        )

        setContent {
            FocusBlockTheme {
                FocusBlockApp(
                    repository = repository,
                    permissions = permissionStatus,
                    resumeCount = resumeCount,
                    actions = actions
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        repository.refresh()
        resumeCount++
    }

    private fun refreshPermissions() {
        permissionStatus = PermissionUtils.check(this)
    }

    private fun requestNotifications() {
        val needsRuntimeRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsRuntimeRequest) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionUtils.openNotificationSettings(this)
        }
    }
}

@Composable
private fun FocusBlockApp(
    repository: FocusRepository,
    permissions: PermissionStatus,
    resumeCount: Int,
    actions: PermissionActions
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()
    val stats by repository.stats.collectAsState()

    val backStack = rememberSaveable(saver = RouteStackSaver) {
        mutableListOf(if (settings.onboardingDone) Route.Main else Route.Splash).toMutableStateList()
    }
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }

    val currentRoute = backStack.last()
    fun navigate(route: Route) = backStack.add(route)
    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1 || (currentRoute == Route.Main && tab != Tab.Home)) {
        if (backStack.size > 1) pop() else tab = Tab.Home
    }

    val protectionState = when {
        !settings.protectionEnabled -> ProtectionState.Paused
        !permissions.accessibility -> ProtectionState.NeedsPermission
        else -> ProtectionState.Live
    }
    val onProtectionChange: (Boolean) -> Unit = { enabled ->
        repository.updateSettings { it.copy(protectionEnabled = enabled) }
        if (enabled && !permissions.accessibility) navigate(Route.Permissions)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
    ) {
        Crossfade(targetState = currentRoute, label = "route") { route ->
            when (route) {
                Route.Splash -> SplashScreen(onGetStarted = { navigate(Route.Onboarding) })

                Route.Onboarding -> PermissionsScreen(
                    status = permissions,
                    actions = actions,
                    isOnboarding = true,
                    onBack = ::pop,
                    onContinue = {
                        repository.updateSettings { it.copy(onboardingDone = true) }
                        backStack.clear()
                        backStack.add(Route.Main)
                    }
                )

                Route.Permissions -> PermissionsScreen(
                    status = permissions,
                    actions = actions,
                    isOnboarding = false,
                    onBack = ::pop,
                    onContinue = ::pop
                )

                Route.Limits -> SetLimitsScreen(
                    settings = settings,
                    onBack = ::pop,
                    onSave = { updated ->
                        repository.updateSettings { updated }
                        Toast.makeText(context, "Limits saved", Toast.LENGTH_SHORT).show()
                        pop()
                    }
                )

                Route.Main -> MainTabs(
                    tab = tab,
                    onTabChange = { tab = it },
                    settings = settings,
                    stats = stats,
                    permissions = permissions,
                    protectionState = protectionState,
                    resumeCount = resumeCount,
                    onProtectionChange = onProtectionChange,
                    onOpenPermissions = { navigate(Route.Permissions) },
                    onOpenLimits = { navigate(Route.Limits) },
                    onAllowUsageAccess = actions.requestUsageAccess,
                    onPlatformToggle = { platform, blocked ->
                        repository.updateSettings { it.withBlocked(platform, blocked) }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainTabs(
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    settings: FocusSettings,
    stats: FocusStats,
    permissions: PermissionStatus,
    protectionState: ProtectionState,
    resumeCount: Int,
    onProtectionChange: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenLimits: () -> Unit,
    onAllowUsageAccess: () -> Unit,
    onPlatformToggle: (Platform, Boolean) -> Unit
) {
    val context = LocalContext.current
    val appUsage by produceState(emptyMap<String, Long>(), permissions.usageAccess, resumeCount, tab) {
        value = if (tab == Tab.Home && permissions.usageAccess) {
            withContext(Dispatchers.IO) {
                PermissionUtils.foregroundTimeToday(
                    context,
                    Platform.entries.filter { it.supported }.map { it.packageName }.toSet()
                )
            }
        } else {
            value
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (tab) {
                Tab.Home -> HomeScreen(
                    settings = settings,
                    stats = stats,
                    protectionState = protectionState,
                    usageAccessGranted = permissions.usageAccess,
                    appUsageMillis = appUsage,
                    onProtectionChange = onProtectionChange,
                    onOpenPermissions = onOpenPermissions,
                    onOpenStats = { onTabChange(Tab.Stats) },
                    onOpenLimits = onOpenLimits,
                    onAllowUsageAccess = onAllowUsageAccess
                )

                Tab.Stats -> StatsScreen(stats = stats)

                Tab.Block -> BlockedPlatformsScreen(
                    settings = settings,
                    onBack = { onTabChange(Tab.Home) },
                    onPlatformToggle = onPlatformToggle,
                    onAddPlatforms = {
                        Toast.makeText(context, "Facebook is coming soon.", Toast.LENGTH_SHORT).show()
                    }
                )

                Tab.Settings -> SettingsScreen(
                    settings = settings,
                    protectionState = protectionState,
                    permissions = permissions,
                    onProtectionChange = onProtectionChange,
                    onOpenPermissions = onOpenPermissions,
                    onOpenPlatforms = { onTabChange(Tab.Block) },
                    onOpenLimits = onOpenLimits
                )
            }
        }
        FocusBottomNav(selected = tab, onSelect = onTabChange)
    }
}
