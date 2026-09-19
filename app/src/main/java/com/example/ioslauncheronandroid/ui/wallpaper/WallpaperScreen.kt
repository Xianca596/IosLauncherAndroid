package com.example.ioslauncheronandroid.ui.wallpaper

import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import com.example.ioslauncheronandroid.ui.glass.GlassSheet
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.widgets.LocalController
import com.example.ioslauncheronandroid.wallpaper.WallpaperStore
import com.example.ioslauncheronandroid.wallpaper.WallpaperTarget
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private val cache = LruCache<String, ImageBitmap>(48)

@Composable
private fun rememberWallpaper(file: File, w: Int, h: Int): ImageBitmap? {
    val key = "${file.name}@$w"
    return produceState(cache.get(key), key) {
        if (value == null) {
            WallpaperStore.decode(file, w, h)?.asImageBitmap()?.also { cache.put(key, it); value = it }
        }
    }.value
}

@Composable
fun WallpaperScreen(onClose: () -> Unit) {
    val files = WallpaperStore.files
    val controller = LocalController.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val pager = rememberPagerState { files.size.coerceAtLeast(1) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var selecting by remember { mutableStateOf(false) }

    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    GlassSheet(
        "Wallpapers", onClose,
        actions = { GlassButton("+ Add", strong = true) { controller.pickWallpapers() } },
    ) {
        if (files.isEmpty()) {
            Text(
                "Add photos to build your wallpaper library.\nThen swipe through them, set the lock or home screen, or rotate the lock screen on every unlock.",
                color = Color.White.copy(0.75f), fontSize = 15.sp,
                modifier = Modifier.padding(28.dp),
            )
            return@GlassSheet
        }
        val names = remember(files.size) { files.map { it.name }.toSet() }
        fun hit(x: Float, y: Float): String? = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key in names && x >= it.offset.x && x <= it.offset.x + it.size.width &&
                y >= it.offset.y && y <= it.offset.y + it.size.height
        }?.key as? String

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            userScrollEnabled = !selecting,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().pointerInput(names) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { o ->
                        selecting = true
                        hit(o.x, o.y)?.let { selected = selected + it; haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                    },
                    onDrag = { c, _ ->
                        hit(c.position.x, c.position.y)?.let {
                            if (it !in selected) { selected = selected + it; haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
                        }
                    },
                    onDragEnd = { selecting = false },
                    onDragCancel = { selecting = false },
                )
            },
        ) {
            item(key = "carousel", span = { GridItemSpan(maxLineSpan) }) {
                HorizontalPager(
                    pager,
                    Modifier.fillMaxWidth().height(380.dp),
                    contentPadding = PaddingValues(horizontal = 84.dp),
                    pageSpacing = 14.dp,
                ) { page ->
                    val file = files.getOrNull(page)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val d = abs(pager.currentPage - page + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                                val s = 1f - 0.12f * d
                                scaleX = s; scaleY = s; alpha = 1f - 0.4f * d
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.Black.copy(0.3f)),
                    ) {
                        val bmp = file?.let { rememberWallpaper(it, 540, 1170) }
                        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Text("9:41", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Light,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 44.dp))
                    }
                }
            }
            item(key = "controls", span = { GridItemSpan(maxLineSpan) }) {
                androidx.compose.foundation.layout.Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        WallpaperTarget.entries.forEach { t ->
                            GlassButton(t.label, strong = t == WallpaperTarget.Lock) {
                                val f = files.getOrNull(pager.currentPage) ?: return@GlassButton
                                scope.launch {
                                    toast(if (WallpaperStore.apply(f, t)) "${t.label} updated" else "Couldn't set wallpaper")
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), elevation = 0).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text("Change lock screen on every unlock", color = Color.White, fontSize = 15.sp)
                            Text("${WallpaperStore.rotation.size} wallpapers in rotation", color = Color.White.copy(0.65f), fontSize = 12.sp)
                        }
                        Switch(
                            WallpaperStore.rotateOnUnlock, { WallpaperStore.rotateOnUnlock = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF34C759), checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(0.2f), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
                        )
                    }
                }
            }
            item(key = "hdr", span = { GridItemSpan(maxLineSpan) }) {
                androidx.compose.foundation.layout.Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (selected.isEmpty()) "Library — hold and swipe across photos to select many"
                        else "${selected.size} selected",
                        color = Color.White.copy(0.7f), fontSize = 13.sp,
                    )
                    if (selected.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton("Add to rotation", strong = true) {
                                WallpaperStore.addToRotation(selected); toast("Added ${selected.size} to rotation"); selected = emptySet()
                            }
                            GlassButton("Remove from rotation") { WallpaperStore.removeFromRotation(selected); selected = emptySet() }
                            GlassButton("Delete") { WallpaperStore.remove(selected); selected = emptySet() }
                            GlassButton("Clear") { selected = emptySet() }
                        }
                    }
                }
            }
            items(files, key = { it.name }) { file ->
                val isSel = file.name in selected
                val inRotation = file.name in WallpaperStore.rotation
                Box(
                    Modifier
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(0.08f))
                        .then(if (isSel) Modifier.border(3.dp, Color.White, RoundedCornerShape(16.dp)) else Modifier)
                        .pointerInput(file, selected.isNotEmpty()) {
                            detectTapGestures {
                                if (selected.isNotEmpty()) selected = if (isSel) selected - file.name else selected + file.name
                                else scope.launch {
                                    pager.animateScrollToPage(files.indexOf(file))
                                    gridState.animateScrollToItem(0)
                                }
                            }
                        },
                ) {
                    val bmp = rememberWallpaper(file, 240, 427)
                    if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (inRotation) Text("↻", color = Color.White, fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp).glass(RoundedCornerShape(50), elevation = 0).padding(horizontal = 7.dp, vertical = 2.dp))
                    if (isSel) Text("✓", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.White, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 2.dp))
                }
            }
        }
    }
}
