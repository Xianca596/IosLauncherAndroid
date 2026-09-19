package com.example.ioslauncheronandroid.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class WallpaperTarget(val label: String) { Lock("Lock screen"), Home("Home screen"), Both("Both") }

object WallpaperStore {
    val files = SnapshotStateList<File>()
    val rotation = SnapshotStateList<String>()

    val changeTick = mutableIntStateOf(0)
    private val _rotateOnUnlock = mutableStateOf(false)
    private lateinit var appContext: Context

    var rotateOnUnlock: Boolean
        get() = _rotateOnUnlock.value
        set(v) { _rotateOnUnlock.value = v; prefs().edit().putBoolean("rotate", v).apply() }

    private fun prefs() = appContext.getSharedPreferences("wallpapers", Context.MODE_PRIVATE)
    private fun dir() = File(appContext.filesDir, "wallpapers").also { it.mkdirs() }

    fun init(context: Context) {
        appContext = context.applicationContext
        _rotateOnUnlock.value = prefs().getBoolean("rotate", false)
        reload()
        rotation.clear()
        rotation.addAll(prefs().getString("rotation", "").orEmpty().split('|').filter { it.isNotBlank() })
    }

    private fun reload() {
        files.clear()
        files.addAll(dir().listFiles().orEmpty().sortedByDescending { it.lastModified() })
    }

    suspend fun add(uris: List<Uri>) = withContext(Dispatchers.IO) {
        uris.forEach { uri ->
            runCatching {
                val out = File(dir(), "${UUID.randomUUID()}.img")
                appContext.contentResolver.openInputStream(uri)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            }
        }
        withContext(Dispatchers.Main) { reload() }
    }

    fun remove(names: Collection<String>) {
        names.forEach { File(dir(), it).delete() }
        rotation.removeAll(names.toSet())
        saveRotation()
        reload()
    }

    fun addToRotation(names: Collection<String>) {
        names.filter { it !in rotation }.forEach { rotation.add(it) }
        saveRotation()
    }

    fun removeFromRotation(names: Collection<String>) {
        rotation.removeAll(names.toSet())
        saveRotation()
    }

    private fun saveRotation() = prefs().edit().putString("rotation", rotation.joinToString("|")).apply()

    suspend fun decode(file: File, maxW: Int, maxH: Int): Bitmap? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return@withContext null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxW && bounds.outHeight / (sample * 2) >= maxH) sample *= 2
        val src = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return@withContext null
        val target = maxW.toFloat() / maxH
        val cw: Int
        val ch: Int
        if (src.width.toFloat() / src.height > target) { ch = src.height; cw = (ch * target).toInt() }
        else { cw = src.width; ch = (cw / target).toInt() }
        Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, cw, ch)
    }

    suspend fun apply(file: File, target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        val dm = appContext.resources.displayMetrics
        val bmp = decode(file, dm.widthPixels, dm.heightPixels) ?: return@withContext false
        val flags = when (target) {
            WallpaperTarget.Lock -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.Home -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.Both -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
        }
        val ok = runCatching { WallpaperManager.getInstance(appContext).setBitmap(bmp, null, true, flags) }.isSuccess
        if (ok && target != WallpaperTarget.Lock) withContext(Dispatchers.Main) { changeTick.intValue++ }
        ok
    }

    suspend fun rotateNext() {
        if (!rotateOnUnlock) return
        val names = rotation.filter { File(dir(), it).exists() }
        if (names.isEmpty()) return
        val idx = (prefs().getInt("rotateIndex", -1) + 1) % names.size
        prefs().edit().putInt("rotateIndex", idx).apply()
        apply(File(dir(), names[idx]), WallpaperTarget.Lock)
    }
}
