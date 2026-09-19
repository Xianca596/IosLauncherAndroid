package com.example.ioslauncheronandroid.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.AppShortcut
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.motion.Motion
import kotlin.math.roundToInt

val LocalIconMenu = compositionLocalOf<(AppInfo, Rect) -> Unit> { { _, _ -> } }

val LocalEditMode = compositionLocalOf { false }

val LocalJiggle = compositionLocalOf<() -> Float> { { 0.5f } }

val LocalRemoveApp = compositionLocalOf<(AppInfo) -> Unit> { { } }

private val MenuBg = Color(0xFF2C2C2E)
private val Divider = Color.White.copy(alpha = 0.12f)
private val RowHeight = 48.dp
private val MenuWidth = 214.dp

@Composable
fun IconMenu(app: AppInfo, iconRect: Rect, onRemoveFromFolder: (() -> Unit)? = null, shortcuts: List<AppShortcut> = emptyList(), onDismiss: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, Motion.Snappy) }
    fun start(i: Intent) { runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }; onDismiss() }

    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val screenW = constraints.maxWidth
        val screenH = constraints.maxHeight
        val gap = with(density) { 8.dp.toPx() }
        val margin = with(density) { 12.dp.toPx() }
        val menuW = with(density) { MenuWidth.toPx() }
        val menuH = with(density) { (RowHeight * ((if (onRemoveFromFolder != null) 4 else 3) + shortcuts.size)).toPx() } + 6f
        val safeBottom = screenH - with(density) { 96.dp.toPx() }

        Box(Modifier.fillMaxSize().background(Color(0xFF1C1C1E).copy(alpha = 0.78f * progress.value)))

        Image(
            app.icon, null,
            Modifier
                .offset { IntOffset(iconRect.left.roundToInt(), iconRect.top.roundToInt()) }
                .size(with(density) { iconRect.width.toDp() }, with(density) { iconRect.height.toDp() })
                .graphicsLayer { val s = 1f + 0.08f * progress.value; scaleX = s; scaleY = s },
        )

        val below = iconRect.bottom + gap + menuH <= safeBottom
        val y = if (below) iconRect.bottom + gap else iconRect.top - gap - menuH
        val x = (if (iconRect.left + menuW <= screenW - margin) iconRect.left else iconRect.right - menuW)
            .coerceIn(margin, screenW - margin - menuW)
        Column(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(MenuWidth)
                .graphicsLayer {
                    alpha = progress.value.coerceIn(0f, 1f)
                    val s = 0.85f + 0.15f * progress.value
                    scaleX = s; scaleY = s
                    transformOrigin = TransformOrigin(if (x == iconRect.left) 0.15f else 0.85f, if (below) 0f else 1f)
                }
                .clip(RoundedCornerShape(14.dp))
                .background(MenuBg)
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            shortcuts.forEach { sc ->
                MenuRow(sc.label, "↗", Color.White) { sc.start(); onDismiss() }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            }
            if (onRemoveFromFolder != null) {
                MenuRow("Remove from Folder", "↩", Color.White) { onRemoveFromFolder(); onDismiss() }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            }
            MenuRow("App info", "ⓘ", Color.White) {
                start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.component.packageName}")))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            MenuRow("Hide from Home", "–", Color.White) { LauncherSettings.hide(app.key); onDismiss() }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            MenuRow("Remove App", "\uD83D\uDDD1", Color(0xFFFF453A)) {
                start(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.component.packageName}")))
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, glyph: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .pointerInput(onClick) { detectTapGestures { onClick() } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = color, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(glyph, color = color, fontSize = 16.sp)
    }
}

@Composable
fun RemoveConfirm(app: AppInfo, onCancel: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, Motion.Snappy) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * progress.value))
            .pointerInput(Unit) { detectTapGestures { onCancel() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(290.dp)
                .graphicsLayer { val s = 0.9f + 0.1f * progress.value; scaleX = s; scaleY = s; alpha = progress.value.coerceIn(0f, 1f) }
                .clip(RoundedCornerShape(20.dp))
                .background(MenuBg)
                .pointerInput(Unit) { detectTapGestures { } },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(app.icon, null, Modifier.padding(top = 20.dp).size(56.dp))
            Text("Delete \u201C${app.label}\u201D?", color = Color.White, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            Text("Deleting this app will also delete its data.", color = Color.White.copy(0.65f), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            ConfirmRow("Delete App", Color(0xFFFF453A)) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.component.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                onCancel()
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            ConfirmRow("Remove from Home Screen", Color(0xFF0A84FF)) { LauncherSettings.hide(app.key); onCancel() }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            ConfirmRow("Cancel", Color.White, onClick = onCancel)
        }
    }
}

@Composable
private fun ConfirmRow(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(50.dp).pointerInput(onClick) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = color, fontSize = 17.sp) }
}
