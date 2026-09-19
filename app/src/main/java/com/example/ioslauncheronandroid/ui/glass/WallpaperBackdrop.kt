package com.example.ioslauncheronandroid.ui.glass

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.roundToInt

class WallpaperImages(val sharp: ImageBitmap, val soft: ImageBitmap)

object WallpaperBackdrop {
    private const val SHARP_WIDTH = 720
    private const val SOFT_WIDTH = 180

    fun currentId(context: Context): Int =
        runCatching { WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)

    fun load(context: Context, screen: IntSize): WallpaperImages? = try {
        val src = WallpaperManager.getInstance(context).drawable?.toBitmap() ?: return null
        val target = screen.width.toFloat() / screen.height
        val cw: Int
        val ch: Int
        if (src.width.toFloat() / src.height > target) {
            ch = src.height; cw = (ch * target).roundToInt()
        } else {
            cw = src.width; ch = (cw / target).roundToInt()
        }
        val cropped = Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, cw, ch)
        val sharp = Bitmap.createScaledBitmap(cropped, SHARP_WIDTH, (SHARP_WIDTH / target).roundToInt(), true)
        val small = Bitmap.createScaledBitmap(cropped, SOFT_WIDTH, (SOFT_WIDTH / target).roundToInt(), true)
        WallpaperImages(sharp.asImageBitmap(), boxBlur(small, radius = 5, passes = 3).asImageBitmap())
    } catch (_: Throwable) {
        null
    }

    private fun boxBlur(bmp: Bitmap, radius: Int, passes: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        var a = IntArray(w * h).also { bmp.getPixels(it, 0, w, 0, 0, w, h) }
        var b = IntArray(w * h)
        repeat(passes) {
            pass(a, b, w, h, radius, horizontal = true)
            pass(b, a, w, h, radius, horizontal = false)
        }
        return Bitmap.createBitmap(a, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val len = if (horizontal) w else h
        val lines = if (horizontal) h else w
        val div = 2 * r + 1
        for (line in 0 until lines) {
            val base = if (horizontal) line * w else line
            val step = if (horizontal) 1 else w
            var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) {
                val p = src[base + i.coerceIn(0, len - 1) * step]
                sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
            }
            for (i in 0 until len) {
                dst[base + i * step] = (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = src[base + (i + r + 1).coerceAtMost(len - 1) * step]
                val rem = src[base + (i - r).coerceAtLeast(0) * step]
                sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                sb += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }
}
