package com.example.ioslauncheronandroid.ui.home
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.example.ioslauncheronandroid.ui.search.SearchMorph
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.example.ioslauncheronandroid.data.AppItem
import com.example.ioslauncheronandroid.data.HomeLayout
import com.example.ioslauncheronandroid.data.UsageStore
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.ioslauncheronandroid.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Row
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.ioslauncheronandroid.data.LauncherAction
import com.example.ioslauncheronandroid.data.LauncherSettings
import com.example.ioslauncheronandroid.ui.glass.glass
import com.example.ioslauncheronandroid.ui.settings.SettingsScreen
import com.example.ioslauncheronandroid.ui.wallpaper.WallpaperScreen
import com.example.ioslauncheronandroid.ui.widgets.WidgetPicker
import com.example.ioslauncheronandroid.ui.widgets.WidgetsPage
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.example.ioslauncheronandroid.data.AppInfo
import com.example.ioslauncheronandroid.data.AppRepository
import com.example.ioslauncheronandroid.ui.glass.Backdrop
import com.example.ioslauncheronandroid.ui.library.AppLibrary
import com.example.ioslauncheronandroid.ui.motion.Motion
import com.example.ioslauncheronandroid.ui.search.SpotlightSearch
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HomeScreen(apps: List<AppInfo>, backdrop: Backdrop?, homePressCount: Int, isDefault: Boolean = true) {
    val columns = LauncherSettings.columns
    val visible = remember(apps, LauncherSettings.hidden) { apps.filter { it.key !in LauncherSettings.hidden } }
    val dock = remember(visible) { AppRepository.pickDock(visible) }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val gridKeys = remember(visible, dock) { visible.filter { it !in dock }.map { it.key } }
    LaunchedEffect(gridKeys) { if (gridKeys.isNotEmpty()) HomeLayout.sync(gridKeys) }
    val cap = columns * GRID_ROWS
    val pages = HomeLayout.items.toList().chunked(cap)

    val pageCount = pages.size + 2
    val pager = rememberPagerState(initialPage = 1) { pageCount }
    var screen by remember { mutableStateOf(Screen.None) }
    var controlCenterOpen by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var menuRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    var showDots by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var homeMenu by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    var onboardingHidden by remember { mutableStateOf(false) }
    val needsWallpaper = Build.VERSION.SDK_INT >= 33 && !LauncherSettings.accessDismissed
    val hasWallpaper = backdrop?.soft != null
    val showOnboarding = !onboardingHidden && (!isDefault || (needsWallpaper && !hasWallpaper))
    var removeTarget by remember { mutableStateOf<AppInfo?>(null) }
    var widgetMenu by remember { mutableStateOf<WidgetMenu?>(null) }
    var openGroup by remember { mutableStateOf<Triple<String, List<AppInfo>, androidx.compose.ui.geometry.Rect>?>(null) }
    var openFolder by remember { mutableStateOf<Pair<String, androidx.compose.ui.geometry.Rect>?>(null) }
    val drag = remember { DragState() }
    val pillRect = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
    val fieldRect = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
    val pagerRect = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
    val dragJobs = remember { arrayOfNulls<kotlinx.coroutines.Job>(2) }
    val pendingHover = remember { arrayOfNulls<String>(1) }
    val jiggle = rememberJiggle(editMode)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var launching by remember { mutableStateOf<LaunchInfo?>(null) }
    val launchProgress = remember { Animatable(0f) }
    var leftLauncher by remember { mutableStateOf(false) }
    val launchApp: (AppInfo, androidx.compose.ui.geometry.Rect) -> Unit = { app, rect ->
        if (launching == null && !rect.isEmpty) {
            val px = android.graphics.Bitmap.createScaledBitmap(app.icon.asAndroidBitmap(), 1, 1, true).getPixel(0, 0)
            launching = LaunchInfo(app, rect, Color(px).copy(alpha = 1f))
            UsageStore.record(app.key)
            leftLauncher = false
            scope.launch {
                launchProgress.snapTo(0f)
                launch { launchProgress.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
                delay(210)
                val opts = android.app.ActivityOptions.makeCustomAnimation(context, R.anim.none, R.anim.none).toBundle()
                if (!AppRepository.launch(context, app, opts)) {
                    launchProgress.animateTo(0f, Motion.Settle); launching = null
                } else {
                    delay(1800)

                    if (!leftLauncher && launching != null) { launchProgress.animateTo(0f, Motion.Settle); launching = null }
                }
            }
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && launching != null) leftLauncher = true
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && launching != null && leftLauncher) {
                scope.launch {
                    launchProgress.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 260f))
                    launching = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val search = remember { Animatable(0f) }
    val entrance = remember { Animatable(0f) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty() && entrance.value == 0f) {
            entrance.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
        }
    }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect {
            if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) showDots = true else { delay(900); showDots = false }
        }
    }
    LaunchedEffect(homePressCount) {
        if (homePressCount > 0) {
            screen = Screen.None
            menuApp = null
            homeMenu = false
            editMode = false
            removeTarget = null
            openFolder = null
            openGroup = null
            widgetMenu = null
            search.animateTo(0f, Motion.Settle)
            pager.animateScrollToPage(1, animationSpec = Motion.Snappy)
        }
    }

    BackHandler(enabled = isDefault) { }
    BackHandler(enabled = editMode && removeTarget == null) { editMode = false }
    BackHandler(enabled = search.value > 0.02f) {
        scope.launch { search.animateTo(0f, Motion.Settle) }
    }

    val density = LocalDensity.current
    val searchScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || search.value <= 0f || available.y >= 0f) return Offset.Zero
                val next = (search.value + available.y / with(density) { 620.dp.toPx() }).coerceIn(0f, 1f)
                val used = (next - search.value) * with(density) { 620.dp.toPx() }
                scope.launch { search.snapTo(next) }
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                val next = (search.value + available.y / with(density) { 620.dp.toPx() }).coerceIn(0f, 1f)
                scope.launch { search.snapTo(next) }
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (search.value <= 0f) return Velocity.Zero
                val open = available.y > 900f || (available.y > -900f && search.value > 0.32f)
                search.animateTo(if (open) 1f else 0f, if (open) Motion.Jump else Motion.Settle)
                return available
            }
        }
    }

    fun cellAt(p: Offset, clamp: Boolean): Pair<Int, Offset>? {
        val r = pagerRect[0]
        val pg = pager.currentPage - 1
        if (r.isEmpty || pg < 0 || pg >= pages.size) return null
        val hp = with(density) { GridHPad.toPx() }
        val tp = with(density) { GridTopPad.toPx() }
        val cw = (r.width - 2 * hp) / columns
        val ch = (r.height - tp) / GRID_ROWS
        val lx = p.x - r.left - hp
        val ly = p.y - r.top - tp
        if (!clamp && (lx < 0 || ly < 0 || lx > cw * columns || ly > ch * GRID_ROWS)) return null
        val col = (lx / cw).toInt().coerceIn(0, columns - 1)
        val row = (ly / ch).toInt().coerceIn(0, GRID_ROWS - 1)
        return (pg * cap + row * columns + col) to Offset((lx - (col + 0.5f) * cw) / cw, (ly - (row + 0.5f) * ch) / ch)
    }
    fun reposition() {
        val item = drag.item ?: return
        val (idx, rel) = cellAt(drag.finger, clamp = true) ?: return
        val items = HomeLayout.items
        val target = idx.coerceIn(0, items.lastIndex)
        val cur = items.indexOfFirst { it.id == item.id }
        val over = items.getOrNull(target)
        val center = abs(rel.x) < 0.30f && abs(rel.y) < 0.30f

        val mode = when {
            over == null || over.id == item.id || cur < 0 -> 0
            item is AppItem && center -> 1
            else -> 2
        }
        val sig = "$target:$mode"
        if (mode == 0) {
            dragJobs[0]?.cancel(); pendingHover[0] = null; drag.folderHoverId = null
        } else if (pendingHover[0] != sig) {
            dragJobs[0]?.cancel(); drag.folderHoverId = null
            pendingHover[0] = sig
            dragJobs[0] = scope.launch {
                if (mode == 1) {
                    delay(380)
                    drag.folderHoverId = over!!.id
                } else {
                    delay(190)
                    val c = HomeLayout.items.indexOfFirst { it.id == item.id }
                    if (c >= 0 && c != target) HomeLayout.move(c, target)
                    pendingHover[0] = null
                }
                if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }

        val r = pagerRect[0]
        val edge = with(density) { 36.dp.toPx() }
        val dir = when {
            drag.finger.x < r.left + edge -> -1
            drag.finger.x > r.right - edge -> 1
            else -> 0
        }
        if (dir == 0) dragJobs[1]?.cancel()
        else if (dragJobs[1]?.isActive != true) {
            dragJobs[1] = scope.launch {
                delay(520)
                val next = pager.currentPage + dir
                if (next in 1..pages.size && drag.item != null) {
                    pager.animateScrollToPage(next, animationSpec = Motion.Snappy)
                    reposition()
                }
            }
        }
    }
    fun startDrag(p: Offset): Boolean {
        val (idx, _) = cellAt(p, clamp = false) ?: return false
        val item = HomeLayout.items.getOrNull(idx) ?: return false
        val r = pagerRect[0]
        val hp = with(density) { GridHPad.toPx() }
        val tp = with(density) { GridTopPad.toPx() }
        val cw = (r.width - 2 * hp) / columns
        val ch = (r.height - tp) / GRID_ROWS
        val within = idx - (pager.currentPage - 1) * cap
        val iconCenter = Offset(
            r.left + hp + (within % columns + 0.5f) * cw,
            r.top + tp + (within / columns) * ch + with(density) { (CellIconTop + LauncherSettings.iconSize.dp / 2).toPx() },
        )
        drag.grab = p - iconCenter
        drag.finger = p
        drag.item = item
        pendingHover[0] = null
        if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        return true
    }
    fun endDrag() {
        dragJobs.forEach { it?.cancel() }
        val d = drag.item
        val hover = drag.folderHoverId
        if (d is AppItem && hover != null) {
            if (HomeLayout.mergeInto(hover, d.key) && LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else HomeLayout.save()
        drag.clear()
    }

    CompositionLocalProvider(
        LocalIconMenu provides { app, rect -> menuRect = rect; menuApp = app },
        LocalEditMode provides editMode,
        LocalJiggle provides jiggle,
        LocalRemoveApp provides { app -> removeTarget = app },
        LocalLaunch provides launchApp,
        LocalWidgetMenu provides { m -> widgetMenu = m },
        LocalOpenGroup provides { title, list, rect -> openGroup = Triple(title, list, rect) },
    ) {
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { backdrop?.size = it }
            .pointerInput(controlCenterOpen) {
                if (controlCenterOpen) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x < size.width * 0.65f ||
                        down.position.y > 100.dp.toPx()
                    ) return@awaitEachGesture

                    val slop = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    }

                    if (slop != null) {
                        var lastY = slop.position.y
                        var distance = lastY - down.position.y

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: break
                            if (!change.pressed) break

                            val currentY = change.position.y
                            distance += currentY - lastY
                            lastY = currentY

                            if (distance > 72.dp.toPx()) {
                                controlCenterOpen = true
                                break
                            }
                            change.consume()
                        }
                    }
                }
            }
            .pointerInput(Unit) { detectTapGestures(
                    onLongPress = {
                        if (!editMode) {
                            if (LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            editMode = true
                        }
                    },
                    onTap = { pos -> if (editMode && cellAt(pos, clamp = false)?.first?.let { HomeLayout.items.getOrNull(it) } == null) editMode = false },
                ) }
            .pointerInput(editMode) {
                if (editMode) return@pointerInput
                val tracker = VelocityTracker()
                var startTime = 0L
                var travelled = 0f
                detectVerticalDragGestures(
                    onDragStart = { tracker.resetTracking(); startTime = System.currentTimeMillis(); travelled = 0f },
                    onDragEnd = {
                        val avg = travelled / ((System.currentTimeMillis() - startTime).coerceAtLeast(1L) / 1000f)
                        val v = if (kotlin.math.abs(avg) > kotlin.math.abs(dragVelocity)) avg else dragVelocity
                        val open = when {
                            v > 900f -> true
                            v < -900f -> false
                            else -> search.value > 0.32f
                        }
                        if (open && LauncherSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        scope.launch { search.animateTo(if (open) 1f else 0f, if (open) Motion.Jump else Motion.Settle) }
                    },
                    onDragCancel = {
                        scope.launch { search.animateTo(0f, Motion.Settle) }
                    },
                ) { change, dy ->
                    change.consume()
                    travelled += dy
                    tracker.addPosition(change.uptimeMillis, change.position)
                    dragVelocity = tracker.calculateVelocity().y
                    scope.launch {
                        search.snapTo((search.value + dy / 620.dp.toPx()).coerceIn(0f, 1f))
                    }
                }
            }
            .pointerInput(editMode) {
                if (!editMode) return@pointerInput
                detectLongPressDragInitial(
                    onStart = { startDrag(it) },
                    onMove = { drag.finger = it; reposition() },
                    onEnd = { endDrag() },
                )
            },
    ) {
        HomeBackground(backdrop)
        val editTop by animateDpAsState(if (editMode) 56.dp else 0.dp, Motion.Dp, label = "editTop")
        val searching = search.value > 0.001f
        val overlayOpen = homeMenu || widgetMenu != null || screen != Screen.None || (showOnboarding && apps.isNotEmpty())
        val blurModifier = if ((searching || overlayOpen) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(if (overlayOpen) 20.dp else (22 * search.value).dp)
        } else {
            Modifier
        }
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = search.value
                    val lp = launchProgress.value
                    val s = (1f - 0.075f * progress) * (1f + 0.10f * lp)
                    scaleX = s
                    scaleY = s
                    alpha = (1f - 0.72f * progress) * (1f - 0.55f * lp)
                    translationY = 18.dp.toPx() * progress
                    transformOrigin = TransformOrigin(0.5f, 0.55f)
                }
                .then(blurModifier)
                .statusBarsPadding()
                .padding(top = editTop.coerceAtLeast(0.dp))
                .navigationBarsPadding(),
        ) {
            Box(Modifier.weight(1f).nestedScroll(searchScroll)) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().onGloballyPositioned {
                    val bnd = it.boundsInWindow()

                    pagerRect[0] = androidx.compose.ui.geometry.Rect(bnd.left, bnd.top, bnd.right, bnd.bottom - with(density) { IndicatorSlot.toPx() })
                },
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1,
            ) { page ->
                val offset = { (pager.currentPage - page) + pager.currentPageOffsetFraction }
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val t = (abs(offset()) / 1.2f).coerceIn(0f, 1f)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            alpha = 1f - 0.55f * t
                            val s = 1f - 0.1f * t
                            scaleX = s
                            scaleY = s
                            translationX = -offset() * 12.dp.toPx()
                        }

                        .padding(bottom = if (page == 0 || page == pageCount - 1) 0.dp else IndicatorSlot),
                ) {
                    when {
                        page == 0 -> WidgetsPage(apps = visible, onAddWidget = { screen = Screen.WidgetPicker }, entrance = { entrance.value })
                        page - 1 < pages.size -> HomeGrid(
                            items = pages[page - 1],
                            apps = byKey,
                            columns = columns,
                            baseIndex = (page - 1) * cap,
                            drag = drag,
                            pageOffset = offset,
                            entrance = { entrance.value },
                            onOpenFolder = { f, r -> openFolder = f.id to r },
                        )
                        else -> AppLibrary(visible, entrance = { entrance.value }, onAddWidget = { screen = Screen.LibraryWidgetPicker })
                    }
                }
            }
            PageIndicator(
                pager = pager,
                count = pageCount,
                showDots = (showDots || scrubbing) && search.value < 0.01f,
                onScrub = { scrubbing = it },
                enabled = pager.currentPage != 0,
                tapEnabled = pager.currentPage != 0 && pager.currentPage != pageCount - 1,
                pillBounds = pillRect,
                onSearch = { scope.launch { search.animateTo(1f, Motion.Jump) } },
                tick = { entrance.value },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 8.dp)
                    .graphicsLayer {
                        val pos = pager.currentPage + pager.currentPageOffsetFraction
                        val today = (1f - pos).coerceIn(0f, 1f)
                        val library = (pos - (pageCount - 2)).coerceIn(0f, 1f)

                        val hide = if (scrubbing) 0f else maxOf(today, library)

                        alpha = entrance.value * (1f - hide) * (1f - (search.value * 10f).coerceIn(0f, 1f))
                        translationY = (1f - entrance.value) * 12.dp.toPx()
                    },
            )
            }

            Dock(
                apps = dock,
                tick = { entrance.value },
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .graphicsLayer {
                        alpha = entrance.value
                        translationY = (1f - entrance.value) * 28.dp.toPx()
                        scaleX = 0.92f + 0.08f * entrance.value
                        scaleY = scaleX
                    },
            )
        }
        if (searching) {
            SpotlightSearch(
                progress = search.value,
                apps = apps,
                onClose = { scope.launch { search.animateTo(0f, Motion.Settle) } },
                onFieldBounds = { fieldRect[0] = it },
                onAction = { a ->
                    screen = when (a) {
                        LauncherAction.Settings -> Screen.Settings
                        LauncherAction.Wallpapers -> Screen.Wallpapers
                        LauncherAction.AddWidget -> Screen.WidgetPicker
                    }
                },
            )
        }
        if (searching) {
            val statusTop = WindowInsets.statusBars.getTop(density)
            val sideMargin = with(density) { 16.dp.toPx() }
            val fallback = androidx.compose.ui.geometry.Rect(
                sideMargin, statusTop + sideMargin,
                (backdrop?.size?.width ?: 1080) - sideMargin, statusTop + sideMargin + with(density) { 52.dp.toPx() },
            )
            SearchMorph(
                progress = { search.value },
                to = { if (fieldRect[0].isEmpty) fallback else fieldRect[0] },
                from = { if (pillRect[0].isEmpty) fallback else pillRect[0] },
            )
        }
        DragOverlay(drag, byKey)
        if (showOnboarding && apps.isNotEmpty()) Onboarding(
            isDefault = isDefault,
            hasWallpaper = hasWallpaper,
            needsWallpaper = needsWallpaper,
            onSkip = {
                if (isDefault && !hasWallpaper) LauncherSettings.accessDismissed = true
                onboardingHidden = true
            },
        )
        openFolder?.let { (id, rect) -> FolderOverlay(id, rect, byKey) { openFolder = null } }
        openGroup?.let { (title, list, rect) -> GroupOverlay(title, list, rect) { openGroup = null } }
        launching?.let { LaunchOverlay(it) { launchProgress.value } }
        if (editMode) EditBar(
            onCustomize = { homeMenu = true },
            onDone = { editMode = false },
        )
        menuApp?.let { app ->
            val folder = HomeLayout.folderOf(app.key)
            IconMenu(
                app, menuRect,
                shortcuts = remember(app) { AppRepository.shortcuts(context, app.component.packageName) },
                onRemoveFromFolder = folder?.let { f -> { HomeLayout.removeFromFolder(f.id, app.key); openFolder = null } },
            ) { menuApp = null }
        }
        removeTarget?.let { RemoveConfirm(it) { removeTarget = null } }
        widgetMenu?.let { GlassContextMenu(it) { widgetMenu = null } }
        if (homeMenu) HomeMenu(
            onDismiss = { homeMenu = false },
            onPick = { screen = it; homeMenu = false },
        )
        when (screen) {
            Screen.Settings -> SettingsScreen(
                apps, { screen = Screen.None },
                onWallpapers = { screen = Screen.Wallpapers },
                onAddWidget = { screen = Screen.WidgetPicker },
            )
            Screen.Wallpapers -> WallpaperScreen { screen = Screen.None }
            Screen.WidgetPicker -> WidgetPicker(toLibrary = false) { screen = Screen.None }
            Screen.LibraryWidgetPicker -> WidgetPicker(toLibrary = true) { screen = Screen.None }
            Screen.None -> {}
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = "CONTROL",
                color = Color.White,
                modifier = Modifier
                    .padding(18.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE52D3545))
                    .clickable { controlCenterOpen = true }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }

        if (controlCenterOpen) {
            ControlCenter(onDismiss = { controlCenterOpen = false })
        }

    }
    }
}
