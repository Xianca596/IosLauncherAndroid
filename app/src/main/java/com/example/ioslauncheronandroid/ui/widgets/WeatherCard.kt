package com.example.ioslauncheronandroid.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.weather.Weather
import com.example.ioslauncheronandroid.weather.WeatherParser
import com.example.ioslauncheronandroid.weather.WeatherState
import com.example.ioslauncheronandroid.weather.WeatherStatus
import java.util.Locale
import kotlin.math.roundToInt

private val Shape = RoundedCornerShape(28.dp)
private val useFahrenheit get() = Locale.getDefault().country == "US"
private fun temp(c: Double): Int = (if (useFahrenheit) c * 9.0 / 5.0 + 32.0 else c).roundToInt()

private fun sky(code: Int, isDay: Boolean): Brush {
    val (top, bottom) = when {
        !isDay && code in 0..2 -> Color(0xFF0B1B3A) to Color(0xFF2B4A85)
        !isDay -> Color(0xFF1B2333) to Color(0xFF3A465C)
        code in 95..99 -> Color(0xFF2C3A4D) to Color(0xFF56657A)
        code in 61..67 || code in 80..82 || code in 51..57 -> Color(0xFF3B4D66) to Color(0xFF6F819B)
        code in 71..77 || code in 85..86 -> Color(0xFF7D91A6) to Color(0xFFC3D0DC)
        code == 45 || code == 48 -> Color(0xFF8593A0) to Color(0xFFB9C3CC)
        code == 3 -> Color(0xFF66798E) to Color(0xFF9EAEBF)
        code == 2 -> Color(0xFF3F86D6) to Color(0xFF86B8EA)
        else -> Color(0xFF1F78D8) to Color(0xFF63B4F4)
    }
    return Brush.verticalGradient(listOf(top, bottom))
}

@Composable
internal fun WeatherCard(modifier: Modifier = Modifier.fillMaxWidth(), compact: Boolean = false) {
    val context = LocalContext.current
    val controller = LocalController.current
    LaunchedEffect(WeatherState.version.intValue) { WeatherState.refresh(context) }
    when (val s = WeatherState.status) {
        is WeatherStatus.Ready -> WeatherContent(s.weather, modifier, compact)
        WeatherStatus.Loading -> Message("Loading…", null, null, {}, modifier, compact)
        is WeatherStatus.Failed -> Message("Weather", s.message, "Try again", { WeatherState.version.intValue++ }, modifier, compact)
    }
}

@Composable
private fun WeatherContent(w: Weather, modifier: Modifier, compact: Boolean) {
    val (emoji, label) = WeatherParser.describe(w.code, w.isDay)
    if (compact) {
        Column(
            modifier
                .glass(Shape, Color.Transparent, elevation = 10)
                .background(sky(w.code, w.isDay))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("📍 ${w.city}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${temp(w.tempC)}°", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.ExtraLight)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(emoji, fontSize = 22.sp)
                Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("H:${temp(w.highC)}°  L:${temp(w.lowC)}°", color = Color.White.copy(0.85f), fontSize = 12.sp, maxLines = 1)
            }
        }
        return
    }
    Column(
        modifier
            .glass(Shape, Color.Transparent, elevation = 10)
            .background(sky(w.code, w.isDay))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("📍 ${w.city}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${temp(w.tempC)}°", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.ExtraLight)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(emoji, fontSize = 38.sp)
                Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("H:${temp(w.highC)}°  L:${temp(w.lowC)}°", color = Color.White.copy(0.85f), fontSize = 14.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.22f)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            w.hours.forEachIndexed { i, h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (i == 0) "Now" else hourLabel(h.hour), color = Color.White.copy(0.8f), fontSize = 12.sp)
                    Text(WeatherParser.describe(h.code, h.isDay).first, fontSize = 20.sp)
                    Text("${temp(h.tempC)}°", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun hourLabel(hour: Int): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "$h12${if (hour < 12) "AM" else "PM"}"
}

@Composable
private fun Message(title: String, body: String?, action: String?, onAction: () -> Unit, modifier: Modifier, compact: Boolean) {
    Column(
        modifier
            .glass(Shape, Color.Transparent, elevation = 10)
            .background(sky(2, true))
            .padding(if (compact) 14.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp, if (compact) Alignment.CenterVertically else Alignment.Top),
    ) {
        Text("⛅  $title", color = Color.White, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.SemiBold)
        if (body != null && !compact) Text(body, color = Color.White.copy(0.85f), fontSize = 14.sp)
        if (action != null) GlassButton(if (compact) action.substringBefore(" ") else action, strong = true, onClick = onAction, modifier = Modifier.padding(top = 2.dp))
    }
}
