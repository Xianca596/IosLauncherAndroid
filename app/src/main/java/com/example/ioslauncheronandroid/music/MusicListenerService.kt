package com.example.ioslauncheronandroid.music

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MusicListenerService : NotificationListenerService() {
    override fun onListenerConnected() = BadgeState.refresh(this)
    override fun onListenerDisconnected() = BadgeState.clear()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = BadgeState.refresh(this)
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = BadgeState.refresh(this)
}
