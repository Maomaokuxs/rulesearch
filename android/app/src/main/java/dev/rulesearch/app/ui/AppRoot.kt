package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.rulesearch.app.SearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 底部 dock 标签页：搜索 / 收藏 / 我的，tab 点击淡入淡出切换，横滑保留滑动。 */
@Composable
fun AppRoot(
    vm: SearchViewModel = viewModel(),
    initialKeyword: String? = null,
    autoOpenDialog: Boolean = false,
    clipboardLink: String? = null,
    onClipboardConsumed: () -> Unit = {},
) {
    // 0=搜索，1=收藏，2=我的
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    // tab 切换淡入淡出：淡出当前页 -> 瞬间跳页 -> 淡入新页，不经过中间页
    var pageAlpha by remember { mutableStateOf(1f) }
    var fadeJob by remember { mutableStateOf<Job?>(null) }
    fun goTab(i: Int) {
        if (pagerState.currentPage == i) return
        fadeJob?.cancel()
        fadeJob = scope.launch {
            Animatable(1f).animateTo(0f, tween(150)) { pageAlpha = value }
            pagerState.scrollToPage(i)
            Animatable(0f).animateTo(1f, tween(150)) { pageAlpha = value }
        }
    }

    // 非搜索页按返回：回到搜索页
    BackHandler(enabled = pagerState.currentPage != 0) {
        vm.clearResults()
        goTab(0)
    }

    // Scaffold 默认 containerColor 是 surface（浅色白），而页面背景是 pageBackground()（浅色 #F2F3F5），
    // 状态栏后面会露出一道白条；统一成 pageBackground() 让状态栏与页面同色。
    Scaffold(
        containerColor = pageBackground(),
        bottomBar = {
            NavigationBar(
                color = if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0xFF1A1A1A) else androidx.compose.ui.graphics.Color.White,
            ) {
                val navColors = top.yukonga.miuix.kmp.basic.NavigationBarDefaults.navigationBarItemColors(
                    selectedContentColor = MiuixTheme.colorScheme.primary,
                    unselectedContentColor = MiuixTheme.colorScheme.secondary,
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { vm.clearResults(); goTab(0) },
                    icon = Icons.Filled.Search,
                    label = "搜索",
                    colors = navColors,
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { goTab(1) },
                    icon = Icons.Filled.FavoriteBorder,
                    label = "收藏",
                    colors = navColors,
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { goTab(2) },
                    icon = Icons.Filled.Person,
                    label = "我的",
                    colors = navColors,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pageAlpha },
            ) { page ->
                when (page) {
                    0 -> SearchTab(
                        vm = vm,
                        initialKeyword = initialKeyword,
                        autoOpenDialog = autoOpenDialog,
                    )
                    1 -> FavoritesTab(vm = vm)
                    else -> MineTab(vm = vm)
                }
            }
        }
    }

    // 剪贴板嗅探到链接：弹窗询问是否打开
    if (clipboardLink != null) {
        ClipboardLinkDialog(
            link = clipboardLink,
            onDismiss = onClipboardConsumed,
        )
    }
}

/** 剪贴板链接提示弹窗。 */
@Composable
private fun ClipboardLinkDialog(link: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isMagnet = link.startsWith("magnet:", ignoreCase = true)
    var showNoDl by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            insideMargin = PaddingValues(20.dp),
        ) {
            Text(
                text = "检测到剪贴板链接",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            Text(
                text = link,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.secondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Button(
                    onClick = onDismiss,
                ) {
                    Text("忽略")
                }
                Button(
                    onClick = {
                        if (openLink(context, link) { showNoDl = true }) {
                            onDismiss()
                        }
                    },
                ) {
                    Text(if (isMagnet) "用下载器打开" else "打开链接")
                }
            }
        }
    }

    if (showNoDl) {
        NoDownloaderDialog(
            link = link,
            onDismiss = {
                showNoDl = false
                onDismiss()
            },
        )
    }
}
