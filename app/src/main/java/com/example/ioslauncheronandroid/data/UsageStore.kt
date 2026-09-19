package com.example.ioslauncheronandroid.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf

object UsageStore {
    private lateinit var prefs: SharedPreferences
    private val counts = HashMap<String, Int>()

    val version = mutableIntStateOf(0)

    fun init(context: Context) {
        prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)
        prefs.all.forEach { (k, v) -> if (v is Int) counts[k] = v }
    }

    fun record(key: String) {
        val n = (counts[key] ?: 0) + 1
        counts[key] = n
        if (::prefs.isInitialized) prefs.edit().putInt(key, n).apply()
        version.intValue++
    }

    fun top(n: Int): List<String> {
        version.intValue
        return counts.entries.sortedByDescending { it.value }.take(n).map { it.key }
    }
}
