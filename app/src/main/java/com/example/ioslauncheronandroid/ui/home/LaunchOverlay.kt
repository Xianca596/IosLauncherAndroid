package com.example.ioslauncheronandroid.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import com.example.ioslauncheronandroid.data.AppInfo
import kotlin.math.min
import kotlin.math.roundToInt

val LocalLaunch = compositionLocalOf<(AppInfo, Rect) -> Unit> { { _, _ -> } }

class LaunchInfo(val app: AppInfo, val rect: Rect, val color: Color)

@Composable
fun LaunchOverlay(info: LaunchInfo, progress: () -> Float) {
    Canvas(Modifier.fillMaxSize()) {
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0.001f) return@Canvas
        val r = info.rect
        val l = lerp(r.left, 0f, p)
        val t = lerp(r.top, 0f, p)
        val rr = lerp(r.right, size.width, p)
        val b = lerp(r.bottom, size.height, p)
        val fillAlpha = (p / 0.22f).coerceIn(0f, 1f)
        drawRoundRect(
            color = info.color.copy(alpha = fillAlpha),
            topLeft = Offset(l, t),
            size = Size(rr - l, b - t),
            cornerRadius = CornerRadius(lerp(r.width * 0.225f, 0f, p)),
        )

        val glyph = lerp(r.width, min(size.width, size.height) * 0.26f, p)
        val cx = lerp(r.center.x, size.width / 2f, p)
        val cy = lerp(r.center.y, size.height / 2f, p)
        drawImage(
            image = info.app.icon,
            dstOffset = IntOffset((cx - glyph / 2f).roundToInt(), (cy - glyph / 2f).roundToInt()),
            dstSize = IntSize(glyph.roundToInt(), glyph.roundToInt()),
            alpha = (1f - (p - 0.35f) / 0.5f).coerceIn(0f, 1f),
        )
    }
}
