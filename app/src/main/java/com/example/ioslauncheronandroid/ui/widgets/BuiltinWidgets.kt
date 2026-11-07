package com.example.ioslauncheronandroid.ui.widgets

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.data.PhotoStore
import com.example.ioslauncheronandroid.data.WidgetCategory
import com.example.ioslauncheronandroid.music.MusicState
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import com.example.ioslauncheronandroid.ui.glass.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class Builtin(val emoji: String, val title: String, val description: String, val category: WidgetCategory) {
    Clock("🕒", "Clock", "Time, a greeting and your next alarm", WidgetCategory.Utilities),
    Calendar("📅", "Calendar", "Today's events — swipe up/down on the clock to flip to it", WidgetCategory.Productivity),
    Weather("⛅", "Weather", "Conditions where you are, plus the next hours", WidgetCategory.Utilities),
    AnalogClock("🕰", "Analog clock", "A classic clock face", WidgetCategory.Utilities),
    Photos("🖼", "Photos", "Your recent photos, one after another", WidgetCategory.Media),
    Music("🎵", "Music player", "What's playing, with play, pause and skip", WidgetCategory.Media),
    Device("🔋", "Device status", "Battery, storage and memory at a glance", WidgetCategory.Utilities),
    Suggested("✨", "Suggested apps", "The apps you open most", WidgetCategory.Productivity),
    Groups("🗂", "Smart groups", "Your apps sorted into categories automatically", WidgetCategory.Productivity);

    var isOn: Boolean
        get() = when (this) {
            Clock -> LauncherSettings.showClock
            Weather -> LauncherSettings.showWeather
            Calendar -> LauncherSettings.showCalendar
            AnalogClock -> LauncherSettings.showAnalog
            Photos -> LauncherSettings.showPhotos
            Music -> LauncherSettings.showMusic
            Device -> LauncherSettings.showBattery
            Suggested -> LauncherSettings.showSuggested
            Groups -> LauncherSettings.showGroups
        }
        set(v) = when (this) {
            Clock -> LauncherSettings.showClock = v
            Weather -> LauncherSettings.showWeather = v
            Calendar -> LauncherSettings.showCalendar = v
            AnalogClock -> LauncherSettings.showAnalog = v
            Photos -> LauncherSettings.showPhotos = v
            Music -> LauncherSettings.showMusic = v
            Device -> LauncherSettings.showBattery = v
            Suggested -> LauncherSettings.showSuggested = v
            Groups -> LauncherSettings.showGroups = v
        }
}

private val CardShape = RoundedCornerShape(28.dp)

@Composable
internal fun AnalogClockCard() {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000 + 20)
        }
    }
    val cal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val minute = cal.get(Calendar.MINUTE)
    val hour = cal.get(Calendar.HOUR)
    Row(
        Modifier.fillMaxWidth().glass(CardShape, elevation = 10).padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Canvas(Modifier.size(132.dp)) {
            val c = center
            val r = size.minDimension / 2f
            drawCircle(Color.White.copy(0.10f), r)
            drawCircle(Color.White.copy(0.45f), r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            repeat(60) { i ->
                val big = i % 5 == 0
                val a = Math.toRadians(i * 6.0 - 90.0)
                val len = if (big) 9.dp.toPx() else 4.dp.toPx()
                drawLine(
                    Color.White.copy(if (big) 0.9f else 0.35f),
                    Offset(c.x + (r - 6.dp.toPx() - len) * cos(a).toFloat(), c.y + (r - 6.dp.toPx() - len) * sin(a).toFloat()),
                    Offset(c.x + (r - 6.dp.toPx()) * cos(a).toFloat(), c.y + (r - 6.dp.toPx()) * sin(a).toFloat()),
                    strokeWidth = (if (big) 2.dp else 1.dp).toPx(), cap = StrokeCap.Round,
                )
            }
            fun hand(angleDeg: Double, length: Float, width: Float, color: Color) {
                val a = Math.toRadians(angleDeg - 90.0)
                drawLine(color, c, Offset(c.x + length * cos(a).toFloat(), c.y + length * sin(a).toFloat()), width, StrokeCap.Round)
            }
            hand((hour + minute / 60.0) * 30.0, r * 0.5f, 5.dp.toPx(), Color.White)
            hand(minute * 6.0, r * 0.75f, 3.5.dp.toPx(), Color.White)
            drawCircle(Color(0xFFFF9F0A), 4.dp.toPx(), c)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(now)), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(now)), color = Color.White.copy(0.7f), fontSize = 16.sp)
        }
    }
}

