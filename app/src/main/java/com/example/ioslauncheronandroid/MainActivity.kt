package com.example.ioslauncheronandroid

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.data.PhotoStore
import com.example.ioslauncheronandroid.ui.widgets.LauncherController
import com.example.ioslauncheronandroid.ui.widgets.LocalController
import com.example.ioslauncheronandroid.wallpaper.WallpaperStore
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.AppRepository
import com.example.ioslauncheronandroid.ui.glass.Backdrop
import com.example.ioslauncheronandroid.ui.glass.LocalBackdrop
import com.example.ioslauncheronandroid.ui.glass.TiltSensor
import com.example.ioslauncheronandroid.ui.glass.WallpaperBackdrop
import com.example.ioslauncheronandroid.ui.home.HomeScreen
import com.example.ioslauncheronandroid.ui.theme.IosLauncherOnAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), LauncherController {
    private val host by lazy { AppWidgetHost(this, HOST_ID) }
    private val awm by lazy { AppWidgetManager.getInstance(this) }
    private var pendingWidget = -1
    private var pendingToLibrary = false
    private val wallpaperVersion = mutableIntStateOf(0)
    private val isDefaultHome = mutableStateOf(true)
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { com.example.ioslauncheronandroid.weather.WeatherState.version.intValue++ }
    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { com.example.ioslauncheronandroid.ui.widgets.CalendarAccess.version.intValue++ }
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshDefaultHome() }
    private var lastWallpaperId = -1
    private var ownBackdrop = false
    private val tilt by lazy { TiltSensor(this) }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
        if (uris.isNotEmpty()) lifecycleScope.launch { WallpaperStore.add(uris) }
    }

    private val photosWidgetPicker = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) PhotoStore.add(uris)
    }

    private companion object {
        const val HOST_ID = 1024
        const val REQ_BIND = 101
        const val REQ_CONFIG = 102
    }

    private var windowBlurActive = false
    private val homePress = mutableIntStateOf(0)
    private val packageVersion = mutableIntStateOf(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            packageVersion.intValue++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PhotoStore.init(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        enableWindowBlur()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)

        setContent {
            IosLauncherOnAndroidTheme {
                val context = LocalContext.current
                val view = LocalView.current
                var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
                val backdrop = remember { Backdrop(windowBlur = windowBlurActive) }

                LaunchedEffect(packageVersion.intValue) {
                    apps = AppRepository.load(context, context.packageName)
                }
                LaunchedEffect(wallpaperVersion.intValue, WallpaperStore.changeTick.intValue) {
                    if (WallpaperStore.changeTick.intValue > 0) kotlinx.coroutines.delay(500)
                    val screen = IntSize(view.resources.displayMetrics.widthPixels, view.resources.displayMetrics.heightPixels)
                    val images = withContext(Dispatchers.Default) { WallpaperBackdrop.load(context, screen) }
                    if (images != null) {
                        backdrop.sharp = images.sharp
                        backdrop.soft = images.soft
                        ownBackdrop = true

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setBackgroundBlurRadius(0)
                    }
                }
                LaunchedEffect(Unit) {
                    snapshotFlow { LauncherSettings.blurRadius }.collect {
                        if (windowBlurActive && !ownBackdrop && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setBackgroundBlurRadius(it)
                    }
                }
                CompositionLocalProvider(LocalBackdrop provides backdrop, LocalController provides this@MainActivity) {
                    HomeScreen(apps, backdrop, homePress.intValue, isDefaultHome.value)
                }
            }
        }
    }

    private fun enableWindowBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val apply = { enabled: Boolean ->
            windowBlurActive = enabled
            window.setBackgroundBlurRadius(if (enabled) LauncherSettings.blurRadius else 0)
        }
        apply(windowManager.isCrossWindowBlurEnabled)
        windowManager.addCrossWindowBlurEnabledListener(mainExecutor) { apply(it) }
    }

    override fun providers(): List<AppWidgetProviderInfo> = awm.installedProviders
    override fun providerInfo(id: Int): AppWidgetProviderInfo? = awm.getAppWidgetInfo(id)
    override fun createWidgetView(id: Int): AppWidgetHostView? =
        awm.getAppWidgetInfo(id)?.let { host.createView(this, id, it) }

    override fun addWidget(info: AppWidgetProviderInfo, toLibrary: Boolean) {
        pendingToLibrary = toLibrary
        val id = host.allocateAppWidgetId()
        if (awm.bindAppWidgetIdIfAllowed(id, info.provider)) {
            finishAddWidget(id, info)
        } else {
            pendingWidget = id
            @Suppress("DEPRECATION")
            startActivityForResult(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider),
                REQ_BIND,
            )
        }
    }

    private fun finishAddWidget(id: Int, info: AppWidgetProviderInfo?) {
        if (info?.configure != null) {
            pendingWidget = id
            host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIG, null)
        } else {
            LauncherSettings.addWidgetId(id, pendingToLibrary)
        }
    }

    override fun removeWidget(id: Int) {
        LauncherSettings.removeWidgetId(id)
        host.deleteAppWidgetId(id)
    }

    override fun pickWallpapers() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidget) ?: pendingWidget
        when (requestCode) {
            REQ_BIND -> if (resultCode == RESULT_OK) finishAddWidget(id, awm.getAppWidgetInfo(id)) else host.deleteAppWidgetId(id)
            REQ_CONFIG -> if (resultCode == RESULT_OK) LauncherSettings.addWidgetId(id, pendingToLibrary) else host.deleteAppWidgetId(id)
        }
    }

    override fun onStart() {
        super.onStart()
        host.startListening()
    }

    override fun onResume() {
        super.onResume()
        tilt.start()
        refreshDefaultHome()

        val id = WallpaperBackdrop.currentId(this)
        if (id != lastWallpaperId || !ownBackdrop) {
            lastWallpaperId = id
            wallpaperVersion.intValue++
        }
    }

    override fun onPause() {
        tilt.stop()
        super.onPause()
    }

    private fun computeIsDefaultHome(): Boolean =
        if (Build.VERSION.SDK_INT >= 29) {
            getSystemService(android.app.role.RoleManager::class.java).isRoleHeld(android.app.role.RoleManager.ROLE_HOME)
        } else {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
        }

    private fun refreshDefaultHome() { isDefaultHome.value = computeIsDefaultHome() }

    override fun requestCalendarAccess() { calendarPermission.launch(android.Manifest.permission.READ_CALENDAR) }

    override fun requestLocationAccess() { locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }

    override fun requestPhotoAccess() {
        photosWidgetPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    override fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = getSystemService(android.app.role.RoleManager::class.java)
            if (roles.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) && !roles.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                roleRequest.launch(roles.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME))
                return
            }
        }
        runCatching { startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }
    }

    override fun requestWallpaperAccess() {
        pickWallpapers()
    }

    override fun onStop() {
        host.stopListening()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePress.intValue++
        }
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }
}
