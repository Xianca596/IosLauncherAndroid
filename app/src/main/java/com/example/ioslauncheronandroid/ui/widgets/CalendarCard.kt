package com.example.ioslauncheronandroid.ui.widgets

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import com.example.ioslauncheronandroid.ui.glass.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(28.dp)

object CalendarAccess {
    val version = mutableIntStateOf(0)
}

private class DayEvent(val title: String, val begin: Long, val end: Long, val allDay: Boolean, val color: Int)

@Composable
internal fun CalendarCard(modifier: Modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    val controller = LocalController.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000 + 20)
        }
    }
    val allowed = context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    val events by produceState<List<DayEvent>>(emptyList(), CalendarAccess.version.intValue, now / 60_000L, allowed) {
        value = if (allowed) loadToday(context, now) else emptyList()
    }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val timeFmt = SimpleDateFormat(if (LauncherSettings.clock24h) "HH:mm" else "h:mm a", Locale.getDefault())
    Row(
        modifier.glass(CardShape, elevation = 10).padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(SimpleDateFormat("EEE", Locale.getDefault()).format(Date(now)).uppercase(), color = Color(0xFFFF6B6B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${cal.get(Calendar.DAY_OF_MONTH)}", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Light)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(0.2f)))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            when {
                !allowed -> {
                    Text("See today's events", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    GlassButton("Allow calendar", strong = true) { controller.requestCalendarAccess() }
                }
                events.isEmpty() -> {
                    Text("No more events today", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)), color = Color.White.copy(0.65f), fontSize = 13.sp)
                }
                else -> events.take(3).forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(4.dp).height(30.dp).background(Color(e.color).copy(alpha = 1f), RoundedCornerShape(2.dp)))
                        Column {
                            Text(e.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                if (e.allDay) "All day" else "${timeFmt.format(Date(e.begin))} – ${timeFmt.format(Date(e.end))}",
                                color = Color.White.copy(0.65f), fontSize = 12.sp, maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadToday(context: Context, now: Long): List<DayEvent> = withContext(Dispatchers.IO) {
    val start = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val end = start + 24 * 60 * 60 * 1000L
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        android.content.ContentUris.appendId(it, start)
        android.content.ContentUris.appendId(it, end)
    }.build()
    val out = mutableListOf<DayEvent>()
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.DISPLAY_COLOR),
            null, null, "${CalendarContract.Instances.ALL_DAY} DESC, ${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val allDay = c.getInt(3) == 1
                val e = c.getLong(2)
                if (!allDay && e < now) continue
                out += DayEvent(c.getString(0)?.takeIf { it.isNotBlank() } ?: "(No title)", c.getLong(1), e, allDay, c.getInt(4))
            }
        }
    }
    out
}
