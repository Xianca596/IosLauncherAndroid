package com.example.ioslauncheronandroid

import android.content.pm.ApplicationInfo
import com.example.ioslauncheronandroid.data.WidgetCategorizer
import com.example.ioslauncheronandroid.data.WidgetCategory
import com.example.ioslauncheronandroid.data.closestMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartLogicTest {
    private fun cat(text: String, appCategory: Int = -1) = WidgetCategorizer.classify(text, appCategory)

    @Test fun groupsByName() {
        assertEquals(WidgetCategory.Productivity, cat("Calendar com.google.android.calendar"))
        assertEquals(WidgetCategory.Media, cat("Spotify music player com.spotify.music"))
        assertEquals(WidgetCategory.Utilities, cat("Clock com.miui.clock"))
        assertEquals(WidgetCategory.Finance, cat("PhonePe UPI payments"))
        assertEquals(WidgetCategory.Shopping, cat("Swiggy food delivery"))
        assertEquals(WidgetCategory.Travel, cat("Google Maps navigation"))
    }

    @Test fun fallsBackToStoreCategory_thenOther() {
        assertEquals(WidgetCategory.Social, cat("Zzzq", ApplicationInfo.CATEGORY_SOCIAL))
        assertEquals(WidgetCategory.News, cat("Zzzq", ApplicationInfo.CATEGORY_NEWS))
        assertEquals(WidgetCategory.Other, cat("Zzzq"))
    }

    @Test fun suggestsClosestSpelling() {
        val apps = listOf("Calculator", "Calendar", "Camera", "Chrome")
        assertEquals("Calculator", closestMatch("calculater", apps))
        assertEquals("Camera", closestMatch("camra", apps))
    }

    @Test fun noSuggestionForExactOrHopelessQueries() {
        val apps = listOf("Calculator", "Camera")
        assertNull(closestMatch("camera", apps))
        assertNull(closestMatch("zzzzzzzz", apps))
        assertNull(closestMatch("ab", apps))
    }

    @Test fun matchesAgainstIndividualWords() {
        assertEquals("Voice Calculator", closestMatch("calculater", listOf("Voice Calculator", "Files")))
    }
}
