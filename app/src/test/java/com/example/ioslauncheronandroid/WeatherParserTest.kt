package com.example.ioslauncheronandroid

import com.example.ioslauncheronandroid.weather.WeatherParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherParserTest {
    private val sample = """
    {"current":{"time":"2026-09-19T16:45","temperature_2m":29.4,"weather_code":2,"is_day":1},
     "daily":{"temperature_2m_max":[31.2,30.0],"temperature_2m_min":[24.1,23.0]},
     "hourly":{"time":["2026-09-19T15:00","2026-09-19T16:00","2026-09-19T17:00","2026-09-19T18:00","2026-09-19T19:00","2026-09-19T20:00","2026-09-19T21:00","2026-09-19T22:00"],
       "temperature_2m":[30,29,28,27,26,25,24,24],"weather_code":[2,2,3,61,61,3,0,0],"is_day":[1,1,1,1,0,0,0,0]}}
    """.trimIndent()

    @Test fun parsesCurrentAndDailyRange() {
        val w = WeatherParser.parse(sample, "Chennai", 1L)
        assertEquals("Chennai", w.city)
        assertEquals(29.4, w.tempC, 0.001)
        assertEquals(31.2, w.highC, 0.001)
        assertEquals(24.1, w.lowC, 0.001)
        assertTrue(w.isDay)
    }

    @Test fun hourlyStartsAtTheCurrentHour_andHasSix() {
        val w = WeatherParser.parse(sample, "X", 1L)
        assertEquals(6, w.hours.size)
        assertEquals(16, w.hours.first().hour)
        assertEquals(61, w.hours[2].code)
    }

    @Test fun cacheRoundTrips() {
        val w = WeatherParser.parse(sample, "Chennai", 42L)
        assertEquals(w, WeatherParser.fromCache(WeatherParser.toCache(w)))
    }

    @Test fun describesConditionsAndNight() {
        assertEquals("Rain", WeatherParser.describe(63, true).second)
        assertEquals("🌙", WeatherParser.describe(0, false).first)
    }
}
