package com.example.ioslauncheronandroid.music

import android.app.Notification
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.mutableStateMapOf

object BadgeState {
    val counts = mutableStateMapOf<String, Int>()

    fun refresh(service: NotificationListenerService) {
        val fresh = HashMap<String, Int>()
        runCatching { service.activeNotifications }.getOrNull()?.forEach { sbn ->

            if (sbn.isOngoing || sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return@forEach
            fresh.merge(sbn.packageName, 1, Int::plus)
        }
        counts.keys.filter { it !in fresh }.forEach { counts.remove(it) }
        fresh.forEach { (pkg, n) -> if (counts[pkg] != n) counts[pkg] = n }
    }

    fun clear() = counts.clear()
}
