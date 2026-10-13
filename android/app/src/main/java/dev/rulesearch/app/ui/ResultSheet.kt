package dev.rulesearch.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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

/** 标题关键词高亮：keyword 在标题中的出现处染主色。 */
@Composable
private fun highlightedTitle(title: String, keyword: String): AnnotatedString {
    val primary = MiuixTheme.colorScheme.primary
    return remember(title, keyword, primary) {
        buildAnnotatedString {
            val kw = keyword.trim()
            if (kw.isEmpty()) {
                append(title)
            } else {
                var startIndex = 0
                while (true) {
                    val idx = title.indexOf(kw, startIndex, ignoreCase = true)
                    if (idx < 0) {
                        append(title.substring(startIndex))
                        break
                    }
                    append(title.substring(startIndex, idx))
                    withStyle(SpanStyle(color = primary)) {
                        append(title.substring(idx, idx + kw.length))
                    }
                    startIndex = idx + kw.length
                }
            }
        }
    }
}

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
    val primary = MiuixTheme.colorScheme.primary

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

    val keyword = vm?.keyword?.trim().orEmpty()
    val titleAnnotated = highlightedTitle(result.title, keyword)
    val isReachable = status == LinkStatus.Reachable && !checking && !isMagnet
    val secondaryBtnBg = if (isSystemInDarkTheme()) Color(0xFF5A5A5A) else Color(0xFFE9E9EB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cardBackground(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 标题 + 收藏星标（关键词高亮）
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = titleAnnotated,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp,
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
                            tint = if (favored) primary
                            else MiuixTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            // 状态标签：只有检测有效时显示（绿点 + 链接有效）
            if (isReachable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(COLOR_REACHABLE),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "链接有效",
                        fontSize = 12.sp,
                        color = COLOR_REACHABLE,
                    )
                }
            }
            // 元信息：网盘徽章 + 大小/时间 · 来源
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isMagnet) {
                    // 网盘徽章：圆角 6dp，主色 12% 背景，主色文字
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = linkType.label,
                            fontSize = 12.sp,
                            color = primary,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                val sizeTime = result.size.ifEmpty { result.time }
                val metaText = buildString {
                    if (sizeTime.isNotEmpty()) append(sizeTime)
                    if (isNotEmpty()) append(" · ")
                    append(result.source)
                }
                Text(
                    text = metaText,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 链接行：截断显示 + 复制（复制完整链接）
            Row(verticalAlignment = Alignment.CenterVertically) {
                val displayUrl =
                    if (result.url.length > 32) result.url.take(32) + "..." else result.url
                Text(
                    text = displayUrl,
                    fontSize = 14.sp,
                    color = primary,
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
                        .background(cardGray())
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
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 6.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            // 主按钮：打开链接（蓝色实心，48dp 高，16dp 圆角）
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(primary)
                    .clickable {
                        if (openLink(context, result.url) { showNoDl = true }) {
                            onDismiss()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isMagnet) "用下载器打开" else "打开链接",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
            // 次按钮：分享（灰底，48dp 高，16dp 圆角）
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(secondaryBtnBg)
                    .clickable {
                        val text = "${result.title}\n${result.url}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享"))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "分享",
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                )
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
