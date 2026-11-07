package com.example.ioslauncheronandroid.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf

object PhotoStore {
    val version = mutableIntStateOf(0)
    private lateinit var appContext: Context

    private fun prefs() =
        appContext.getSharedPreferences("selected_photos", Context.MODE_PRIVATE)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getUris(): List<Uri> {
        if (!::appContext.isInitialized) return emptyList()
        return prefs().getStringSet("uris", emptySet())
            .orEmpty()
            .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
    }

    fun add(uris: List<Uri>) {
        if (!::appContext.isInitialized) return

        val saved = prefs().getStringSet("uris", emptySet())
            .orEmpty().toMutableSet()

        uris.forEach { uri ->
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            saved.add(uri.toString())
        }

        prefs().edit().putStringSet("uris", saved).apply()
        version.intValue++
    }
}
