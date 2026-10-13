package dev.rulesearch.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.LinkStatus
import dev.rulesearch.app.LinkTypes
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel
import dev.rulesearch.app.checkLink
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 从标题里抠提取码：「提取码：8x4k」「密码 1234」这类常见写法。 */
fun extractCode(title: String): String? =
    Regex("(提取码|访问码|密码|取件码|code)[：:\\s]*([A-Za-z0-9]{4,8})", RegexOption.IGNORE_CASE)
        .find(title)
        ?.groupValues?.getOrNull(2)
        ?.takeIf { it.any(Char::isDigit) || it.length >= 4 }

/** 点结果行后的底部详情弹层：只做"拿走"这一件事。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    result: SearchResultUi,
    onDismiss: () -> Unit,
    vm: SearchViewModel? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val linkType = remember(result) { LinkTypes.detect(result.url, result.title) }
    var status by remember(result) { mutableStateOf<LinkStatus>(LinkStatus.Checking) }
    var checking by remember(result) { mutableStateOf(false) }
    var showNoDl by remember { mutableStateOf(false) }
    val isMagnet = linkType == LinkTypes.MAGNET
    val code = remember(result) { extractCode(result.title) }

    fun toast(msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // 打开即检测：用一键检测的缓存，不重复请求
    LaunchedEffect(result) {
        val cached = vm?.linkStatus?.get(result.url)
        status = when {
            isMagnet -> LinkStatus.Skipped
            cached != null -> if (cached) LinkStatus.Reachable else LinkStatus.Unreachable
            else -> checkLink(result.url)
        }
    }

    fun recheck() {
        if (isMagnet || checking) return
        scope.launch {
            checking = true
            status = checkLink(result.url)
            checking = false
        }
    }

    val statusText = when (status) {
        LinkStatus.Checking -> "链接检测中…"
        LinkStatus.Reachable -> "链接可访问"
        LinkStatus.Unreachable -> "链接无法访问"
        LinkStatus.Skipped -> "磁力链接，无需检测"
    }
    val statusColor = when (status) {
        LinkStatus.Reachable -> COLOR_REACHABLE
        LinkStatus.Unreachable -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.secondary
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MiuixTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 标题 + 收藏星标
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = result.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (vm != null) {
                    val favored = vm.isFavorite(result.url)
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .clickable {
                                vm.toggleFavorite(result)
                                toast(if (favored) "已取消收藏" else "已收藏")
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (favored) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = if (favored) "取消收藏" else "收藏",
                            tint = if (favored) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            // 元信息：类型 · 大小/时间 · 来源 · 存活状态
            val meta = result.size.ifEmpty { result.time }
            Text(
                text = buildString {
                    append(linkType.label)
                    if (meta.isNotEmpty()) append(" · $meta")
                    append(" · ${result.source}")
                    append(" · $statusText")
                },
                fontSize = 12.sp,
                color = statusColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // 链接行：可点 + 复制
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.url,
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SheetTextButton(label = "复制") {
                    clipboard.setText(AnnotatedString(result.url))
                    toast("链接已复制")
                }
            }
            // 提取码：单独大字展示，点按复制
            if (code != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackground())
                        .clickable {
                            clipboard.setText(AnnotatedString(code))
                            toast("提取码已复制")
                        }
                        .padding(vertical = 14.dp),
                ) {
                    Text(
                        text = "提取码 · 点按复制",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.secondary,
                    )
                    Text(
                        text = code,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 6.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            // 底部三按钮：打开链接 ｜ 分享 ｜ 已检测有效（15px 蓝，均分）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SheetTextButton(
                        label = if (isMagnet) "用下载器打开" else "打开链接",
                        primary = true,
                    ) {
                        if (openLink(context, result.url) { showNoDl = true }) {
                            onDismiss()
                        }
                    }
                }
                Box(
                    modifier = Modifier.size(1.dp, 20.dp)
                        .background(MiuixTheme.colorScheme.outline),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SheetTextButton(label = "分享", primary = true) {
                        val text = "${result.title}\n${result.url}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享"))
                    }
                }
                if (!isMagnet) {
                    Box(
                        modifier = Modifier.size(1.dp, 20.dp)
                            .background(MiuixTheme.colorScheme.outline),
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        // 已检测有效为纯状态展示，不可点
                        val done = status == LinkStatus.Reachable && !checking
                        if (done) {
                            Text(
                                text = "已检测有效",
                                fontSize = 15.sp,
                                color = COLOR_REACHABLE,
                            )
                        } else {
                            SheetTextButton(
                                label = if (checking) "检测中…" else "检测有效性",
                                primary = true,
                            ) { recheck() }
                        }
                    }
                }
            }
        }
    }

    if (showNoDl) {
        NoDownloaderDialog(
            link = result.url,
            onDismiss = {
                showNoDl = false
                onDismiss()
            },
        )
    }
}

/** 弹层里的文字按钮：主操作染主色，其余默认色。 */
@Composable
private fun SheetTextButton(
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (primary) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
    }
}
