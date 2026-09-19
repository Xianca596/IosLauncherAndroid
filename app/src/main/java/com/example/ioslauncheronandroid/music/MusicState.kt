package com.example.ioslauncheronandroid.music

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat

object MusicState {
    var hasAccess by mutableStateOf(false)
        private set
    var title by mutableStateOf<String?>(null)
        private set
    var artist by mutableStateOf<String?>(null)
        private set
    var art by mutableStateOf<ImageBitmap?>(null)
        private set
    var playing by mutableStateOf(false)
        private set

    private var manager: MediaSessionManager? = null
    private var listener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = show(metadata)
        override fun onPlaybackStateChanged(state: PlaybackState?) { playing = state?.state == PlaybackState.STATE_PLAYING }
        override fun onSessionDestroyed() = attach(null)
    }

    fun start(context: Context) {
        stop()
        val app = context.applicationContext
        hasAccess = NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)
        if (!hasAccess) return
        val sm = app.getSystemService(MediaSessionManager::class.java) ?: return
        val component = ComponentName(app, MusicListenerService::class.java)
        val l = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }
        runCatching {
            sm.addOnActiveSessionsChangedListener(l, component)
            manager = sm
            listener = l
            pick(sm.getActiveSessions(component))
        }
    }

    fun stop() {
        listener?.let { runCatching { manager?.removeOnActiveSessionsChangedListener(it) } }
        listener = null
        manager = null
        attach(null)
    }

    private fun pick(sessions: List<MediaController>?) {
        attach(sessions?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions?.firstOrNull())
    }

    private fun attach(c: MediaController?) {
        controller?.unregisterCallback(callback)
        controller = c
        c?.registerCallback(callback)
        show(c?.metadata)
        playing = c?.playbackState?.state == PlaybackState.STATE_PLAYING
    }

    private fun show(m: MediaMetadata?) {
        title = m?.getString(MediaMetadata.METADATA_KEY_TITLE)
        artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        art = (m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART))?.asImageBitmap()
    }

    fun playPause() { controller?.transportControls?.let { if (playing) it.pause() else it.play() } }
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
}
