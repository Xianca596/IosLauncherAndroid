package com.example.ioslauncheronandroid.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import kotlinx.coroutines.launch

val LocalOpenGroup = compositionLocalOf<(String, List<AppInfo>, Rect) -> Unit> { { _, _, _ -> } }

@Composable
fun GroupOverlay(title: String, apps: List<AppInfo>, from: Rect, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 360f)) }
    val close: () -> Unit = { scope.launch { progress.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 500f)); onClose() } }
    BackHandler(onBack = close)
    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f * progress.value))
            .pointerInput(Unit) { detectTapGestures { close() } },
    ) {
        val cx = with(density) { maxWidth.toPx() / 2f }
        val cy = with(density) { maxHeight.toPx() / 2f }
        val panelW = maxWidth * 0.9f
        val panelWpx = with(density) { panelW.toPx() }
        Column(
            Modifier
                .align(Alignment.Center)
                .width(panelW)
                .heightIn(max = maxHeight * 0.66f)
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
                .padding(horizontal = 14.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("${apps.size} apps", color = Color.White.copy(0.6f), fontSize = 13.sp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = ((progress.value - 0.35f) / 0.65f).coerceIn(0f, 1f) },
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items(apps, key = { it.key }) { app ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(app, iconSize = 52.dp, onLaunch = { onClose() })
                    }
                }
            }
        }
    }
}
