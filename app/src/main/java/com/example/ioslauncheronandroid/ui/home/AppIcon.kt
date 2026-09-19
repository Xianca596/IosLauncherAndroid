package com.example.ioslauncheronandroid.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.music.BadgeState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.motion.Motion

@Composable
fun AppIcon(
    app: AppInfo,
    modifier: Modifier = Modifier,
    iconSize: Dp = LauncherSettings.iconSize.dp,
    showLabel: Boolean = LauncherSettings.showLabels,
    scaleBoost: Float = 1f,
    onLaunch: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val openMenu = LocalIconMenu.current
    val launchApp = LocalLaunch.current
    val editing = LocalEditMode.current
    val jiggle = LocalJiggle.current
    val removeApp = LocalRemoveApp.current
    val dir = if (app.key.hashCode() % 2 == 0) 1f else -1f
    val amp = 1.5f + (kotlin.math.abs(app.key.hashCode()) % 3) * 0.5f

    val bounds = remember { arrayOf(Rect.Zero) }
    val iconBounds = remember { arrayOf(Rect.Zero) }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = Motion.Bouncy,
        label = "press",
    )
    val lift by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = Motion.Snappy,
        label = "lift",
    )
    Column(
        modifier = modifier
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
            .pointerInput(app, editing) {
                if (editing) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        pressed = false
                    },
                    onLongPress = {
                        if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        openMenu(app, iconBounds[0])
                    },
                    onTap = {
                        if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLaunch?.invoke()
                        launchApp(app, iconBounds[0])
                    },
                )
            }
            .graphicsLayer {
                val s = pressScale * scaleBoost
                scaleX = s
                scaleY = s
                translationY = -lift * 6.dp.toPx()
                if (editing) rotationZ = (jiggle() - 0.5f) * 2f * amp * dir
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(iconSize).onGloballyPositioned { iconBounds[0] = it.boundsInWindow() },
            )
            val badge = BadgeState.counts[app.component.packageName] ?: 0
            if (badge > 0 && !editing) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .background(Color(0xFFFF3B30), CircleShape)
                        .border(1.5.dp, Color.White.copy(0.9f), CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (badge > 99) "99+" else "$badge", color = Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            }
            if (editing) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-5).dp, y = (-5).dp)
                        .size(22.dp)
                        .background(Color(0xFF3A3A3C), CircleShape)
                        .border(1.dp, Color.White.copy(0.35f), CircleShape)
                        .pointerInput(app) { detectTapGestures { removeApp(app) } },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(width = 10.dp, height = 2.dp).background(Color.White, RoundedCornerShape(1.dp))) }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                ),
                modifier = Modifier
                    .width(iconSize + 10.dp)
                    .graphicsLayer { alpha = (2f - scaleBoost).coerceIn(0.35f, 1f) },
            )
        }
    }
}
