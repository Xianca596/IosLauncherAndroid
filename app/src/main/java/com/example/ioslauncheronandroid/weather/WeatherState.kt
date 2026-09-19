package com.example.ioslauncheronandroid.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

data class HourForecast(val hour: Int, val tempC: Double, val code: Int, val isDay: Boolean)

data class Weather(
    val city: String,
    val tempC: Double,
    val code: Int,
    val isDay: Boolean,
    val highC: Double,
    val lowC: Double,
    val hours: List<HourForecast>,
    val fetchedAt: Long,
)

sealed interface WeatherStatus {
    data object Loading : WeatherStatus
    data object NeedsLocation : WeatherStatus
    data class Failed(val message: String) : WeatherStatus
    data class Ready(val weather: Weather) : WeatherStatus
}

object WeatherState {
    var status by mutableStateOf<WeatherStatus>(WeatherStatus.Loading)
        private set

    val version = mutableIntStateOf(0)

    private const val MAX_AGE_MS = 30 * 60_000L

    suspend fun refresh(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("weather", Context.MODE_PRIVATE)
        if (status !is WeatherStatus.Ready) {
            prefs.getString("cache", null)?.let { runCatching { WeatherParser.fromCache(it) }.getOrNull() }?.let { status = WeatherStatus.Ready(it) }
        }
        val cached = (status as? WeatherStatus.Ready)?.weather
        if (!force && cached != null && System.currentTimeMillis() - cached.fetchedAt < MAX_AGE_MS) return@withContext

        if (app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (cached == null) status = WeatherStatus.NeedsLocation
            return@withContext
        }
        val loc = findLocation(app)
        if (loc == null) {
            if (cached == null) status = WeatherStatus.Failed("Couldn't find your location")
            return@withContext
        }
        runCatching {
            val body = http(
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f".format(Locale.US, loc.latitude, loc.longitude) +
                    "&current=temperature_2m,weather_code,is_day&hourly=temperature_2m,weather_code,is_day" +
                    "&daily=temperature_2m_max,temperature_2m_min&forecast_days=2&timezone=auto",
            )
            val city = cityName(app, loc) ?: cached?.city ?: "My Location"
            val w = WeatherParser.parse(body, city, System.currentTimeMillis())
            prefs.edit().putString("cache", WeatherParser.toCache(w)).apply()
            status = WeatherStatus.Ready(w)
        }.onFailure { if (cached == null) status = WeatherStatus.Failed("Weather is unavailable right now") }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun findLocation(context: Context): Location? {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val last = lm.getProviders(true).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (last != null) return last
        if (Build.VERSION.SDK_INT < 30) return null
        val provider = lm.getProviders(true).firstOrNull() ?: return null
        return withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { cont ->
                runCatching { lm.getCurrentLocation(provider, null, context.mainExecutor) { cont.resume(it) } }.onFailure { cont.resume(null) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun cityName(context: Context, loc: Location): String? = runCatching {
        Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
            ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }.getOrNull()

    private fun http(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000
        c.readTimeout = 8_000
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }
}

object WeatherParser {
    fun parse(body: String, city: String, now: Long): Weather {
        val root = JSONObject(body)
        val cur = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")
        val days = hourly.getJSONArray("is_day")

        val nowHour = cur.getString("time").take(13)
        val start = (0 until times.length()).firstOrNull { times.getString(it).startsWith(nowHour) } ?: 0
        val hours = (start until minOf(start + 6, times.length())).map { i ->
            HourForecast(times.getString(i).substring(11, 13).toInt(), temps.getDouble(i), codes.getInt(i), days.getInt(i) == 1)
        }
        return Weather(
            city = city,
            tempC = cur.getDouble("temperature_2m"),
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
            highC = daily.getJSONArray("temperature_2m_max").getDouble(0),
            lowC = daily.getJSONArray("temperature_2m_min").getDouble(0),
            hours = hours,
            fetchedAt = now,
        )
    }

    fun toCache(w: Weather): String = JSONObject()
        .put("city", w.city).put("t", w.tempC).put("c", w.code).put("d", w.isDay).put("h", w.highC).put("l", w.lowC).put("at", w.fetchedAt)
        .put("hours", JSONArray(w.hours.map { JSONObject().put("h", it.hour).put("t", it.tempC).put("c", it.code).put("d", it.isDay) }))
        .toString()

    fun fromCache(s: String): Weather {
        val o = JSONObject(s)
        val arr = o.getJSONArray("hours")
        return Weather(
            o.getString("city"), o.getDouble("t"), o.getInt("c"), o.getBoolean("d"), o.getDouble("h"), o.getDouble("l"),
            (0 until arr.length()).map { arr.getJSONObject(it).let { j -> HourForecast(j.getInt("h"), j.getDouble("t"), j.getInt("c"), j.getBoolean("d")) } },
            o.getLong("at"),
        )
    }

    fun describe(code: Int, isDay: Boolean): Pair<String, String> = when (code) {
        0 -> (if (isDay) "☀️" else "🌙") to "Clear"
        1 -> (if (isDay) "🌤" else "🌙") to "Mostly clear"
        2 -> "⛅" to "Partly cloudy"
        3 -> "☁️" to "Cloudy"
        45, 48 -> "🌫" to "Fog"
        51, 53, 55, 56, 57 -> "🌦" to "Drizzle"
        61, 63, 65, 66, 67 -> "🌧" to "Rain"
        71, 73, 75, 77, 85, 86 -> "❄️" to "Snow"
        80, 81, 82 -> "🌧" to "Showers"
        95, 96, 99 -> "⛈" to "Thunderstorm"
        else -> "🌡" to "Weather"
    }
}
