package com.example.ioslauncheronandroid.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.example.ioslauncheronandroid.data.LauncherSettings
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import com.example.ioslauncheronandroid.ui.motion.Motion

val IndicatorSlot = 48.dp

@Composable
fun PageIndicator(
    pager: PagerState,
    count: Int,
    showDots: Boolean,
    onSearch: () -> Unit,
    onScrub: (Boolean) -> Unit,
    enabled: Boolean = true,
    tapEnabled: Boolean = enabled,
    pillBounds: Array<androidx.compose.ui.geometry.Rect>? = null,
    modifier: Modifier = Modifier,
    tick: (() -> Float)? = null,
) {
    val dots by animateFloatAsState(if (showDots) 1f else 0f, Motion.Soft, label = "dots")
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    Box(
        modifier

            .width(240.dp)
            .pointerInput(count, enabled) {
                if (!enabled) return@pointerInput

                val spacing = 15.dp.toPx()
                fun indexAt(x: Float) = (((x - size.width / 2f) / spacing) + (count - 1) / 2f).roundToInt().coerceIn(0, count - 1)
                var last = -1
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        onScrub(true)
                        last = indexAt(off.x)
                        if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        scope.launch { pager.animateScrollToPage(last, animationSpec = Motion.Snappy) }
                    },
                    onDragEnd = { onScrub(false) },
                    onDragCancel = { onScrub(false) },
                ) { change, _ ->
                    change.consume()
                    val i = indexAt(change.position.x)
                    if (i != last) {
                        last = i
                        if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        scope.launch { pager.animateScrollToPage(i, animationSpec = Motion.Snappy) }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .graphicsLayer {
                    alpha = 1f - dots
                    val s = 1f - 0.15f * dots
                    scaleX = s; scaleY = s
                }
                .onGloballyPositioned { pillBounds?.set(0, it.boundsInWindow()) }
                .liquidGlass(null, elevation = 2, tick = tick)
                .pointerInput(tapEnabled) { if (tapEnabled) detectTapGestures { onSearch() } }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.example.ioslauncheronandroid.ui.search.SearchGlyph(12.dp)
            Text("Search", color = Color.White.copy(0.85f), fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp))
        }

        if (dots > 0.01f) {
            Box(
                Modifier
                    .graphicsLayer {
                        alpha = dots
                        val s = 0.85f + 0.15f * dots
                        scaleX = s; scaleY = s
                    }

                    .pointerInput(count) {
                        detectTapGestures { off ->
                            val spacing = 15.dp.toPx()
                            val i = (((off.x - size.width / 2f) / spacing) + (count - 1) / 2f).roundToInt().coerceIn(0, count - 1)
                            scope.launch { pager.animateScrollToPage(i, animationSpec = Motion.Snappy) }
                        }
                    },
            ) { PageDots(pager, count) }
        }
    }
}
