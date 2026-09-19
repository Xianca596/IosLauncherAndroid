package com.example.ioslauncheronandroid.data

import android.provider.Settings

sealed interface SettingsTarget {
    data class System(val action: String) : SettingsTarget
    data class Launcher(val action: LauncherAction) : SettingsTarget
}

enum class LauncherAction { Settings, Wallpapers, AddWidget }

data class SettingsEntry(val title: String, val subtitle: String, val keywords: String, val target: SettingsTarget)

object SettingsIndex {
    private fun sys(title: String, action: String, keywords: String = "") =
        SettingsEntry(title, "System settings", "$title $keywords", SettingsTarget.System(action))

    private fun app(title: String, sub: String, action: LauncherAction, keywords: String = "") =
        SettingsEntry(title, sub, "$title $keywords", SettingsTarget.Launcher(action))

    val all: List<SettingsEntry> = listOf(
        app("Launcher settings", "Launcher", LauncherAction.Settings, "icon size grid columns blur glass theme customize"),
        app("Wallpapers", "Launcher", LauncherAction.Wallpapers, "lock screen background rotate photos"),
        app("Add widget", "Launcher", LauncherAction.AddWidget, "widgets home clock battery"),
        sys("Wi‑Fi", Settings.ACTION_WIFI_SETTINGS, "wifi internet network wireless"),
        sys("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, "pair headphones devices"),
        sys("Mobile network", Settings.ACTION_DATA_ROAMING_SETTINGS, "data sim cellular roaming"),
        sys("Airplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS, "flight"),
        sys("Display", Settings.ACTION_DISPLAY_SETTINGS, "brightness dark mode screen timeout refresh rate"),
        sys("Sound", Settings.ACTION_SOUND_SETTINGS, "volume ringtone vibration silent"),
        sys("Battery", Settings.ACTION_BATTERY_SAVER_SETTINGS, "power saver charging"),
        sys("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "space memory files"),
        sys("Apps", Settings.ACTION_APPLICATION_SETTINGS, "applications manage uninstall permissions"),
        sys("Default apps", Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, "launcher browser home"),
        sys("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS, "gps"),
        sys("Security", Settings.ACTION_SECURITY_SETTINGS, "lock fingerprint password pin face"),
        sys("Privacy", Settings.ACTION_PRIVACY_SETTINGS, "permissions"),
        sys("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS, "text size talkback"),
        sys("Date & time", Settings.ACTION_DATE_SETTINGS, "timezone clock"),
        sys("Language", Settings.ACTION_LOCALE_SETTINGS, "keyboard region"),
        sys("Keyboard", Settings.ACTION_INPUT_METHOD_SETTINGS, "input typing"),
        sys("NFC", Settings.ACTION_NFC_SETTINGS, "tap pay"),
        sys("Notifications", Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS, "alerts"),
        sys("Wallpaper (system)", Intent_SET_WALLPAPER, "background"),
        sys("Developer options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "usb debugging"),
        sys("About phone", Settings.ACTION_DEVICE_INFO_SETTINGS, "version model info"),
        sys("All settings", Settings.ACTION_SETTINGS, "system"),
    )

    fun search(query: String): List<SettingsEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return all.filter { e -> q.split(' ').all { it in e.keywords.lowercase() } }
    }
}

private const val Intent_SET_WALLPAPER = "android.intent.action.SET_WALLPAPER"
