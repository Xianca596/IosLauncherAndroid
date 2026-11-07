package com.example.ioslauncheronandroid.weather

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
    data class Failed(val message: String) : WeatherStatus
    data class Ready(val weather: Weather) : WeatherStatus
}

object WeatherState {
    var status by mutableStateOf<WeatherStatus>(WeatherStatus.Loading)
        private set

    val version = mutableIntStateOf(0)

    suspend fun refresh(context: Context, force: Boolean = false) =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences("weather", Context.MODE_PRIVATE)

            val cached = prefs.getString("cache", null)
                ?.let {
                    runCatching {
                        WeatherParser.fromCache(it)
                    }.getOrNull()
                }

            if (cached != null) {
                status = WeatherStatus.Ready(cached)
            } else {
                status = WeatherStatus.Failed(
                    "Location access is disabled. Weather data is unavailable."
                )
            }
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
