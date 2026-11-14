package com.nightwise.timewise

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nightwise.timewise.bedtime.BedtimeScheduler
import com.nightwise.timewise.challenge.ChallengeScheduler
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.FocusSettings
import com.nightwise.timewise.data.badges.BadgeRepository
import com.nightwise.timewise.data.challenge.ChallengeRepository
import com.nightwise.timewise.data.journey.JourneyLogic
import com.nightwise.timewise.data.journey.JourneyRepository
import com.nightwise.timewise.data.limits.AppLimitRepository
import com.nightwise.timewise.data.profile.ProfileLogic
import com.nightwise.timewise.data.usage.AppInsights
import com.nightwise.timewise.data.usage.ScreenTimeRepository
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.permissions.PermissionStatus
import com.nightwise.timewise.permissions.PermissionUtils
import com.nightwise.timewise.snapshot.DailySnapshotScheduler
import com.nightwise.timewise.widget.UsageWidgets
import com.nightwise.timewise.ui.components.FocusBottomNav
import com.nightwise.timewise.ui.components.Tab
import com.nightwise.timewise.ui.screens.AppDetailScreen
import com.nightwise.timewise.ui.screens.AppInsightsScreen
import com.nightwise.timewise.ui.screens.AppLimitPickerScreen
import com.nightwise.timewise.ui.screens.AppsScreen
import com.nightwise.timewise.ui.screens.AwarenessScreen
import com.nightwise.timewise.ui.screens.CategoriesScreen
import com.nightwise.timewise.ui.screens.BadgesScreen
import com.nightwise.timewise.ui.screens.BedtimeScreen
import com.nightwise.timewise.ui.screens.FeaturesScreen
import com.nightwise.timewise.ui.screens.FocusRoute
import com.nightwise.timewise.ui.screens.FrictionSetupRoute
import com.nightwise.timewise.ui.screens.HomeScreen
import com.nightwise.timewise.ui.screens.PermissionActions
import com.nightwise.timewise.ui.screens.PermissionsScreen
import com.nightwise.timewise.ui.screens.ProfileScreen
import com.nightwise.timewise.ui.screens.ReportsScreen
import com.nightwise.timewise.ui.screens.SettingsScreen
import com.nightwise.timewise.ui.screens.SplashScreen
import com.nightwise.timewise.ui.screens.StatsScreen
import com.nightwise.timewise.ui.theme.Cream
import com.nightwise.timewise.ui.theme.TimeWiseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class Route {
    Splash, Onboarding, Main, Permissions, Apps, Reports, Badges, Profile, AppInsights, AppDetail, Features,
    AppLimitPicker, FrictionSetup, Awareness, Categories
}

private val RouteStackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map { it.name } },
    restore = { names -> names.map(Route::valueOf).toMutableStateList() }
)

private const val USAGE_REFRESH_MS = 60_000L

class MainActivity : ComponentActivity() {

    private lateinit var repository: FocusRepository
    private var permissionStatus by mutableStateOf(PermissionStatus())

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
        BedtimeScheduler.sync(this)
        DailySnapshotScheduler.schedule(this)

        val actions = PermissionActions(
            requestUsageAccess = { PermissionUtils.openUsageAccessSettings(this) },
            requestNotifications = ::requestNotifications,
            requestAccessibility = { PermissionUtils.openAccessibilitySettings(this) }
        )

