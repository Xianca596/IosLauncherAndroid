package com.example.ioslauncheronandroid

import com.example.ioslauncheronandroid.data.LauncherAction
import com.example.ioslauncheronandroid.data.SettingsIndex
import com.example.ioslauncheronandroid.data.SettingsTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsIndexTest {
    @Test
    fun blankQuery_returnsNothing() {
        assertTrue(SettingsIndex.search("").isEmpty())
        assertTrue(SettingsIndex.search("   ").isEmpty())
    }

    @Test
    fun matchesTitlesAndKeywords_caseInsensitively() {
        assertTrue(SettingsIndex.search("WIFI").any { it.title.startsWith("Wi") })
        assertTrue(SettingsIndex.search("brightness").any { it.title == "Display" })
    }

    @Test
    fun everyWordMustMatch() {
        assertTrue(SettingsIndex.search("wifi banana").isEmpty())
    }

    @Test
    fun launcherScreensAreSearchable() {
        val hit = SettingsIndex.search("lock screen").first { it.target is SettingsTarget.Launcher }
        assertEquals(LauncherAction.Wallpapers, (hit.target as SettingsTarget.Launcher).action)
    }
}
