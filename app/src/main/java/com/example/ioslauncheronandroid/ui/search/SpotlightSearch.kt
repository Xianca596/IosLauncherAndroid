package com.example.ioslauncheronandroid.ui.search

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.ioslauncheronandroid.data.LauncherAction
import com.example.ioslauncheronandroid.data.closestMatch
import com.example.ioslauncheronandroid.data.SettingsIndex
import com.example.ioslauncheronandroid.data.SettingsTarget
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import com.example.ioslauncheronandroid.ui.home.AppIcon
import kotlin.math.pow

@Composable
fun SpotlightSearch(
    progress: Float,
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onFieldBounds: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onAction: (LauncherAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val open = progress > 0.96f
    LaunchedEffect(open) {
        if (open) focus.requestFocus() else if (progress == 0f) query = ""
    }

    val results = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    val settingsResults = remember(query) { SettingsIndex.search(query) }

    val browsing = query.isBlank()
    val rows = remember(apps) { buildRows(apps) }
    val letterIndex = remember(rows) {
        rows.withIndex().filter { it.value is ListRow.Header }.associate { (it.value as ListRow.Header).letter to it.index }
    }
    val listState = rememberLazyListState()
    val railScope = rememberCoroutineScope()

    val eased = progress.coerceAtLeast(0f).pow(0.85f)
    val entering = progress > 0.3f
    Box(
        modifier
            .fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onFieldBounds(it.boundsInWindow()) }

                    .graphicsLayer { alpha = ((progress - 0.94f) / 0.06f).coerceIn(0f, 1f) }
                    .liquidGlass(20.dp, 8, lift = 0.14f, tick = { progress })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchGlyph(20.dp)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search apps & settings", color = Color.White.copy(0.65f), fontSize = 17.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
                Text(
                    "Cancel",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickableNoRipple(onClose),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        alpha = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        translationY = 24.dp.toPx() * (1f - progress)
                    }
                    .liquidGlass(28.dp, 8, tick = { progress }),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 8.dp, top = 8.dp, bottom = 8.dp, end = if (browsing) 34.dp else 8.dp,
                    ),
                ) {
                    if (browsing) {
                        itemsIndexed(rows, key = { _, r -> if (r is ListRow.Header) "h:${r.letter}" else (r as ListRow.App).app.key }) { index, r ->
                            when (r) {
                                is ListRow.Header -> Text(
                                    r.letter.toString(), color = Color.White.copy(0.65f), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 2.dp),
                                )
                                is ListRow.App -> AppRow(r.app, entering, index, onClose)
                            }
                        }
                    } else {
                        itemsIndexed(results, key = { _, app -> app.key }) { index, app ->
                            AppRow(app, entering, index, onClose, stagger = false)
                        }
                    }
                    if (settingsResults.isNotEmpty()) {
                        item(key = "settings-header") {
                            Text("Settings", color = Color.White.copy(0.6f), fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 4.dp))
                        }
                        items(settingsResults, key = { "s:" + it.title }) { e ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .pointerInput(e) {
                                        detectTapGestures {
                                            when (val t = e.target) {
                                                is SettingsTarget.System -> runCatching {
                                                    context.startActivity(Intent(t.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                                }
                                                is SettingsTarget.Launcher -> onAction(t.action)
                                            }
                                            onClose()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(e.title, color = Color.White, fontSize = 17.sp)
                                Text(e.subtitle, color = Color.White.copy(0.55f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                val noResults = !browsing && results.isEmpty() && settingsResults.isEmpty()
                if (noResults) {
                    val suggestion = remember(query, apps) {
                        closestMatch(query, apps.map { it.label } + SettingsIndex.all.map { it.title })
                    }
                    NoResults(query, suggestion) { query = it }
                }
                if (browsing) {
                    AlphabetRail(
                        present = letterIndex.keys,
                        onLetter = { ch ->

                            val target = RailLetters.dropWhile { it != ch }.firstOrNull { it in letterIndex }
                                ?: letterIndex.keys.maxOrNull()
                            target?.let { railScope.launch { listState.scrollToItem(letterIndex.getValue(it)) } }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .fillMaxHeight(0.8f)
                            .heightIn(max = 380.dp),
                    )
                }
            }
        }
    }
}

private sealed interface ListRow {
    data class Header(val letter: Char) : ListRow
    data class App(val app: AppInfo) : ListRow
}

private fun buildRows(apps: List<AppInfo>): List<ListRow> {
    val grouped = apps.groupBy { a -> a.label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#' }
    return RailLetters.flatMap { ch ->
        grouped[ch]?.let { list -> listOf<ListRow>(ListRow.Header(ch)) + list.map { ListRow.App(it) } } ?: emptyList()
    }
}

@Composable
private fun AppRow(app: AppInfo, entering: Boolean, index: Int, onClose: () -> Unit, stagger: Boolean = true) {
    val animated = stagger && index < 14
    val enter = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(entering) {
        if (!animated) return@LaunchedEffect
        if (entering) {
            delay(index * 30L)
            enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
        } else {
            enter.snapTo(0f)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                translationY = (1f - e) * 44.dp.toPx()
                val sc = 0.94f + 0.06f * e
                scaleX = sc
                scaleY = sc
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppIcon(app, iconSize = 44.dp, showLabel = false, onLaunch = onClose)
        Text(app.label, color = Color.White, fontSize = 17.sp)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onClick() } }

@Composable
private fun BoxScope.NoResults(query: String, suggestion: String?, onPick: (String) -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f)) }
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(32.dp)
            .graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                translationY = (1f - enter.value) * 16.dp.toPx()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🔍", fontSize = 36.sp)
        Text(
            "No results for “${query.trim()}”",
            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )
        Text("Check the spelling or try a new search.", color = Color.White.copy(0.65f), fontSize = 14.sp, textAlign = TextAlign.Center)
        if (suggestion != null) {
            Text(
                "Did you mean “$suggestion”?",
                color = Color(0xFF7CC0FF), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp).pointerInput(suggestion) { detectTapGestures { onPick(suggestion) } },
            )
        }
    }
}
