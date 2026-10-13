package dev.rulesearch.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SortMode
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/** 结果页：单列结果行 + 顶部筛选入口（无 FAB）。 */
@Composable
internal fun ResultsView(
    vm: SearchViewModel,
    initialKeyword: String?,
    onResultClick: (SearchResultUi) -> Unit,
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    val keyword = vm.keyword
    val focusManager = LocalFocusManager.current

    // 搜索框 FLIP 入场：从首页位置（y+200dp、56dp 高）缩小上移到顶部（48dp），进入结果页时播放一次
    val flipOffsetY = remember { Animatable(200f) }
    val flipScale = remember { Animatable(56f / 48f) }
    // 只在新关键词搜索时播 FLIP，切 tab/切分段不重播
    LaunchedEffect(keyword) {
        if (vm.flipPlayedForKeyword != keyword) {
            vm.flipPlayedForKeyword = keyword
            launch { flipOffsetY.animateTo(0f, tween(300)) }
            launch { flipScale.animateTo(1f, tween(300)) }
        } else {
            flipOffsetY.snapTo(0f)
            flipScale.snapTo(1f)
        }
    }

    // 骨架屏最少显示 800ms，避免加载过快时闪烁
    var showSkeleton by remember { mutableStateOf(vm.loading) }
    val skeletonStart = remember { mutableLongStateOf(if (vm.loading) SystemClock.uptimeMillis() else 0L) }
    LaunchedEffect(vm.loading) {
        if (vm.loading) {
            skeletonStart.longValue = SystemClock.uptimeMillis()
            showSkeleton = true
        } else {
            val elapsed = SystemClock.uptimeMillis() - skeletonStart.longValue
            val remain = 800L - elapsed
            if (remain > 0) delay(remain)
            if (!vm.loading) showSkeleton = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶栏 FLIP：从首页位置缩小上移到顶部（mock 方案4）
        // 漏斗筛选按钮：无底灰色线形图标，有筛选条件时染蓝
        val filterActive = vm.sortMode != SortMode.DEFAULT ||
            vm.hiddenPanBrands.isNotEmpty() || vm.onlyReachable
        val flipDensity = LocalDensity.current
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp)
                .offset { with(flipDensity) { IntOffset(0, flipOffsetY.value.dp.roundToPx()) } }
                .graphicsLayer {
                    scaleX = flipScale.value
                    scaleY = flipScale.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
        ) {
            var resShowDropdown by remember { mutableStateOf(false) }
            SearchInput(
                vm = vm,
                initialKeyword = keyword.ifBlank { initialKeyword },
                showDropdown = resShowDropdown,
                onShowDropdownChange = { resShowDropdown = it },
                compact = true,
                showFunnel = true,
                funnelActive = filterActive,
                onFunnelClick = { showFilterSheet = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // 多选模式指示行：已选 N 17sp 加粗黑 + 右取消 15sp 蓝
        if (vm.selectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "已选 ${vm.selectedUrls.size}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.clip(CircleShape)
                        .clickable { vm.toggleSelectionMode() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "取消",
                        fontSize = 15.sp,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        }

        when {
            vm.error != null -> {
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val tabTitles = vm.groupedResults.map { (title, list) -> "$title ${list.size}" }
                    if (tabTitles.isNotEmpty()) {
                        SegmentedControl(
                            tabs = tabTitles,
                            selectedIndex = vm.selectedTab,
                            onSelect = vm::selectTab,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp),
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "出错：" + vm.error.orEmpty(),
                            color = MiuixTheme.colorScheme.secondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            else -> Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // 分段控件：始终显示，骨架屏时也不隐藏（加载时用占位）
                val tabTitles = vm.groupedResults.map { (title, list) -> "$title ${list.size}" }
                    .ifEmpty { listOf("网盘", "磁力") }
                SegmentedControl(
                    tabs = tabTitles,
                    selectedIndex = vm.selectedTab,
                    onSelect = vm::selectTab,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                )
                // 结果列表圆角容器：顶部 16dp 圆角
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White),
                ) {
                    Crossfade(
                        targetState = showSkeleton,
                        animationSpec = tween(200),
                        modifier = Modifier.fillMaxSize(),
                        label = "skeleton_content",
                    ) { skeleton ->
                        if (skeleton) {
                            SkeletonRows()
                        } else if (vm.results.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "没有搜到相关资源，换个关键词试试",
                                    color = MiuixTheme.colorScheme.secondary,
                                    fontSize = 14.sp,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(vm.currentTabResults, key = { it.url }) { r ->
                                    ResultRow(
                                        r = r,
                                        keyword = keyword,
                                        selectionMode = vm.selectionMode,
                                        selected = r.url in vm.selectedUrls,
                                        linkOk = vm.linkStatus[r.url],
                                        onClick = {
                                            if (vm.selectionMode) vm.toggleSelect(r.url)
                                            else onResultClick(r)
                                        },
                                        onLongClick = {
                                            if (!vm.selectionMode) {
                                                vm.toggleSelectionMode()
                                                vm.toggleSelect(r.url)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // 多选模式底部操作条：复制所选 / 分享
        if (vm.selectionMode) {
            SelectionBar(vm = vm)
        }
    }

    // 筛选 bottom sheet：排序 + 网盘品牌 + 仅看有效链接
    if (showFilterSheet) {
        FilterSheet(vm = vm, onDismiss = { showFilterSheet = false })
    }
}


/** 加载中的骨架行：mock 方案1 —— 4 条，标题行 + meta 行，shimmer 扫光。 */
@Composable
private fun SkeletonRows() {
    val brush = shimmerBrush()
    val hairline = if (isSystemInDarkTheme()) Color(0xFF1C1C1C) else Color(0xFFECECEC)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        repeat(8) { i ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.75f).height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(0.45f).height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush),
                    )
                }
                // 右侧有效占位小圆点
                Box(
                    modifier = Modifier.size(8.dp)
                        .clip(CircleShape)
                        .background(brush),
                )
            }
            if (i < 3) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.dp)
                        .background(hairline),
                )
            }
        }
    }
}

/** shimmer 扫光笔刷：深浅自适应。 */
@Composable
private fun shimmerBrush(): Brush {
    val base = cardGray()
    val highlight = if (isSystemInDarkTheme()) Color(0xFF525252) else Color.White
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 500f, 0f),
        end = Offset(translate, 0f),
    )
}

/** 多选模式底部操作条：复制所选 / 分享。 */
@Composable
private fun SelectionBar(vm: SearchViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val count = vm.selectedUrls.size

    fun copySelected() {
        val text = vm.selectedResults().joinToString("\n") { it.url }
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, "已复制 $count 条链接", Toast.LENGTH_SHORT).show()
    }

    fun shareSelected() {
        val text = vm.selectedResults().joinToString("\n\n") { "${it.title}\n${it.url}" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享链接"))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = ::copySelected,
            enabled = count > 0,
            modifier = Modifier.weight(1f),
        ) {
            Text("复制所选($count)")
        }
        Button(
            onClick = ::shareSelected,
            enabled = count > 0,
            modifier = Modifier.weight(1f),
        ) {
            Text("分享")
        }
    }

}
