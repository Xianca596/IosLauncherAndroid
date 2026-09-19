package com.example.ioslauncheronandroid.ui.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.motion.Motion
import kotlin.math.exp
import kotlin.math.roundToInt

val RailLetters: List<Char> = ('A'..'Z').toList() + '#'

@Composable
fun AlphabetRail(
    present: Set<Char>,
    onLetter: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var pos by remember { mutableFloatStateOf(-10f) }
    var active by remember { mutableIntStateOf(-1) }
    val engaged = animateFloatAsState(if (active >= 0) 1f else 0f, Motion.Snappy, label = "rail")
    val n = RailLetters.size

    fun update(y: Float) {
        if (size.height == 0) return
        val units = y / size.height * n
        pos = units - 0.5f
        val i = units.toInt().coerceIn(0, n - 1)
        if (i != active) {
            active = i
            if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onLetter(RailLetters[i])
        }
    }

    Box(
        modifier
            .width(24.dp)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    update(down.position.y)
                    drag(down.id) { it.consume(); update(it.position.y) }
                    active = -1
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            RailLetters.forEachIndexed { i, ch ->
                Box(
                    Modifier
                        .weight(1f)
                        .width(24.dp)
                        .graphicsLayer {
                            val d = i - pos
                            val bulge = exp(-d * d / (2f * 1.7f * 1.7f)) * engaged.value
                            translationX = -bulge * 22.dp.toPx()
                            scaleX = 1f + bulge * 0.9f
                            scaleY = scaleX
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ch.toString(),
                        color = Color.White.copy(alpha = if (ch in present) 0.95f else 0.3f),
                        style = TextStyle(
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                        ),
                        modifier = Modifier.wrapContentHeight(unbounded = true),
                    )
                }
            }
        }

        if (engaged.value > 0.02f && active >= 0) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((-92.dp.toPx()).roundToInt(), (pos / n * size.height + size.height / n / 2f - 30.dp.toPx()).roundToInt()) }
                    .size(60.dp)
                    .graphicsLayer {
                        alpha = engaged.value
                        val s = 0.6f + 0.4f * engaged.value
                        scaleX = s; scaleY = s
                    }
                    .glass(CircleShape, Color.White.copy(0.28f), 6),
                contentAlignment = Alignment.Center,
            ) {
                Text(RailLetters[active].toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
