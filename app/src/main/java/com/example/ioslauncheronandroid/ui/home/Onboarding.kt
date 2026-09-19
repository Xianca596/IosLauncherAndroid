package com.example.ioslauncheronandroid.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.motion.Motion
import com.example.ioslauncheronandroid.ui.widgets.LocalController

@Composable
fun Onboarding(isDefault: Boolean, hasWallpaper: Boolean, needsWallpaper: Boolean, onSkip: () -> Unit) {
    val controller = LocalController.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, Motion.Settle) }
    val allDone = isDefault && (hasWallpaper || !needsWallpaper)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * progress.value))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .graphicsLayer {
                    val s = 0.92f + 0.08f * progress.value
                    scaleX = s; scaleY = s; alpha = progress.value.coerceIn(0f, 1f)
                }
                .glass(RoundedCornerShape(32.dp), Color(0xFF1C1C1F).copy(alpha = 0.95f), 16)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Welcome", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                if (allDone) "You're all set." else "Two quick steps to get the full experience.",
                color = Color.White.copy(0.7f), fontSize = 15.sp,
            )
            Step(
                1, "Make it your home screen",
                "Pressing Home will open this launcher. Uninstall it any time and Android goes back to your previous one.",
                done = isDefault, action = "Set as default",
            ) { controller.requestDefaultLauncher() }
            if (needsWallpaper) {
                Step(
                    2, "Unlock Liquid Glass",
                    "Lets the dock and menus blur and bend your real wallpaper. Needs photo access, then all-files access.",
                    done = hasWallpaper, action = "Allow access",
                ) { controller.requestWallpaperAccess() }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassButton(if (allDone) "Get started" else "Not now", strong = allDone, onClick = onSkip)
            }
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String, done: Boolean, action: String, onClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(30.dp).background(if (done) Color(0xFF34C759) else Color.White.copy(0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(if (done) "✓" else "$number", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = Color.White.copy(0.65f), fontSize = 13.sp, lineHeight = 18.sp)
            if (!done) GlassButton(action, strong = true, onClick = onClick, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
