package com.example.ioslauncheronandroid.ui.widgets

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import com.example.ioslauncheronandroid.ui.home.PageHeader
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import com.example.ioslauncheronandroid.weather.WeatherState
import com.example.ioslauncheronandroid.ui.home.detectLongPressInitial
import com.example.ioslauncheronandroid.ui.home.MenuAction
import com.example.ioslauncheronandroid.ui.home.WidgetMenu
import com.example.ioslauncheronandroid.ui.home.LocalWidgetMenu
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.data.UsageStore
import com.example.ioslauncheronandroid.data.WidgetCategorizer
import com.example.ioslauncheronandroid.data.WidgetCategory
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.home.AppIcon
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(28.dp)

@Composable
fun WidgetsPage(apps: List<AppInfo>, onAddWidget: () -> Unit, entrance: () -> Float, modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf(false) }
    val controller = LocalController.current
    val context = LocalContext.current
    val ids = LauncherSettings.widgetIds.toList()
    val groups = remember(ids) {
        ids.groupBy { WidgetCategorizer.category(context, controller.providerInfo(it)) }.toSortedMap(compareBy { it.ordinal })
    }
    LazyColumn(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 0.35f + 0.65f * entrance()
                translationY = (1f - entrance()) * 20.dp.toPx()
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
            }

            .drawWithContent {
                drawContent()
                val fade = 34.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black, 1f to Color.Transparent,
                        startY = size.height - fade, endY = size.height,
                    ),
                    topLeft = Offset(0f, size.height - fade),
                    size = Size(size.width, fade),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                )
            },
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") { TodayHeader(editing) { editing = !editing } }
        val squares = listOf(Builtin.Weather, Builtin.Photos).filter { it.isOn }
        val stack = listOf(Builtin.Clock, Builtin.Calendar).filter { it.isOn }
        Builtin.entries.filter { it.isOn }.forEach { widget ->
            if (widget == Builtin.Clock || widget == Builtin.Calendar) {
                if (widget != stack.first()) return@forEach
                item(key = "builtin-stack") {
                    val extra = listOf(MenuAction(if (LauncherSettings.clock24h) "Use 12-hour time" else "Use 24-hour time", "⏱") { LauncherSettings.clock24h = !LauncherSettings.clock24h })
                    Removable(editing, onRemove = { stack.forEach { it.isOn = false } }, extra = extra, onEdit = { editing = true }) {
                        WidgetStack(stack.size) { page ->
                            if (stack[page] == Builtin.Clock) ClockCard(Modifier.fillMaxSize()) else CalendarCard(Modifier.fillMaxSize())
                        }
                    }
                }
                return@forEach
            }
            if (widget == Builtin.Weather || widget == Builtin.Photos) {
                if (widget != squares.first()) return@forEach
                item(key = "builtin-squares") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        squares.forEach { sq ->
                            val tile = Modifier.weight(1f).aspectRatio(1f)
                            val extra = if (sq == Builtin.Weather) listOf(MenuAction("Refresh Weather", "↻") { WeatherState.version.intValue++ }) else emptyList()
                            Removable(editing, onRemove = { sq.isOn = false }, extra = extra, onEdit = { editing = true }, modifier = tile) {
                                if (sq == Builtin.Weather) WeatherCard(Modifier.fillMaxSize(), compact = true)
                                else PhotosCard(Modifier.fillMaxSize(), compact = true)
                            }
                        }
                        if (squares.size == 1) Box(Modifier.weight(1f))
                    }
                }
                return@forEach
            }
            item(key = "builtin-${widget.name}") {
                val extra = when (widget) {
                    Builtin.Clock -> listOf(MenuAction(if (LauncherSettings.clock24h) "Use 12-hour time" else "Use 24-hour time", "⏱") { LauncherSettings.clock24h = !LauncherSettings.clock24h })
                    Builtin.Weather -> listOf(MenuAction("Refresh Weather", "↻") { WeatherState.version.intValue++ })
                    else -> emptyList()
                }
                Removable(editing, onRemove = { widget.isOn = false }, extra = extra, onEdit = { editing = true }) {
                    when (widget) {
                        Builtin.Clock -> ClockCard()
                        Builtin.Calendar -> CalendarCard()
                        Builtin.Weather -> WeatherCard()
                        Builtin.AnalogClock -> AnalogClockCard()
                        Builtin.Photos -> PhotosCard()
                        Builtin.Music -> MusicCard()
                        Builtin.Device -> DeviceCard()
                        Builtin.Suggested -> SuggestedCard(apps)
                        Builtin.Groups -> GroupsCard(apps)
                    }
                }
            }
        }
        groups.forEach { (category, group) ->
            item(key = "group-${category.name}") { GroupHeader(category, group.size) }
            items(group, key = { it }) { id -> HostedWidget(id, editing, onEdit = { editing = true }) }
        }
        item(key = "add") { AddWidgetCard(empty = ids.isEmpty(), onClick = onAddWidget) }
    }
}

