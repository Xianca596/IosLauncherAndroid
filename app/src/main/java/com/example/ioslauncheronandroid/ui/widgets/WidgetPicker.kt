package com.example.ioslauncheronandroid.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.ioslauncheronandroid.data.UsageStore
import com.example.ioslauncheronandroid.data.WidgetCategorizer
import com.example.ioslauncheronandroid.data.WidgetCategory
import com.example.ioslauncheronandroid.ui.glass.GlassSheet
import com.example.ioslauncheronandroid.ui.glass.GlassTokens
import com.example.ioslauncheronandroid.ui.glass.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class WidgetRow(
    val info: AppWidgetProviderInfo,
    val label: String,
    val app: String,
    val pkg: String,
    val preview: ImageBitmap?,
    val category: WidgetCategory,
    val size: String?,
)

@Composable
fun WidgetPicker(toLibrary: Boolean = false, onClose: () -> Unit) {
    val controller = LocalController.current
    val context = LocalContext.current
    val rows by produceState<List<WidgetRow>>(emptyList()) {
        value = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            controller.providers().map { p ->
                val preview = runCatching { p.loadPreviewImage(context, 0) ?: p.loadIcon(context, 0) }.getOrNull()
                    ?.let { runCatching { it.toBitmap(320, 200).asImageBitmap() }.getOrNull() }
                val pkg = p.provider.packageName
                val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                val size = if (Build.VERSION.SDK_INT >= 31 && p.targetCellWidth > 0 && p.targetCellHeight > 0) "${p.targetCellWidth}×${p.targetCellHeight}" else null
                WidgetRow(p, p.loadLabel(pm), app, pkg, preview, WidgetCategorizer.category(context, p), size)
            }.sortedWith(compareBy({ it.app.lowercase() }, { it.label.lowercase() }))
        }
    }
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf<WidgetCategory?>(null) }

    var showAll by remember { mutableStateOf(false) }

    val curated = listOf("clock", "weather", "calendar", "photo", "gallery", "music", "player", "note", "battery", "podcast", "radio", "fit", "health", "task", "todo")
    val visibleRows = remember(rows, showAll) {
        if (showAll) rows else rows
            .filter { r -> "${r.label} ${r.app} ${r.pkg}".lowercase().let { t -> curated.any { it in t } } }
            .groupBy { it.pkg }.values.flatMap { it.take(2) }
    }
    val hiddenCount = rows.size - visibleRows.size
    val filtered = remember(visibleRows, query) {
        if (query.isBlank()) visibleRows else visibleRows.filter { "${it.label} ${it.app} ${it.category.title}".contains(query.trim(), ignoreCase = true) }
    }

    val builtins = remember(query, toLibrary) {
        if (toLibrary) emptyList() else Builtin.entries.filter { query.isBlank() || "${it.title} ${it.description} ${it.category.title}".contains(query.trim(), ignoreCase = true) }
    }
    val categories = remember(filtered, builtins) {
        (filtered.map { it.category } + builtins.map { it.category }).distinct().sortedBy { it.ordinal }
    }
    val countIn = { c: WidgetCategory -> filtered.count { it.category == c } + builtins.count { it.category == c } }
    val usedPackages = UsageStore.top(8).map { it.substringBefore('/') }
    val suggested = if (query.isBlank() && chip == null) filtered.filter { it.pkg in usedPackages }.take(6) else emptyList()
    val shownCategories = if (chip == null) categories else categories.filter { it == chip }

    GlassSheet("Add widget", onClose) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glass(RoundedCornerShape(16.dp), GlassTokens.Tint, 0)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search widgets", color = Color.White.copy(0.55f), fontSize = 16.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "all") { Chip("All  ${filtered.size + builtins.size}", chip == null) { chip = null } }
                items(categories, key = { it.name }) { c ->
                    Chip("${c.emoji} ${c.title}  ${countIn(c)}", chip == c) { chip = if (chip == c) null else c }
                }
            }
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (suggested.isNotEmpty()) {
                    item(key = "h-suggested") { SectionTitle("✨  SUGGESTED FOR YOU") }
                    items(suggested, key = { "s:" + it.info.provider.flattenToString() }) { r -> WidgetCard(r) { controller.addWidget(r.info, toLibrary); onClose() } }
                }

                if (chip == null && builtins.isNotEmpty()) {
                    item(key = "h-builtin") { SectionTitle("✨  FROM THIS LAUNCHER  ·  ${builtins.size}") }
                    items(builtins, key = { "b:" + it.name }) { b -> BuiltinCard(b) { b.isOn = true; onClose() } }
                }
                shownCategories.forEach { category ->
                    val mine = filtered.filter { it.category == category }
                    val mineBuiltins = if (chip == null) emptyList() else builtins.filter { it.category == category }
                    if (mine.isEmpty() && mineBuiltins.isEmpty()) return@forEach
                    item(key = "h-${category.name}") { SectionTitle("${category.emoji}  ${category.title.uppercase()}  ·  ${mine.size + mineBuiltins.size}") }
                    items(mineBuiltins, key = { "b:" + it.name }) { b -> BuiltinCard(b) { b.isOn = true; onClose() } }
                    items(mine, key = { "c:" + it.info.provider.flattenToString() }) { r -> WidgetCard(r) { controller.addWidget(r.info, toLibrary); onClose() } }
                }
                if (rows.isNotEmpty() && shownCategories.isEmpty()) {
                    item(key = "empty") {
                        Text("No widgets match “${query.trim()}”.", color = Color.White.copy(0.65f), fontSize = 15.sp, modifier = Modifier.padding(24.dp))
                    }
                }
                if (hiddenCount > 0 || showAll) {
                    item(key = "show-all") {
                        Text(
                            if (showAll) "Show fewer widgets" else "Show all widgets from apps ($hiddenCount more)",
                            color = Color(0xFF7CC0FF), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp).pointerInput(showAll) { detectTapGestures { showAll = !showAll } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp))
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .glass(RoundedCornerShape(50), if (selected) Color.White.copy(0.34f) else Color.White.copy(0.10f), 0)
            .pointerInput(onClick) { detectTapGestures { onClick() } }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun WidgetCard(r: WidgetRow, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), elevation = 0)
            .pointerInput(r) { detectTapGestures { onClick() } }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (r.preview != null) {
            Image(r.preview, null, Modifier.size(width = 110.dp, height = 70.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Fit)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(r.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(listOfNotNull(r.app, r.size).joinToString("  ·  "), color = Color.White.copy(0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun BuiltinCard(b: Builtin, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), elevation = 0)
            .pointerInput(b) { detectTapGestures { onClick() } }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(width = 110.dp, height = 70.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.14f)),
            contentAlignment = Alignment.Center,
        ) { Text(b.emoji, fontSize = 34.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(b.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(b.description, color = Color.White.copy(0.6f), fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (b.isOn) Text("Added", color = Color(0xFF34C759), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