private class PhotoItem(val uri: android.net.Uri, val image: ImageBitmap, val takenAt: Long)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun PhotosCard(modifier: Modifier = Modifier.fillMaxWidth().height(250.dp), compact: Boolean = false) {
    val context = LocalContext.current
    val controller = LocalController.current
    val photos by produceState<List<PhotoItem>>(emptyList(), PhotoStore.version.intValue) { value = loadRecentPhotos(context) }
    Box(
        modifier
            .glass(CardShape, Color.Transparent, elevation = 10)
            .clip(CardShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photos.isEmpty()) {
            Column(
                Modifier.fillMaxSize().background(Color.White.copy(0.08f)).padding(if (compact) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp, Alignment.CenterVertically),
            ) {
                Text("🖼", fontSize = if (compact) 28.sp else 34.sp)
                Text(if (compact) "Photos" else "Show your photos here", color = Color.White, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.SemiBold)
                if (!compact) Text("Choose photos to show them here — swipe through your selected pictures.", color = Color.White.copy(0.65f), fontSize = 13.sp)
                GlassButton(if (compact) "Choose" else "Choose photos", strong = true) { controller.requestPhotoAccess() }
            }
        } else {
            val pager = androidx.compose.foundation.pager.rememberPagerState { photos.size }
            LaunchedEffect(pager.currentPage, photos.size) {
                if (photos.size < 2) return@LaunchedEffect
                delay(8_000)
                if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % photos.size)
            }
            androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                val photo = photos[page]
                Box(
                    Modifier
                        .fillMaxSize()

                        .clipToBounds()
                        .pointerInput(photo) {
                            detectTapGestures {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW).setDataAndType(photo.uri, "image/*")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                    )
                                }
                            }
                        },
                ) {
                    Image(
                        photo.image, null,
                        Modifier.fillMaxSize().graphicsLayer {
                            val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                            translationX = -offset * size.width
                            val d = kotlin.math.abs(offset)
                            alpha = (1f - d * 2f).coerceIn(0f, 1f)
                            val sc = 1f + 0.06f * d
                            scaleX = sc; scaleY = sc
                        },
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (compact) 64.dp else 96.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f)))))
            val taken = Date(photos[pager.currentPage.coerceIn(0, photos.lastIndex)].takenAt)
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 10.dp else 14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    if (!compact) Text("PHOTOS", color = Color.White.copy(0.75f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Text(
                        SimpleDateFormat(if (compact) "d MMM" else "EEEE, d MMMM", Locale.getDefault()).format(taken),
                        color = Color.White, fontSize = if (compact) 14.sp else 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    )
                }
                val dotCount = minOf(photos.size, if (compact) 4 else 7)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(dotCount) { i ->
                        val active = i == pager.currentPage % dotCount
                        Box(Modifier.size(if (active) 6.dp else 4.dp).background(Color.White.copy(if (active) 1f else 0.5f), androidx.compose.foundation.shape.CircleShape))
                    }
                }
            }
        }
    }
}

private suspend fun loadRecentPhotos(context: Context): List<PhotoItem> =
    withContext(Dispatchers.IO) {
        PhotoStore.getUris().mapNotNull { uri ->
            runCatching {
                PhotoItem(
                    uri,
                    decodeSampled(context, uri, 1080, 720).asImageBitmap(),
                    System.currentTimeMillis()
                )
            }.getOrNull()
        }
    }
private fun decodeSampled(context: Context, uri: android.net.Uri, maxW: Int, maxH: Int): android.graphics.Bitmap {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxW && bounds.outHeight / (sample * 2) >= maxH) sample *= 2
    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
        ?: error("cannot decode $uri")
}

@Composable
internal fun MusicCard() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        MusicState.start(context)
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) MusicState.start(context) }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); MusicState.stop() }
    }
    Column(
        Modifier.fillMaxWidth().glass(CardShape, elevation = 10).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!MusicState.hasAccess) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🎵", fontSize = 34.sp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Music player", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Allow notification access to see and control what's playing.", color = Color.White.copy(0.65f), fontSize = 13.sp)
                }
            }
            GlassButton("Allow access", strong = true) {
                runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.14f)), contentAlignment = Alignment.Center) {
                    val art = MusicState.art
                    if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Text("🎵", fontSize = 26.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(MusicState.title ?: "Nothing playing", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(MusicState.artist ?: "Start music in any app", color = Color.White.copy(0.65f), fontSize = 14.sp, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Transport("⏮") { MusicState.previous() }
                Transport(if (MusicState.playing) "⏸" else "▶", big = true) { MusicState.playPause() }
                Transport("⏭") { MusicState.next() }
            }
        }
    }
}

@Composable
private fun Transport(glyph: String, big: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(if (big) 56.dp else 46.dp)
            .background(Color.White.copy(if (big) 0.26f else 0.14f), androidx.compose.foundation.shape.CircleShape)
            .pointerInput(onClick) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = Color.White, fontSize = if (big) 24.sp else 19.sp) }
}

@Composable
internal fun GroupsCard(apps: List<com.example.ioslauncheronandroid.data.AppInfo>) {
    val openGroup = com.example.ioslauncheronandroid.ui.home.LocalOpenGroup.current
    val groups = remember(apps) {
        apps.groupBy { it.group }.filterKeys { it != WidgetCategory.Other }.entries.sortedByDescending { it.value.size }.take(4)
    }
    if (groups.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().glass(CardShape, elevation = 10).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("APP GROUPS", color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 6.dp))
        groups.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (cat, list) -> GroupTile(cat, list, { r -> openGroup(cat.title, list, r) }, Modifier.weight(1f)) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GroupTile(cat: WidgetCategory, apps: List<com.example.ioslauncheronandroid.data.AppInfo>, onOpen: (androidx.compose.ui.geometry.Rect) -> Unit, modifier: Modifier) {
    val bounds = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
    Column(
        modifier
            .androidxBounds(bounds)
            .glass(RoundedCornerShape(20.dp), Color.White.copy(0.10f), 0)
            .pointerInput(apps) { detectTapGestures { onOpen(bounds[0]) } }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            apps.take(4).chunked(2).forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    r.forEach { Image(it.icon, null, Modifier.size(34.dp)) }
                }
            }
        }
        Column {
            Text("${cat.emoji} ${cat.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${apps.size} apps", color = Color.White.copy(0.6f), fontSize = 12.sp)
        }
    }
}

private fun Modifier.androidxBounds(holder: Array<androidx.compose.ui.geometry.Rect>): Modifier =
    this.onGloballyPositioned { holder[0] = it.boundsInWindow() }
