package com.example.ioslauncheronandroid.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.platform.LocalDensity
import com.example.ioslauncheronandroid.ui.glass.BackgroundScale
import com.example.ioslauncheronandroid.ui.glass.ParallaxDp
import com.example.ioslauncheronandroid.ui.glass.Tilt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Row
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.widgets.WidgetPicker
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ioslauncheronandroid.ui.glass.Backdrop

@Composable
internal fun rememberJiggle(active: Boolean): () -> Float {
    if (!active) return NoJiggle
    val t = rememberInfiniteTransition(label = "jiggle")
    val v = t.animateFloat(0f, 1f, infiniteRepeatable(tween(115, easing = LinearEasing), RepeatMode.Reverse), label = "j")
    return remember(v) { { v.value } }
}

private val NoJiggle: () -> Float = { 0.5f }

@Composable
internal fun EditBar(onCustomize: () -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlassButton("Customize", onClick = onCustomize)
        GlassButton("Done", strong = true, onClick = onDone)
    }
}

enum class Screen { None, Settings, Wallpapers, WidgetPicker, LibraryWidgetPicker }

@Composable
internal fun HomeMenu(onDismiss: () -> Unit, onPick: (Screen) -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(0.35f)).pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.72f)
                .glass(RoundedCornerShape(28.dp), Color.Black.copy(0.3f), 12)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Wallpapers" to Screen.Wallpapers, "Add widget" to Screen.WidgetPicker, "Launcher settings" to Screen.Settings).forEach { (t, sc) ->
                Text(
                    t, color = Color.White, fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(16.dp), elevation = 0)
                        .pointerInput(sc) { detectTapGestures { onPick(sc) } }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
internal fun HomeBackground(backdrop: Backdrop?) {
    val soft = backdrop?.soft
    val sharp = backdrop?.sharp
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        when {
            soft != null -> Box(
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = BackgroundScale; scaleY = BackgroundScale
                    translationX = -Tilt.dx * ParallaxDp * density.density
                    translationY = -Tilt.dy * ParallaxDp * density.density
                },
            ) {
                val blurMix = (LauncherSettings.blurRadius / 110f).coerceIn(0f, 1f)
                if (sharp != null && blurMix < 0.999f) {
                    Image(sharp, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.Low)
                }
                Image(
                    soft, null,
                    Modifier.fillMaxSize().graphicsLayer { alpha = blurMix },
                    contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.Low,
                )
            }
            backdrop?.windowBlur != true -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B)))
                )
            )
        }

        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(LauncherSettings.dim * 0.6f), Color.Black.copy(LauncherSettings.dim * 1.4f)))))
    }
}
