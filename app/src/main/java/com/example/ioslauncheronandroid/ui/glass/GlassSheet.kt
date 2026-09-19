package com.example.ioslauncheronandroid.ui.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.ui.motion.Motion
import kotlinx.coroutines.launch

@Composable
fun GlassSheet(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { progress.animateTo(1f, Motion.Settle) }
    val close: () -> Unit = { scope.launch { progress.animateTo(0f, Motion.Snappy); onClose() } }
    BackHandler(onBack = close)

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f * progress.value))
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = (1f - progress.value) * size.height * 0.6f
                    alpha = progress.value.coerceIn(0f, 1f)
                }
                .statusBarsPadding()
                .padding(top = 8.dp)
                .glass(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), Color.Black.copy(0.5f), 0)
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                actions()
                GlassButton("Done", onClick = close)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, strong: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .liquidGlass(null, 0, lift = if (strong) 0.30f else 0.14f)
            .pointerInput(onClick) { detectTapGestures { onClick() } }
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
