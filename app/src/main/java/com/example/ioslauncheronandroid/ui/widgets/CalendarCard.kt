package com.example.ioslauncheronandroid.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.glass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(28.dp)

@Composable
internal fun CalendarCard(modifier: Modifier = Modifier.fillMaxWidth()) {
    val nowState = produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000 + 20)
        }
    }
    val now = nowState.value
    val timeFmt = SimpleDateFormat(
        if (LauncherSettings.clock24h) "HH:mm" else "h:mm a",
        Locale.getDefault()
    )

    Row(
        modifier.glass(CardShape, elevation = 10).padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat("EEE", Locale.getDefault()).format(Date(now)).uppercase(),
                color = Color(0xFFFF6B6B),
                fontSize = 14.sp
            )
            Text(
                SimpleDateFormat("d", Locale.getDefault()).format(Date(now)),
                color = Color.White,
                fontSize = 52.sp
            )
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(0.2f)))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Text("Calendar", color = Color.White, fontSize = 16.sp)
            Text(
                SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)),
                color = Color.White.copy(0.7f),
                fontSize = 13.sp
            )
            Text(timeFmt.format(Date(now)), color = Color.White.copy(0.7f), fontSize = 13.sp)
        }
    }
}
