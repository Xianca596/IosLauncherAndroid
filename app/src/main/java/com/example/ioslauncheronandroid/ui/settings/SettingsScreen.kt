package com.example.ioslauncheronandroid.ui.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.LauncherSettings as S
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import com.example.ioslauncheronandroid.ui.glass.GlassSheet
import com.example.ioslauncheronandroid.ui.glass.LocalBackdrop
import com.example.ioslauncheronandroid.ui.widgets.LocalController
import com.example.ioslauncheronandroid.ui.glass.glass

@Composable
fun SettingsScreen(
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onWallpapers: () -> Unit,
    onAddWidget: () -> Unit,
) {
    val controller = LocalController.current
    val context = LocalContext.current
    val badgesOn = remember { androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    val hasWallpaper = LocalBackdrop.current?.soft != null
    GlassSheet("Launcher settings", onClose) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("Home screen") {
                SliderRow("Icon size", "${S.iconSize.toInt()} dp", S.iconSize, 48f..76f) { S.iconSize = it }
                ChoiceRow("Columns", listOf(4, 5), S.columns) { S.columns = it }
                SwitchRow("Show app names", S.showLabels) { S.showLabels = it }
                SwitchRow("Dock magnification", S.dockMagnify) { S.dockMagnify = it }
                SwitchRow("Haptic feedback", S.haptics) { S.haptics = it }
            }
            Section("Glass & background") {
                SliderRow("Background blur", "${S.blurRadius}", S.blurRadius.toFloat(), 0f..250f) { S.blurRadius = it.toInt() }
                SliderRow("Background dim", "${(S.dim * 100).toInt()}%", S.dim, 0f..0.7f) { S.dim = it }
                SwitchRow("Motion parallax", S.parallax) { S.parallax = it }
                ActionRow(if (hasWallpaper) "Liquid Glass: wallpaper access granted" else "Enable Liquid Glass (wallpaper access)") {
                    if (!hasWallpaper) controller.requestWallpaperAccess()
                }
                SliderRow("Glass opacity", "${(S.glassAlpha * 100).toInt()}%", S.glassAlpha, 0.04f..0.45f) { S.glassAlpha = it }
            }
            Section("Widgets page") {
                com.example.ioslauncheronandroid.ui.widgets.Builtin.entries.forEach { w -> SwitchRow("${w.emoji}  ${w.title}", w.isOn) { w.isOn = it } }
                SwitchRow("24‑hour clock", S.clock24h) { S.clock24h = it }
                SliderRow("Clock size", "${S.clockSize.toInt()}", S.clockSize, 36f..110f) { S.clockSize = it }
                ActionRow("Add a widget", onAddWidget)
            }
            Section("Wallpapers") {
                ActionRow("Manage wallpapers & lock screen", onWallpapers)
            }
            Section("Hidden apps") {
                val hidden = apps.filter { it.key in S.hidden }
                if (hidden.isEmpty()) Text("No hidden apps. Hold an app icon and choose Hide.", color = Color.White.copy(0.6f), fontSize = 13.sp)
                hidden.forEach { app ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(app.label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        GlassButton("Show") { S.unhide(app.key) }
                    }
                }
            }
            Section("System") {
                ActionRow(if (badgesOn) "Notification badges: on" else "Turn on notification badges") {
                    if (!badgesOn) runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                ActionRow("Set as default home app") { controller.requestDefaultLauncher() }
                ActionRow("Use a different home app") {
                    runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                ActionRow("Uninstall this launcher") {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                Text(
                    "Not for you? Pick another home app, or uninstall — Android goes straight back to your previous launcher.",
                    color = Color.White.copy(0.55f), fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title.uppercase(), color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
        Column(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp), elevation = 0).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF34C759), checkedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(0.2f), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
        )
    }
}

@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(value, color = Color.White.copy(0.65f), fontSize = 14.sp)
        }
        Slider(
            current, onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.25f)),
        )
    }
}

@Composable
private fun ChoiceRow(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        options.forEach { o -> GlassButton("$o", strong = o == selected) { onSelect(o) } }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pointerInput(onClick) { detectTapGestures { onClick() } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("›", color = Color.White.copy(0.6f), fontSize = 22.sp)
    }
}
