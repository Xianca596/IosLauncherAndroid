package com.example.ioslauncheronandroid.ui.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import kotlin.math.roundToInt

@Composable
fun SearchGlyph(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = size.toPx() * 0.13f
        drawCircle(Color.White.copy(0.85f), radius = this.size.minDimension * 0.34f, center = Offset(this.size.width * 0.42f, this.size.height * 0.42f), style = Stroke(w))
        drawLine(Color.White.copy(0.85f), Offset(this.size.width * 0.66f, this.size.height * 0.66f), Offset(this.size.width * 0.95f, this.size.height * 0.95f), w, StrokeCap.Round)
    }
}

@Composable
fun SearchMorph(progress: () -> Float, from: () -> Rect, to: () -> Rect) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset {
                    val p = progress().coerceIn(0f, 1.06f)
                    val a = from(); val b = to()
                    IntOffset(lerp(a.left, b.left, p).roundToInt(), lerp(a.top, b.top, p).roundToInt())
                }
                .layout { measurable, _ ->
                    val p = progress().coerceIn(0f, 1.06f)
                    val a = from(); val b = to()
                    val w = lerp(a.width, b.width, p).roundToInt().coerceAtLeast(1)
                    val h = lerp(a.height, b.height, p).roundToInt().coerceAtLeast(1)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) { placeable.place(0, 0) }
                }
                .graphicsLayer {
                    alpha = 1f - ((progress() - 0.94f) / 0.06f).coerceIn(0f, 1f)
                }
                .liquidGlass(20.dp, 8, lift = 0.14f, tick = progress),
        ) {
            SearchGlyph(
                20.dp,
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(lerp(14.dp.toPx(), 16.dp.toPx(), progress().coerceIn(0f, 1f)).roundToInt(), 0) }
                    .graphicsLayer {
                        val s = lerp(0.6f, 1f, progress().coerceIn(0f, 1f))
                        scaleX = s; scaleY = s
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
            )
            Text(
                "Search", color = Color.White.copy(0.85f), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterStart).offset { IntOffset(lerp(31.dp.toPx(), 44.dp.toPx(), progress().coerceIn(0f, 1f)).roundToInt(), 0) }
                    .graphicsLayer { alpha = (1f - progress() * 2.2f).coerceIn(0f, 1f) },
            )
            Text(
                "Search apps & settings", color = Color.White.copy(0.65f), fontSize = 17.sp,
                modifier = Modifier.align(Alignment.CenterStart).offset { IntOffset(lerp(31.dp.toPx(), 44.dp.toPx(), progress().coerceIn(0f, 1f)).roundToInt(), 0) }
                    .graphicsLayer { alpha = ((progress() - 0.45f) * 2.2f).coerceIn(0f, 1f) },
            )
        }
    }
}
