package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.SearchViewModel
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 独立搜索页：点首页/结果页搜索框进入；历史列表 + 在线建议；搜后进结果页。
 * 替代之前的下拉覆盖——全屏独占一页，不再有盖住搜索框/键盘/胶囊的问题。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchPage(
    vm: SearchViewModel,
    initialText: String,
    onClose: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
) {
    var text by remember { mutableStateOf(initialText) }
    var online by remember { mutableStateOf<List<String>>(emptyList()) }

    BackHandler { onClose() }

    // 在线建议：输入防抖 400ms，失败就只显示历史
    LaunchedEffect(text) {
        val q = text.trim()
        if (q.isEmpty()) {
            online = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        val latest = text.trim()
        if (latest != q || latest.isEmpty()) return@LaunchedEffect
        val hist = vm.suggestionsFor(latest)
        online = vm.onlineSuggestions(latest)
            .filter { s -> hist.none { it.equals(s, ignoreCase = true) } }
    }

    fun doSearch(kw: String) {
        val k = kw.trim()
        if (k.isEmpty()) return
        vm.keyword = k
        vm.search()
        onClose()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(pageBackground())
            // 点空白处回首页（行内点击由各行自己消费，不会误触）
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() },
    ) {
        // 顶栏：返回 + 搜索框（48dp，自动聚焦弹键盘）
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
            SearchInput(
                text = text,
                onTextChange = { text = it },
                onSearch = ::doSearch,
                autoFocus = true,
                modifier = Modifier.weight(1f),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        // 下方列表：空输入显示历史，有输入显示在线建议
        val showHistory = text.isBlank()
        val showOnline = text.isNotBlank() && online.isNotEmpty()
        // 历史/建议列表：比搜索框晚 80ms 淡入，框先落位、列表再出现
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(durationMillis = 300, delayMillis = 80)),
            exit = fadeOut(tween(durationMillis = 200)),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (showHistory) {
                    if (vm.history.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "暂无搜索历史",
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.secondary,
                                )
                            }
                        }
                    } else {
                        items(vm.history) { h ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { doSearch(h) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ClockIcon(
                                    tint = MiuixTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = h,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                )
                                // 右侧 ↙：点一下直接搜这条
                                Box(
                                    modifier = Modifier.size(32.dp)
                                        .clickable { doSearch(h) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "直接搜索",
                                        tint = MiuixTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp).rotate(-45f),
                                    )
                                }
                            }
                        }
                    }
                }
                if (showOnline) {
                    items(online) { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { doSearch(s) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = s,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 手绘时钟图标（material-icons-core 没有 History，Canvas 画最稳）。 */
@Composable
private fun ClockIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val r = size.minDimension / 2 - stroke / 2
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(color = tint, radius = r, style = Stroke(width = stroke))
        drawLine(
            color = tint, start = c, end = c + Offset(0f, -r * 0.55f),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )
        drawLine(
            color = tint, start = c, end = c + Offset(r * 0.45f, r * 0.3f),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )
    }
}
