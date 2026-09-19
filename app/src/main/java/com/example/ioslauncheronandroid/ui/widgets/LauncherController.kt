package com.example.ioslauncheronandroid.ui.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.staticCompositionLocalOf

interface LauncherController {
    fun providers(): List<AppWidgetProviderInfo>
    fun providerInfo(id: Int): AppWidgetProviderInfo?
    fun createWidgetView(id: Int): AppWidgetHostView?
    fun addWidget(info: AppWidgetProviderInfo, toLibrary: Boolean)
    fun removeWidget(id: Int)
    fun pickWallpapers()

    fun requestWallpaperAccess()

    fun requestDefaultLauncher()

    fun requestPhotoAccess()

    fun requestLocationAccess()

    fun requestCalendarAccess()
}

val LocalController = staticCompositionLocalOf<LauncherController> { error("No LauncherController") }
