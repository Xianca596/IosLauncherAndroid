package com.example.ioslauncheronandroid.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.AppItem
import com.example.ioslauncheronandroid.data.FolderItem
import com.example.ioslauncheronandroid.data.HomeItem
import com.example.ioslauncheronandroid.data.HomeLayout
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.Tilt
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.motion.Motion
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

const val GRID_ROWS = 6
val GridHPad = 12.dp
val GridTopPad = 16.dp
val CellIconTop = 4.dp

@Stable
class DragState {
    var item by mutableStateOf<HomeItem?>(null)
    var finger by mutableStateOf(Offset.Zero)
    var grab by mutableStateOf(Offset.Zero)
    var folderHoverId by mutableStateOf<String?>(null)
    val draggingId: String? get() = item?.id
    fun clear() { item = null; folderHoverId = null }
}

@Composable
fun HomeGrid(
    items: List<HomeItem>,
    apps: Map<String, AppInfo>,
    columns: Int,
    baseIndex: Int,
    drag: DragState,
    pageOffset: () -> Float,
    entrance: () -> Float,
    onOpenFolder: (FolderItem, Rect) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = GridHPad, end = GridHPad, top = GridTopPad)) {
        val cellW = maxWidth / columns
        val cellH = maxHeight / GRID_ROWS
        val cwPx = with(density) { cellW.roundToPx() }
        val chPx = with(density) { cellH.roundToPx() }
        items.forEachIndexed { i, item ->
            key(item.id) {
                val col = i % columns
                val row = i / columns
                val pos by animateIntOffsetAsState(
                    IntOffset(col * cwPx, row * chPx),
                    spring(dampingRatio = 0.76f, stiffness = 420f),
                    label = "cell",
                )
                val stagger = ((baseIndex + i) * 0.03f).coerceAtMost(0.45f)
                Box(
                    Modifier
                        .offset { pos }
                        .size(cellW, cellH)
                        .graphicsLayer {
                            val local = ((entrance() - stagger) / (1f - stagger)).coerceIn(0f, 1f)
                            val po = pageOffset()

                            translationX = -po * col * 22.dp.toPx() + Tilt.dx * 5.dp.toPx()
                            translationY = Tilt.dy * 5.dp.toPx() + (1f - local) * 36.dp.toPx() +
                                abs(po) * (row * 4.dp.toPx()) * sign(po + 0.0001f)
                            val s = 0.82f + 0.18f * local
                            scaleX = s; scaleY = s
                            alpha = local
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (drag.draggingId != item.id) {
                        when (item) {
                            is AppItem -> apps[item.key]?.let { AppIcon(it, Modifier.padding(top = CellIconTop)) }
                            is FolderItem -> FolderIcon(item, apps, drag.folderHoverId == item.id, onOpenFolder, Modifier.padding(top = CellIconTop))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderGlyph(folder: FolderItem, apps: Map<String, AppInfo>, size: Dp, hover: Boolean = false) {
    val scale by animateFloatAsState(if (hover) 1.16f else 1f, Motion.Bouncy, label = "folderHover")
    val mini = (size - 14.dp) / 3
    Column(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .glass(RoundedCornerShape(size * 0.24f), Color.White.copy(alpha = if (hover) 0.4f else 0.26f), 0)
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(3) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { c ->
                    val app = folder.keys.getOrNull(r * 3 + c)?.let { apps[it] }
                    Box(Modifier.size(mini)) { if (app != null) Image(app.icon, null, Modifier.fillMaxSize()) }
                }
            }
        }
    }
}

@Composable
private fun FolderIcon(
    folder: FolderItem,
    apps: Map<String, AppInfo>,
    hover: Boolean,
    onOpen: (FolderItem, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = LocalEditMode.current
    val jiggle = LocalJiggle.current
    val bounds = remember { arrayOf(Rect.Zero) }
    val dir = if (folder.id.hashCode() % 2 == 0) 1f else -1f
    val iconSize = LauncherSettings.iconSize.dp
    Column(
        modifier
            .graphicsLayer { if (editing) rotationZ = (jiggle() - 0.5f) * 2f * 2f * dir }
            .pointerInput(folder.id, editing) {
                if (editing) return@pointerInput
                detectTapGestures(onTap = { onOpen(folder, bounds[0]) })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(Modifier.onGloballyPositioned { bounds[0] = it.boundsInWindow() }) { FolderGlyph(folder, apps, iconSize, hover) }
            if (editing) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-5).dp, y = (-5).dp)
                        .size(22.dp)
                        .background(Color(0xFF3A3A3C), CircleShape)
                        .border(1.dp, Color.White.copy(0.35f), CircleShape)
                        .pointerInput(folder.id) { detectTapGestures { HomeLayout.removeFolder(folder.id) } },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(width = 10.dp, height = 2.dp).background(Color.White, RoundedCornerShape(1.dp))) }
            }
        }
        if (LauncherSettings.showLabels) {
            Spacer(Modifier.height(5.dp))
            Text(
                folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                style = TextStyle(color = Color.White, fontSize = 11.sp),
                modifier = Modifier.width(iconSize + 10.dp),
            )
        }
    }
}

@Composable
fun DragOverlay(drag: DragState, apps: Map<String, AppInfo>) {
    val item = drag.item ?: return
    val iconSize = LauncherSettings.iconSize.dp
    val px = with(LocalDensity.current) { iconSize.toPx() }
    Box(
        Modifier
            .offset {
                IntOffset(
                    (drag.finger.x - drag.grab.x - px / 2f).roundToInt(),
                    (drag.finger.y - drag.grab.y - px / 2f).roundToInt(),
                )
            }
            .size(iconSize)
            .graphicsLayer { scaleX = 1.14f; scaleY = 1.14f; alpha = 0.96f },
    ) {
        when (item) {
            is AppItem -> apps[item.key]?.let { Image(it.icon, null, Modifier.fillMaxSize()) }
            is FolderItem -> FolderGlyph(item, apps, iconSize)
        }
    }
}
