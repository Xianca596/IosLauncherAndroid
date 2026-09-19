package com.example.ioslauncheronandroid

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.wallpaper.WallpaperStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LauncherApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        LauncherSettings.init(this)
        WallpaperStore.init(this)
        com.example.ioslauncheronandroid.data.HomeLayout.init(this)
        com.example.ioslauncheronandroid.data.UsageStore.init(this)

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val pending = goAsync()
                scope.launch {
                    try { WallpaperStore.rotateNext() } finally { pending.finish() }
                }
            }
        }, IntentFilter(Intent.ACTION_USER_PRESENT))
    }
}
