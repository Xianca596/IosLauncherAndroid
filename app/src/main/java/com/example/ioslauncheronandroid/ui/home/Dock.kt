package com.example.ioslauncheronandroid.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import com.example.ioslauncheronandroid.ui.motion.Motion
import kotlin.math.abs
import kotlin.math.exp

@Composable
fun Dock(apps: List<AppInfo>, modifier: Modifier = Modifier, tick: (() -> Float)? = null) {
    var touchX by remember { mutableFloatStateOf(Float.NaN) }
    var active by remember { mutableStateOf(false) }
    var dockSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val breathing by animateFloatAsState(
        targetValue = if (active && LauncherSettings.dockMagnify) 1f else 0f,
        animationSpec = Motion.Dock,
        label = "dockBreath",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { dockSize = it }

            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            active = true
                            touchX = change.position.x
                        } else if (event.changes.none { it.pressed }) {
                            active = false
                            touchX = Float.NaN
                        }
                    }
                }
            }
            .graphicsLayer {
                translationY = -breathing * 4.dp.toPx()
                scaleX = 1f + breathing * 0.012f
                scaleY = 1f + breathing * 0.018f
            }
            .liquidGlass(36.dp, elevation = 16, tick = { breathing + (tick?.invoke() ?: 0f) })
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .height(78.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            val count = apps.size.coerceAtLeast(1)
            val slot = if (dockSize.width > 0) dockSize.width.toFloat() / count else 0f
            apps.forEachIndexed { index, app ->
                DockSlot(
                    app = app,
                    index = index,
                    slotWidth = slot,
                    touchX = touchX,
                    breathing = breathing,
                    densityPxPerDp = density.density,
                )
            }
        }
    }
}

@Composable
private fun DockSlot(
    app: AppInfo,
    index: Int,
    slotWidth: Float,
    touchX: Float,
    breathing: Float,
    densityPxPerDp: Float,
) {
    val center = slotWidth * (index + 0.5f)
    val dist = if (touchX.isNaN() || slotWidth == 0f) {
        999f
    } else {
        abs(touchX - center) / (56f * densityPxPerDp)
    }
    val target = 1f + (0.42f * exp(-dist * dist * 0.85f) * breathing)
    val rise by animateFloatAsState(target, Motion.Dock, label = "dockMag$index")
    AppIcon(
        app = app,
        showLabel = false,
        iconSize = 58.dp,
        scaleBoost = rise,
        modifier = Modifier.graphicsLayer {
            translationY = -(rise - 1f) * 22.dp.toPx()
        },
    )
}