@Composable
private fun TodayHeader(editing: Boolean, onToggle: () -> Unit) {
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()).uppercase() }
    PageHeader(caption = date, title = "Today", actionLabel = if (editing) "Done" else "Edit", onAction = onToggle)
}

@Composable
internal fun ClockCard(modifier: Modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    val fmt = if (LauncherSettings.clock24h) "HH:mm" else "h:mm"
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000 + 20)
        }
    }
    val greeting = when (Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val alarm = remember(now) {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
            ?.takeIf { info ->

                val owner = info.showIntent?.creatorPackage
                owner == null || owner.contains("clock", ignoreCase = true) || owner.contains("alarm", ignoreCase = true)
            }
            ?.let { SimpleDateFormat(if (LauncherSettings.clock24h) "EEE HH:mm" else "EEE h:mm a", Locale.getDefault()).format(Date(it.triggerTime)) }
    }
    Row(
        modifier.glass(CardShape, elevation = 10).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(greeting, color = Color.White.copy(0.75f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(SimpleDateFormat(fmt, Locale.getDefault()).format(Date(now)), color = Color.White, fontSize = LauncherSettings.clockSize.sp, fontWeight = FontWeight.Light)
        }
        if (alarm != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text("⏰", fontSize = 22.sp)
                Text(alarm, color = Color.White.copy(0.75f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun DeviceCard() {
    val context = LocalContext.current
    var battery by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val update = { i: Intent? ->
            if (i != null) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                battery = if (l >= 0) l * 100 / s else -1
                val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = update(i)
        }
        update(context.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        onDispose { context.unregisterReceiver(r) }
    }

    val usage by produceState(0f to 0f) {
        while (true) {
            val storage = runCatching {
                val st = StatFs(Environment.getDataDirectory().path)
                1f - st.availableBytes.toFloat() / st.totalBytes
            }.getOrDefault(0f)
            val memory = runCatching {
                val mi = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
                1f - mi.availMem.toFloat() / mi.totalMem
            }.getOrDefault(0f)
            value = storage to memory
            delay(30_000)
        }
    }
    Row(
        Modifier.fillMaxWidth().glass(CardShape, elevation = 10).padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Ring(
            fraction = battery.coerceAtLeast(0) / 100f,
            center = if (battery >= 0) "$battery%" else "—",
            label = if (charging) "Charging" else "Battery",
            color = if (battery in 0..20 && !charging) Color(0xFFFF6B6B) else Color(0xFF7CFFB2),
        )
        Ring(usage.first, "${(usage.first * 100).toInt()}%", "Storage", Color(0xFF64B5FF))
        Ring(usage.second, "${(usage.second * 100).toInt()}%", "Memory", Color(0xFFC792FF))
    }
}

@Composable
private fun Ring(fraction: Float, center: String, label: String, color: Color) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "ring")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 7.dp.toPx()
                val topLeft = Offset(w / 2, w / 2)
                val arc = Size(size.width - w, size.height - w)
                drawArc(Color.White.copy(0.16f), 0f, 360f, false, topLeft, arc, style = Stroke(w))
                drawArc(color, -90f, 360f * shown, false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Round))
            }
            Text(center, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(label, color = Color.White.copy(0.7f), fontSize = 12.sp)
    }
}

@Composable
internal fun SuggestedCard(apps: List<AppInfo>) {
    val topKeys = UsageStore.top(4)
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val used = topKeys.mapNotNull { byKey[it] }
    val picked = used.ifEmpty { apps.sortedByDescending { it.installedAt }.take(4) }
    if (picked.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().glass(CardShape, elevation = 10).padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (used.isEmpty()) "RECENTLY ADDED" else "SUGGESTED FOR YOU",
            color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            picked.forEach { AppIcon(it, iconSize = 56.dp) }
        }
    }
}

@Composable
private fun GroupHeader(category: WidgetCategory, count: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${category.emoji}  ${category.title.uppercase()}",
            color = Color.White.copy(0.65f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
            modifier = Modifier.weight(1f),
        )
        Text("$count", color = Color.White.copy(0.45f), fontSize = 12.sp)
    }
}

