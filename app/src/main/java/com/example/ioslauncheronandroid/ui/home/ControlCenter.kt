package com.example.ioslauncheronandroid.ui.home

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private fun Context.findActivity(): android.app.Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
internal fun ControlCenter(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val audio = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val activity = remember(context) { context.findActivity() }

    val maxVolume = remember {
        audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }

    var volume by remember {
        mutableFloatStateOf(
            audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        )
    }

    var brightness by remember { mutableFloatStateOf(0.65f) }

    fun openSettings(action: String) {
        try {
            context.startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) { }
        }
    }

    fun openApp(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            openSettings(Settings.ACTION_SETTINGS)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 38.dp, bottom = 18.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF238414D),
                            Color(0xF21D222B),
                            Color(0xF229303B)
                        )
                    )
                )
                .clickable(enabled = false) { }
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Control Center",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Quick controls",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
                Text(
                    "×",
                    color = Color.White,
                    fontSize = 30.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    Modifier
                        .weight(1.15f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.13f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ControlTile(
                            "◉", "Wi-Fi", Modifier.weight(1f), Color(0xFF168BFF)
                        ) { openSettings(Settings.ACTION_WIFI_SETTINGS) }
                        ControlTile(
                            "ᛒ", "Bluetooth", Modifier.weight(1f), Color(0xFF168BFF)
                        ) { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ControlTile(
                            "✈", "Airplane", Modifier.weight(1f), Color(0xFF555D6B)
                        ) { openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }
                        ControlTile(
                            "▥", "Mobile data", Modifier.weight(1f), Color(0xFF168BFF)
                        ) { openSettings(Settings.ACTION_DATA_ROAMING_SETTINGS) }
                    }
                }

                Column(
                    Modifier
                        .weight(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.13f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    ControlTile(
                        "◐", "Focus", Modifier.fillMaxWidth(), Color(0xFF7656C8)
                    ) { openSettings(Settings.ACTION_SOUND_SETTINGS) }
                    ControlTile(
                        "⌁", "Display", Modifier.fillMaxWidth(), Color(0xFF777F8D)
                    ) { openSettings(Settings.ACTION_DISPLAY_SETTINGS) }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color.White.copy(alpha = 0.13f))
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                Text(
                    "Brightness",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☀", color = Color.White, fontSize = 19.sp)
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            activity?.let { act ->
                                val params = act.window.attributes
                                params.screenBrightness = it.coerceIn(0.05f, 1f)
                                act.window.attributes = params
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                }

                Text(
                    "Volume",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("◖", color = Color.White, fontSize = 19.sp)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            audio.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (it * maxVolume).roundToInt().coerceIn(0, maxVolume),
                                0
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color.White.copy(alpha = 0.13f))
                    .padding(15.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "♫  Now Playing",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("•••", color = Color.White.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "No media playing",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlTile(
                    "◷", "Timer", Modifier.weight(1f), Color(0xFF68758C)
                ) {
                    openApp(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                }
                ControlTile(
                    "▦", "Calculator", Modifier.weight(1f), Color(0xFF68758C)
                ) {
                    openApp(
                        Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_APP_CALCULATOR)
                    )
                }
                ControlTile(
                    "◎", "Camera", Modifier.weight(1f), Color(0xFF68758C)
                ) {
                    openApp(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                }
            }

            Text(
                "Tap outside the panel to close",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ControlTile(
    symbol: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF596273),
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(17.dp))
            .background(color.copy(alpha = 0.85f))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
