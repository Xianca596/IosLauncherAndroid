package com.example.ioslauncheronandroid.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import com.example.ioslauncheronandroid.ui.home.PageHeader
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.GlassButton
import com.example.ioslauncheronandroid.ui.widgets.HostedWidget
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.UsageStore
import com.example.ioslauncheronandroid.ui.home.LocalOpenGroup
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.ui.glass.GlassTokens
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.home.AppIcon

@Composable
fun AppLibrary(
    apps: List<AppInfo>,
    entrance: () -> Float = { 1f },
    onAddWidget: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TextFieldValue(LauncherSettings.libraryName)) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(editing) { if (editing) focus.requestFocus() }
    val openGroup = LocalOpenGroup.current
    val groups = remember(apps) { apps.groupBy { it.group }.toSortedMap(compareBy { it.ordinal }).toList() }
    val topKeys = UsageStore.top(4)
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val suggested = topKeys.mapNotNull { byKey[it] }
    val recent = remember(apps) { apps.sortedByDescending { it.installedAt }.take(4) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp)
            .graphicsLayer {
                alpha = 0.35f + 0.65f * entrance()
                translationY = (1f - entrance()) * 20.dp.toPx()
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
            }

            .drawWithContent {
                drawContent()
                val fade = 34.dp.toPx()
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black, 1f to Color.Transparent,
                        startY = size.height - fade, endY = size.height,
                    ),
                    topLeft = Offset(0f, size.height - fade),
                    size = androidx.compose.ui.geometry.Size(size.width, fade),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                )
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(
            caption = "${apps.size} APPS",
            title = LauncherSettings.libraryName,
            actionLabel = if (editing) "Done" else "Edit",
            onAction = {
                if (editing) LauncherSettings.libraryName = draft.text
                else LauncherSettings.libraryName.let { draft = TextFieldValue(it, TextRange(0, it.length)) }
                editing = !editing
            },
            modifier = Modifier.graphicsLayer {
                alpha = entrance()
                translationX = (1f - entrance()) * -16.dp.toPx()
            },
            titleContent = if (editing) {
                {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = if (it.text.length > 24) draft else it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).glass(RoundedCornerShape(14.dp), elevation = 0).padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            } else null,
        )
        LauncherSettings.libraryWidgetIds.forEach { id -> key(id) { HostedWidget(id, editing) } }
        if (editing) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassButton("+  Add widget", strong = true, onClick = onAddWidget)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (suggested.isNotEmpty()) LibraryCard("Suggestions", suggested, null, Modifier.weight(1f))
            LibraryCard("Recently Added", recent, null, Modifier.weight(1f))
            if (suggested.isEmpty()) Box(Modifier.weight(1f))
        }
        groups.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (cat, list) ->
                    LibraryCard("${cat.emoji} ${cat.title}", list, { rect -> openGroup(cat.title, list, rect) }, Modifier.weight(1f))
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibraryCard(title: String, apps: List<AppInfo>, onOpen: ((Rect) -> Unit)?, modifier: Modifier = Modifier) {
    val bounds = remember { arrayOf(Rect.Zero) }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
                .glass(RoundedCornerShape(28.dp), GlassTokens.TintStrong)
                .pointerInput(apps) { if (onOpen != null) detectTapGestures { onOpen(bounds[0]) } }
                .padding(12.dp),
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                val cells = if (apps.size <= 4) apps.map { it as AppInfo? } else apps.take(3) + listOf(null)
                cells.chunked(2).forEach { r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        r.forEach { app ->
                            if (app != null) AppIcon(app, iconSize = 44.dp, showLabel = false)
                            else MiniCluster(apps.drop(3).take(4))
                        }
                        if (r.size == 1) Box(Modifier.size(44.dp))
                    }
                }
            }
        }
        Text(
            if (onOpen != null) "$title  ·  ${apps.size}" else title,
            color = Color.White, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, start = 6.dp),
        )
    }
}

@Composable
private fun MiniCluster(apps: List<AppInfo>) {
    Column(Modifier.size(44.dp), verticalArrangement = Arrangement.SpaceBetween) {
        apps.chunked(2).forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                r.forEach { Image(it.icon, null, Modifier.size(20.dp)) }
            }
        }
    }
}
