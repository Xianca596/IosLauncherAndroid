package com.example.ioslauncheronandroid.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object LauncherSettings {
    private lateinit var prefs: SharedPreferences

    private val _iconSize = mutableFloatStateOf(62f)
    private val _columns = mutableIntStateOf(4)
    private val _showLabels = mutableStateOf(true)
    private val _glassAlpha = mutableFloatStateOf(0.16f)
    private val _blurRadius = mutableIntStateOf(110)
    private val _dim = mutableFloatStateOf(0.25f)
    private val _dockMagnify = mutableStateOf(true)
    private val _haptics = mutableStateOf(true)
    private val _parallax = mutableStateOf(true)
    private val _accessDismissed = mutableStateOf(false)
    private val _showClock = mutableStateOf(true)
    private val _showBattery = mutableStateOf(true)
    private val _showSuggested = mutableStateOf(true)
    private val _showAnalog = mutableStateOf(false)
    private val _showCalendar = mutableStateOf(true)
    private val _showGroups = mutableStateOf(true)
    private val _showWeather = mutableStateOf(true)
    private val _showPhotos = mutableStateOf(false)
    private val _showMusic = mutableStateOf(false)
    private val _clock24h = mutableStateOf(false)
    private val _clockSize = mutableFloatStateOf(64f)
    private val _hidden = mutableStateOf<Set<String>>(emptySet())
    private val _libraryName = mutableStateOf("App Library")
    val widgetIds = SnapshotStateList<Int>()
    val libraryWidgetIds = SnapshotStateList<Int>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
        _iconSize.floatValue = prefs.getFloat("iconSize", 62f)
        _columns.intValue = prefs.getInt("columns", 4)
        _showLabels.value = prefs.getBoolean("showLabels", true)
        _glassAlpha.floatValue = prefs.getFloat("glassAlpha", 0.16f)
        _blurRadius.intValue = prefs.getInt("blurRadius", 110)
        _dim.floatValue = prefs.getFloat("dim", 0.25f)
        _dockMagnify.value = prefs.getBoolean("dockMagnify", true)
        _haptics.value = prefs.getBoolean("haptics", true)
        _parallax.value = prefs.getBoolean("parallax", true)
        _accessDismissed.value = prefs.getBoolean("accessDismissed", false)
        _showClock.value = prefs.getBoolean("showClock", true)
        _showBattery.value = prefs.getBoolean("showBattery", true)
        _showSuggested.value = prefs.getBoolean("showSuggested", true)
        _showAnalog.value = prefs.getBoolean("showAnalog", false)
        _showCalendar.value = prefs.getBoolean("showCalendar", true)
        _showGroups.value = prefs.getBoolean("showGroups", true)
        _showWeather.value = prefs.getBoolean("showWeather", true)
        _showPhotos.value = prefs.getBoolean("showPhotos", false)
        _showMusic.value = prefs.getBoolean("showMusic", false)
        _clock24h.value = prefs.getBoolean("clock24h", false)
        _clockSize.floatValue = prefs.getFloat("clockSize", 64f)
        _hidden.value = prefs.getStringSet("hidden", emptySet()).orEmpty().toSet()
        _libraryName.value = prefs.getString("libraryName", "App Library").orEmpty().ifBlank { "App Library" }
        widgetIds.clear()
        prefs.getString("widgetIds", "").orEmpty().split(',').mapNotNullTo(widgetIds) { it.toIntOrNull() }
        libraryWidgetIds.clear()
        prefs.getString("libraryWidgetIds", "").orEmpty().split(',').mapNotNullTo(libraryWidgetIds) { it.toIntOrNull() }
    }

    var iconSize: Float
        get() = _iconSize.floatValue
        set(v) { _iconSize.floatValue = v; prefs.edit().putFloat("iconSize", v).apply() }
    var columns: Int
        get() = _columns.intValue
        set(v) { _columns.intValue = v; prefs.edit().putInt("columns", v).apply() }
    var showLabels: Boolean
        get() = _showLabels.value
        set(v) { _showLabels.value = v; prefs.edit().putBoolean("showLabels", v).apply() }
    var glassAlpha: Float
        get() = _glassAlpha.floatValue
        set(v) { _glassAlpha.floatValue = v; prefs.edit().putFloat("glassAlpha", v).apply() }
    var blurRadius: Int
        get() = _blurRadius.intValue
        set(v) { _blurRadius.intValue = v; prefs.edit().putInt("blurRadius", v).apply() }
    var dim: Float
        get() = _dim.floatValue
        set(v) { _dim.floatValue = v; prefs.edit().putFloat("dim", v).apply() }
    var dockMagnify: Boolean
        get() = _dockMagnify.value
        set(v) { _dockMagnify.value = v; prefs.edit().putBoolean("dockMagnify", v).apply() }
    var haptics: Boolean
        get() = _haptics.value
        set(v) { _haptics.value = v; prefs.edit().putBoolean("haptics", v).apply() }
    var parallax: Boolean
        get() = _parallax.value
        set(v) { _parallax.value = v; prefs.edit().putBoolean("parallax", v).apply() }
    var accessDismissed: Boolean
        get() = _accessDismissed.value
        set(v) { _accessDismissed.value = v; prefs.edit().putBoolean("accessDismissed", v).apply() }
    var showClock: Boolean
        get() = _showClock.value
        set(v) { _showClock.value = v; prefs.edit().putBoolean("showClock", v).apply() }
    var showWeather: Boolean
        get() = _showWeather.value
        set(v) { _showWeather.value = v; prefs.edit().putBoolean("showWeather", v).apply() }
    var showGroups: Boolean
        get() = _showGroups.value
        set(v) { _showGroups.value = v; prefs.edit().putBoolean("showGroups", v).apply() }
    var showCalendar: Boolean
        get() = _showCalendar.value
        set(v) { _showCalendar.value = v; prefs.edit().putBoolean("showCalendar", v).apply() }
    var showAnalog: Boolean
        get() = _showAnalog.value
        set(v) { _showAnalog.value = v; prefs.edit().putBoolean("showAnalog", v).apply() }
    var showPhotos: Boolean
        get() = _showPhotos.value
        set(v) { _showPhotos.value = v; prefs.edit().putBoolean("showPhotos", v).apply() }
    var showMusic: Boolean
        get() = _showMusic.value
        set(v) { _showMusic.value = v; prefs.edit().putBoolean("showMusic", v).apply() }
    var showSuggested: Boolean
        get() = _showSuggested.value
        set(v) { _showSuggested.value = v; prefs.edit().putBoolean("showSuggested", v).apply() }
    var showBattery: Boolean
        get() = _showBattery.value
        set(v) { _showBattery.value = v; prefs.edit().putBoolean("showBattery", v).apply() }
    var clock24h: Boolean
        get() = _clock24h.value
        set(v) { _clock24h.value = v; prefs.edit().putBoolean("clock24h", v).apply() }
    var clockSize: Float
        get() = _clockSize.floatValue
        set(v) { _clockSize.floatValue = v; prefs.edit().putFloat("clockSize", v).apply() }

    val hidden: Set<String> get() = _hidden.value
    fun hide(key: String) = setHidden(_hidden.value + key)
    fun unhide(key: String) = setHidden(_hidden.value - key)
    private fun setHidden(v: Set<String>) {
        _hidden.value = v
        prefs.edit().putStringSet("hidden", v).apply()
    }

    var libraryName: String
        get() = _libraryName.value
        set(v) {
            val name = v.trim().ifBlank { "App Library" }
            _libraryName.value = name
            prefs.edit().putString("libraryName", name).apply()
        }

    fun addWidgetId(id: Int, toLibrary: Boolean = false) {
        (if (toLibrary) libraryWidgetIds else widgetIds).add(id)
        saveWidgets()
    }
    fun removeWidgetId(id: Int) { widgetIds.remove(id); libraryWidgetIds.remove(id); saveWidgets() }
    private fun saveWidgets() = prefs.edit()
        .putString("widgetIds", widgetIds.joinToString(","))
        .putString("libraryWidgetIds", libraryWidgetIds.joinToString(","))
        .apply()
}
