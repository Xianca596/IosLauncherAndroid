package com.example.ioslauncheronandroid.ui.glass

import com.example.ioslauncheronandroid.data.LauncherSettings
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border

@Stable
class Backdrop(val windowBlur: Boolean) {
    var sharp by mutableStateOf<ImageBitmap?>(null)

    var soft by mutableStateOf<ImageBitmap?>(null)
    var size by mutableStateOf(IntSize.Zero)
}

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

object GlassTokens {
    val Tint = Color.White.copy(alpha = 0.16f)
    val TintStrong = Color.White.copy(alpha = 0.24f)
    val Border = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.65f),
        0.45f to Color.White.copy(alpha = 0.08f),
        1f to Color.White.copy(alpha = 0.35f),
    )
    val Sheen = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.22f),
        0.5f to Color.White.copy(alpha = 0.02f),
        1f to Color.White.copy(alpha = 0.06f),
    )
}

fun Modifier.glass(shape: Shape, tint: Color = Color.Unspecified, elevation: Int = 12): Modifier =
    this
        .then(if (elevation > 0) Modifier.drawBehind { outerShadow(shape, elevation.dp.toPx()) } else Modifier)
        .clip(shape)
        .drawBehind {
            drawRect(if (tint == Color.Unspecified) Color.White.copy(alpha = LauncherSettings.glassAlpha) else tint)
            drawRect(GlassTokens.Sheen)
        }
        .border(1.dp, GlassTokens.Border, shape)

private fun DrawScope.outerShadow(shape: Shape, blurPx: Float) {
    if (android.os.Build.VERSION.SDK_INT < 28) return
    val path = when (val o = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> Path().apply { addRect(o.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
        is Outline.Generic -> o.path
    }
    clipPath(path, ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                setShadowLayer(blurPx * 1.4f, 0f, blurPx * 0.35f, android.graphics.Color.argb(70, 0, 0, 0))
            }
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
        }
    }
}
