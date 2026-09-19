package com.example.ioslauncheronandroid.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.FolderItem
import com.example.ioslauncheronandroid.data.HomeLayout
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import kotlinx.coroutines.launch

@Composable
fun FolderOverlay(folderId: String, from: Rect, apps: Map<String, AppInfo>, onClose: () -> Unit) {
    val folder = HomeLayout.items.firstOrNull { it.id == folderId } as? FolderItem
    LaunchedEffect(folder == null) { if (folder == null) onClose() }
    if (folder == null) return

    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 360f)) }
    val close: () -> Unit = { scope.launch { progress.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 500f)); onClose() } }
    BackHandler(onBack = close)

    var name by remember(folder.id) { mutableStateOf(TextFieldValue(folder.name)) }
    val list = folder.keys.mapNotNull { apps[it] }
    val rows = (list.size + 2) / 3
    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f * progress.value))
            .pointerInput(Unit) { detectTapGestures { close() } },
    ) {
        val cx = with(density) { maxWidth.toPx() / 2f }
        val cy = with(density) { maxHeight.toPx() / 2f }
        val panelW = maxWidth * 0.86f
        val panelWpx = with(density) { panelW.toPx() }
        Column(
            Modifier
                .align(Alignment.Center)
                .width(panelW)
                .graphicsLayer {
                    val p = progress.value
                    val s = lerp(from.width / panelWpx, 1f, p)
                    scaleX = s; scaleY = s
                    translationX = lerp(from.center.x - cx, 0f, p)
                    translationY = lerp(from.center.y - cy, 0f, p)
                    alpha = (p * 1.6f).coerceIn(0f, 1f)
                }
                .liquidGlass(36.dp, elevation = 18, lift = 0.08f, tick = { progress.value })
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.graphicsLayer { alpha = ((progress.value - 0.35f) / 0.65f).coerceIn(0f, 1f) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it; HomeLayout.renameFolder(folder.id, it.text.take(20).ifBlank { "Folder" }) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                )
                list.chunked(3).forEach { r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        r.forEach { app -> AppIcon(app, onLaunch = { onClose() }) }
                        repeat(3 - r.size) { Box(Modifier.width(62.dp)) }
                    }
                }
            }
        }
    }
}