@Composable
private fun AddWidgetCard(empty: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(CardShape, elevation = 10)
            .pointerInput(onClick) { detectTapGestures { onClick() } }
            .padding(horizontal = 20.dp, vertical = if (empty) 26.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(42.dp).background(Color.White.copy(0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Text("+", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Add widget", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (empty) "Pick from the widgets on your phone — they're sorted by type for you."
                else "Sorted into groups automatically.",
                color = Color.White.copy(0.65f), fontSize = 13.sp, lineHeight = 17.sp,
            )
        }
    }
}

@Composable
internal fun HostedWidget(id: Int, editing: Boolean, onEdit: () -> Unit = {}) {
    val controller = LocalController.current
    val info = remember(id) { controller.providerInfo(id) } ?: return
    val density = LocalDensity.current
    val heightDp = with(density) { info.minHeight.toDp() }.coerceIn(110.dp, 420.dp)
    val openMenu = LocalWidgetMenu.current
    val bounds = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(heightDp)
            .clip(RoundedCornerShape(28.dp))
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
            .pointerInput(editing) {
                if (editing) return@pointerInput
                detectLongPressInitial {
                    openMenu(WidgetMenu(bounds[0], listOf(MenuAction("Edit Widgets", "✎", onClick = onEdit), MenuAction("Remove Widget", "−", destructive = true) { controller.removeWidget(id) })))
                }
            },
    ) {
        val wDp = maxWidth.value.toInt()
        val hDp = heightDp.value.toInt()
        AndroidView(
            factory = { ctx -> controller.createWidgetView(id) ?: View(ctx) },
            update = {
                @Suppress("DEPRECATION")
                (it as? android.appwidget.AppWidgetHostView)?.updateAppWidgetSize(null, wDp, hDp, wDp, hDp)
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (editing) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(0.25f))
                    .pointerInput(id) { detectTapGestures { } },
            )
            Text(
                "Remove",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .glass(RoundedCornerShape(50), Color(0xFFFF453A).copy(0.55f), 0)
                    .pointerInput(id) { detectTapGestures { controller.removeWidget(id) } }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Removable(
    editing: Boolean,
    onRemove: () -> Unit,
    extra: List<MenuAction> = emptyList(),
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val openMenu = LocalWidgetMenu.current
    val bounds = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
    Box(
        modifier
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
            .pointerInput(editing, extra) {
                if (editing) return@pointerInput
                detectLongPressInitial {
                    openMenu(WidgetMenu(bounds[0], extra + MenuAction("Edit Widgets", "✎", onClick = onEdit) + MenuAction("Remove Widget", "−", destructive = true, onClick = onRemove)))
                }
            },
    ) {
        content()
        if (editing) {
            Box(Modifier.matchParentSize().clip(CardShape).background(Color.Black.copy(0.25f)).pointerInput(Unit) { detectTapGestures { } })
            Text(
                "Remove",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .glass(RoundedCornerShape(50), Color(0xFFFF453A).copy(0.55f), 0)
                    .pointerInput(onRemove) { detectTapGestures { onRemove() } }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun WidgetStack(count: Int, content: @Composable (Int) -> Unit) {
    if (count == 1) {
        Box(Modifier.fillMaxWidth().height(StackHeight)) { content(0) }
        return
    }
    val pager = androidx.compose.foundation.pager.rememberPagerState { count }
    Box(Modifier.fillMaxWidth().height(StackHeight)) {
        androidx.compose.foundation.pager.VerticalPager(pager, Modifier.fillMaxSize()) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val d = kotlin.math.abs((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                        alpha = 1f - 0.6f * d
                        val sc = 1f - 0.06f * d
                        scaleX = sc; scaleY = sc
                    },
            ) { content(page) }
        }
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(count) { i ->
                Box(
                    Modifier
                        .size(5.dp)
                        .graphicsLayer { alpha = if (pager.currentPage == i) 1f else 0.4f }
                        .background(Color.White, CircleShape),
                )
            }
        }
    }
}

private val StackHeight = 156.dp
