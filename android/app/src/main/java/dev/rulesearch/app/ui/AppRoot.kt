package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 底部 dock 标签页：搜索 / 收藏 / 足迹 / 设置，切换带滑动+淡入动画。 */
@Composable
fun AppRoot(
    vm: SearchViewModel = viewModel(),
    initialKeyword: String? = null,
    autoOpenDialog: Boolean = false,
    clipboardLink: String? = null,
    onClipboardConsumed: () -> Unit = {},
) {
    // 0=搜索，1=收藏，2=足迹，3=设置
    var tab by remember { mutableIntStateOf(0) }

    // 非搜索页按返回：回到搜索页
    BackHandler(enabled = tab != 0) { tab = 0 }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = Icons.Filled.Search,
                    label = "搜索",
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = Icons.Filled.FavoriteBorder,
                    label = "收藏",
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = Icons.Filled.Refresh,
                    label = "足迹",
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = Icons.Filled.Settings,
                    label = "设置",
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    // 往右切往左滑，往左切往右滑
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it * dir / 4 })
                        .togetherWith(
                            fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it * dir / 4 },
                        )
                },
                label = "dock_tab",
            ) { t ->
                when (t) {
                    0 -> SearchTab(
                        vm = vm,
                        initialKeyword = initialKeyword,
                        autoOpenDialog = autoOpenDialog,
                    )
                    1 -> FavoritesTab(vm = vm)
                    2 -> FootprintsTab(vm = vm)
                    else -> SettingsTab(vm = vm)
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
