package com.example.ioslauncheronandroid.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlinx.coroutines.withTimeoutOrNull

suspend fun PointerInputScope.detectLongPressDragInitial(
    onStart: (Offset) -> Boolean,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        val stillHeld = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val c = ev.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                if (!c.pressed) return@withTimeoutOrNull false
                if ((c.position - down.position).getDistance() > slop) return@withTimeoutOrNull false
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
        if (stillHeld != null) return@awaitEachGesture
        if (!onStart(down.position)) return@awaitEachGesture
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val c = ev.changes.firstOrNull { it.id == down.id } ?: break

            val moved = c.positionChangeIgnoreConsumed() != Offset.Zero
            c.consume()
            if (!c.pressed) break
            if (moved) onMove(c.position)
        }
        onEnd()
    }
}

suspend fun PointerInputScope.detectLongPressInitial(onLongPress: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val c = ev.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                if (!c.pressed || (c.position - down.position).getDistance() > slop) return@withTimeoutOrNull true
            }
            @Suppress("UNREACHABLE_CODE")
            true
        }
        if (released != null) return@awaitEachGesture
        onLongPress(down.position)

        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
            c.consume()
            if (!c.pressed) break
        }
    }
}
