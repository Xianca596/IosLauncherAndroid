package com.example.ioslauncheronandroid.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import com.example.ioslauncheronandroid.ui.motion.Motion
import kotlin.math.roundToInt

class MenuAction(val label: String, val glyph: String = "", val destructive: Boolean = false, val onClick: () -> Unit)
class WidgetMenu(val rect: Rect, val actions: List<MenuAction>)

val LocalWidgetMenu = compositionLocalOf<(WidgetMenu) -> Unit> { { } }

private val RowH = 48.dp
private val MenuW = 236.dp

@Composable
fun GlassContextMenu(menu: WidgetMenu, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, Motion.Snappy) }
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        val screenW = constraints.maxWidth
        val screenH = constraints.maxHeight
        val margin = with(density) { 14.dp.toPx() }
        val gap = with(density) { 10.dp.toPx() }
        val menuW = with(density) { MenuW.toPx() }
        val menuH = with(density) { (RowH * menu.actions.size).toPx() }
        val r = menu.rect

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f * progress.value)))

        Box(
            Modifier
                .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                .graphicsLayer { alpha = progress.value }
                .border(2.dp, Color.White.copy(0.7f), RoundedCornerShape(28.dp)),
        )
        val below = r.bottom + gap + menuH <= screenH - margin
        val y = (if (below) r.bottom + gap else r.top - gap - menuH).coerceIn(margin, screenH - margin - menuH)
        val x = (if (r.left + menuW <= screenW - margin) r.left else r.right - menuW).coerceIn(margin, screenW - margin - menuW)
        Column(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(MenuW)
                .graphicsLayer {
                    alpha = progress.value.coerceIn(0f, 1f)
                    val s = 0.86f + 0.14f * progress.value
                    scaleX = s; scaleY = s
                    transformOrigin = TransformOrigin(0.2f, if (below) 0f else 1f)
                }
                .liquidGlass(18.dp, elevation = 16, lift = 0.02f, tick = { progress.value })
                .background(Color(0xFF101014).copy(alpha = 0.42f))
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            menu.actions.forEachIndexed { i, a ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(0.6.dp).background(Color.White.copy(0.16f)))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(RowH)
                        .pointerInput(a) { detectTapGestures { a.onClick(); onDismiss() } }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(a.label, color = if (a.destructive) Color(0xFFFF6961) else Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (a.glyph.isNotEmpty()) Text(a.glyph, color = if (a.destructive) Color(0xFFFF6961) else Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