        setContent {
            TimeWiseTheme {
                TimeWiseApp(
                    repository = repository,
                    permissions = permissionStatus,
                    actions = actions
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    /** Leaving the app is when the home screen comes back into view. */
    override fun onStop() {
        super.onStop()
        UsageWidgets.refresh(this)
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
private fun TimeWiseApp(
    repository: FocusRepository,
    permissions: PermissionStatus,
    actions: PermissionActions
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by repository.settings.collectAsState()
    val screenTime = remember { ScreenTimeRepository(context.applicationContext) }

    val challenges = remember { ChallengeRepository.get(context) }
    val challengeState by challenges.state.collectAsState()
    val badges = remember { BadgeRepository.get(context) }
    val badgeRecords by badges.records.collectAsState()
    val journey = remember { JourneyRepository.get(context) }
    val journeyState by journey.state.collectAsState()
    val appLimits = remember { AppLimitRepository.get(context) }
    val limitsState by appLimits.state.collectAsState()
    LaunchedEffect(permissions) { journey.notePermissions(permissions) }

    val usage by produceState<UsageSnapshot?>(null, permissions.usageAccess) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = withContext(Dispatchers.IO) {
                    screenTime.load().also { snapshot ->
                        badges.record(snapshot)
                        journey.captureBaseline(snapshot)
                    }
                }
                val ended = withContext(Dispatchers.IO) { challenges.evaluate() }
                if (ended != null) {
                    ChallengeScheduler.cancel(context)
                    ChallengeScheduler.notifyOutcome(context, ended)
                }
                delay(USAGE_REFRESH_MS)
            }
        }
    }
    val profile = remember(usage) { usage?.let { ProfileLogic.summarize(it.history, it.today.epochDay) } }
    val appDailyAverages = remember(usage) {
        usage?.let { AppInsights.weeklyDailyAverages(it.history, it.today.epochDay) }.orEmpty()
    }
    val journeyProgress = remember(usage, journeyState) {
        usage?.let { JourneyLogic.progress(it.history, journeyState.baseline, journeyState.installDay, it.today.epochDay) }
    }

    val backStack = rememberSaveable(saver = RouteStackSaver) {
        mutableListOf(if (settings.onboardingDone) Route.Main else Route.Splash).toMutableStateList()
    }
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var appsDay by rememberSaveable { mutableLongStateOf(FocusRepository.currentEpochDay()) }

    val currentRoute = backStack.last()
    fun navigate(route: Route) = backStack.add(route)
    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    val openApps: (Long) -> Unit = { day ->
        appsDay = day
        navigate(Route.Apps)
    }
    var detailPackage by rememberSaveable { mutableStateOf("") }
    val openApp: (String) -> Unit = { packageName ->
        detailPackage = packageName
        navigate(Route.AppDetail)
    }
    val updateSettings: (FocusSettings) -> Unit = { updated ->
        repository.updateSettings { updated }
        BedtimeScheduler.sync(context, updated)
    }

    BackHandler(enabled = backStack.size > 1 || (currentRoute == Route.Main && tab != Tab.Home)) {
        if (backStack.size > 1) pop() else tab = Tab.Home
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

                Route.Apps -> AppsScreen(day = usage?.day(appsDay), epochDay = appsDay, onOpenApp = openApp, onBack = ::pop)

                Route.AppInsights -> AppInsightsScreen(usage = usage, onOpenApp = openApp, onBack = ::pop)

                Route.AppDetail -> AppDetailScreen(usage = usage, packageName = detailPackage, onBack = ::pop)

                Route.Features -> FeaturesScreen(onBack = ::pop)

                Route.AppLimitPicker -> AppLimitPickerScreen(
                    state = limitsState,
                    todayApps = usage?.today?.apps.orEmpty(),
                    dailyAverages = appDailyAverages,
                    onSetLimit = appLimits::setLimit,
                    onRemoveLimit = appLimits::removeLimit,
                    onBack = ::pop
                )

                Route.FrictionSetup -> FrictionSetupRoute(
                    onOpenAwareness = { navigate(Route.Awareness) },
                    onBack = ::pop
                )

                Route.Categories -> CategoriesScreen(usage = usage, onBack = ::pop)

                Route.Awareness -> AwarenessScreen(dailyAverageMillis = profile?.dailyAverageMillis, onBack = ::pop)

                Route.Reports -> ReportsScreen(
                    usage = usage,
                    challengeHistory = challengeState.history,
                    onBack = ::pop
                )

                Route.Badges -> BadgesScreen(
                    usage = usage,
                    records = badgeRecords,
                    onSeen = badges::markAllSeen,
                    onBack = ::pop
                )

                Route.Profile -> ProfileScreen(
                    summary = profile,
                    journey = journeyState,
                    journeyProgress = journeyProgress,
                    usageAccessGranted = permissions.usageAccess,
                    onAllowUsageAccess = actions.requestUsageAccess,
                    onOpenFocus = {
                        pop()
                        tab = Tab.Focus
                    },
                    onBack = ::pop
                )

                Route.Main -> Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (tab) {
                            Tab.Home -> HomeScreen(
                                usage = usage,
                                usageAccessGranted = permissions.usageAccess,
                                activeChallenge = challengeState.active,
                                badgeRecords = badgeRecords,
                                profile = profile?.profile,
                                onAllowUsageAccess = actions.requestUsageAccess,
                                onOpenApps = openApps,
                                onOpenApp = openApp,
                                onOpenFocus = { tab = Tab.Focus },
                                onOpenBadges = { navigate(Route.Badges) },
                                onOpenProfile = { navigate(Route.Profile) },
                                onOpenCategories = { navigate(Route.Categories) }
                            )

                            Tab.Focus -> FocusRoute(
                                usageAccessGranted = permissions.usageAccess,
                                notificationsGranted = permissions.notifications,
                                accessibilityEnabled = permissions.accessibility,
                                todayApps = usage?.today?.apps.orEmpty(),
                                dailyAverages = appDailyAverages,
                                onAllowUsageAccess = actions.requestUsageAccess,
                                onAllowNotifications = actions.requestNotifications,
                                onEnableAccessibility = actions.requestAccessibility,
                                onOpenAppInfo = { PermissionUtils.openAppDetails(context) },
                                onOpenLimitPicker = { navigate(Route.AppLimitPicker) },
                                onOpenFrictionSetup = { navigate(Route.FrictionSetup) },
                                onOpenAwareness = { navigate(Route.Awareness) }
                            )

                            Tab.Stats -> StatsScreen(
                                usage = usage,
                                usageAccessGranted = permissions.usageAccess,
                                onAllowUsageAccess = actions.requestUsageAccess,
                                onOpenApps = openApps,
                                onOpenApp = openApp,
                                onOpenAppInsights = { navigate(Route.AppInsights) },
                                badgeRecords = badgeRecords,
                                onOpenReports = { navigate(Route.Reports) },
                                onOpenBadges = { navigate(Route.Badges) }
                            )

                            Tab.Bedtime -> BedtimeScreen(
                                settings = settings,
                                sleepLog = usage?.sleepLog?.take(7),
                                notificationsGranted = permissions.notifications,
                                usageAccessGranted = permissions.usageAccess,
                                onSettingsChange = updateSettings,
                                onAllowNotifications = actions.requestNotifications,
                                onAllowUsageAccess = actions.requestUsageAccess
                            )

                            Tab.Settings -> SettingsScreen(
                                settings = settings,
                                permissions = permissions,
                                lastBackgroundSave = repository.lastBackgroundSave(),
                                profile = profile,
                                installedAt = journeyState.installedAt,
                                onOpenProfile = { navigate(Route.Profile) },
                                onOpenPermissions = { navigate(Route.Permissions) },
                                onOpenBedtime = { tab = Tab.Bedtime },
                                onOpenBatterySettings = { PermissionUtils.openAppDetails(context) },
                                onOpenFeatures = { navigate(Route.Features) }
                            )
                        }
                    }
                    FocusBottomNav(selected = tab, onSelect = { tab = it })
                }
            }
        }
    }
}
