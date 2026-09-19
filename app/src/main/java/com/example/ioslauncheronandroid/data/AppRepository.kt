package com.example.ioslauncheronandroid.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class AppInfo(
    val label: String,
    val component: ComponentName,
    val group: WidgetCategory,
    val icon: ImageBitmap,
    val installedAt: Long = 0L,
) {
    val key: String get() = component.flattenToString()
}

class AppShortcut(val label: String, val start: () -> Unit)

object AppRepository {
    private fun storeCategory(info: ApplicationInfo): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.category else -1

    private const val ICON_PX = 160

    suspend fun load(context: Context, selfPackage: String): List<AppInfo> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        coroutineScope {
            pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != selfPackage }
                .map { ri ->
                    async {
                        val ai = ri.activityInfo
                        AppInfo(
                            label = ri.loadLabel(pm).toString(),
                            component = ComponentName(ai.packageName, ai.name),
                            group = WidgetCategorizer.classify("${ri.loadLabel(pm)} ${ai.packageName}", storeCategory(ai.applicationInfo)),
                            icon = squircleIcon(ri.loadIcon(pm)).asImageBitmap(),
                            installedAt = runCatching { pm.getPackageInfo(ai.packageName, 0).firstInstallTime }.getOrDefault(0L),
                        )
                    }
                }
                .awaitAll()
                .sortedBy { it.label.lowercase() }
        }
    }

    private fun squircleIcon(d: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val half = ICON_PX / 2f
        val path = Path()
        val steps = 96
        for (i in 0..steps) {
            val t = 2 * Math.PI * i / steps
            val cs = Math.cos(t); val sn = Math.sin(t)
            val x = half + half * (Math.signum(cs) * Math.pow(Math.abs(cs), 2 / 5.0)).toFloat()
            val y = half + half * (Math.signum(sn) * Math.pow(Math.abs(sn), 2 / 5.0)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.clipPath(path)
        d.setBounds(0, 0, ICON_PX, ICON_PX)
        d.draw(c)
        return bmp
    }

    fun pickDock(apps: List<AppInfo>): List<AppInfo> {
        val wanted = listOf(
            listOf("dialer", "incallui"),
            listOf("messag", "mms"),
            listOf("chrome", "browser", "firefox"),
            listOf("camera"),
            listOf("photos", "gallery"),
            listOf("music", "youtube.music"),
            listOf("settings"),
        )
        val picked = wanted.mapNotNull { hints ->
            apps.firstOrNull { app -> hints.any { it in app.component.packageName } }
        }.distinct().take(4)
        return picked.ifEmpty { apps.take(4) }
    }

    fun shortcuts(context: Context, pkg: String): List<AppShortcut> {
        if (Build.VERSION.SDK_INT < 25) return emptyList()
        return runCatching {
            val la = context.getSystemService(android.content.pm.LauncherApps::class.java)
            if (!la.hasShortcutHostPermission()) return emptyList()
            val query = android.content.pm.LauncherApps.ShortcutQuery().setPackage(pkg).setQueryFlags(
                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
            la.getShortcuts(query, android.os.Process.myUserHandle()).orEmpty()
                .filter { it.isEnabled }
                .mapNotNull { s ->
                    val label = (s.shortLabel ?: s.longLabel)?.toString().orEmpty()
                    if (label.isBlank()) null else AppShortcut(label) { runCatching { la.startShortcut(s, null, null) } }
                }
                .take(4)
        }.getOrDefault(emptyList())
    }

    fun launch(context: Context, app: AppInfo, options: android.os.Bundle?): Boolean {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(i, options)
            true
        } catch (_: Exception) {
            false
        }
    }
}
