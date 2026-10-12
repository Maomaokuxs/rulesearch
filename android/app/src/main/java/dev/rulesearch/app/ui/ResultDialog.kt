package dev.rulesearch.app.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import dev.rulesearch.app.LinkStatus
import dev.rulesearch.app.LinkType
import dev.rulesearch.app.LinkTypes
import dev.rulesearch.app.R
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel
import dev.rulesearch.app.checkLink
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 点卡片后的弹窗：识别网盘类型、检测链接是否可访问，再决定跳转。 */
@Composable
fun ResultDialog(
    result: SearchResultUi,
    onDismiss: () -> Unit,
    vm: SearchViewModel? = null, // 传了才显示收藏按钮
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val linkType: LinkType = remember(result) { LinkTypes.detect(result.url, result.title) }
    var status by remember(result) { mutableStateOf<LinkStatus>(LinkStatus.Checking) }
    var showNoDl by remember { mutableStateOf(false) }
    val isMagnet = linkType == LinkTypes.MAGNET

    LaunchedEffect(result) {
        // 一键检测跑过就有缓存，直接用，不重复请求
        val cached = vm?.linkStatus?.get(result.url)
        status = when {
            linkType == LinkTypes.MAGNET -> LinkStatus.Skipped
            cached != null -> if (cached) LinkStatus.Reachable else LinkStatus.Unreachable
            else -> checkLink(result.url)
        }
    }

    val statusText = when (status) {
        LinkStatus.Checking -> "链接检测中…"
        LinkStatus.Reachable -> "链接可访问"
        LinkStatus.Unreachable -> "链接无法访问"
        LinkStatus.Skipped -> "磁力链接，无需检测"
    }
    val statusColor = when (status) {
        LinkStatus.Reachable -> MiuixTheme.colorScheme.primary
        LinkStatus.Unreachable -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.secondary
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            insideMargin = PaddingValues(20.dp),
        ) {
            Text(
                text = result.title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${linkType.label} · $statusText",
                fontSize = 13.sp,
                color = statusColor,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = result.url,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            // 图标按钮行：横屏右对齐，竖屏居中；长按显示文字说明
            var showAddFilter by remember { mutableStateOf(false) }
            val isLandscape =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    4.dp,
                    if (isLandscape) Alignment.End else Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                if (vm != null) {
                    // 一键添加过滤词
                    DialogIconButton(
                        resId = R.drawable.ic_block,
                        label = "添加过滤词",
                        onClick = { showAddFilter = true },
                    )
                }
                DialogIconButton(
                    resId = R.drawable.ic_copy,
                    label = "复制链接",
                    onClick = {
                        clipboard.setText(AnnotatedString(result.url))
                        Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                    },
                )
                if (vm != null) {
                    val favored = vm.isFavorite(result.url)
                    DialogIconButton(
                        imageVector = if (favored) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        label = if (favored) "取消收藏" else "收藏",
                        tint = if (favored) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurface,
                        onClick = {
                            vm.toggleFavorite(result)
                            Toast.makeText(
                                context,
                                if (favored) "已取消收藏" else "已收藏",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
                // 最常用的打开链接放最右
                DialogIconButton(
                    resId = if (isMagnet) R.drawable.ic_download else R.drawable.ic_open_in_new,
                    label = if (isMagnet) "用下载器打开" else "在浏览器打开",
                    onClick = {
                        // 磁力：有下载器弹选择器，没有则提示安装；网盘走浏览器
                        if (openLink(context, result.url) { showNoDl = true }) {
                            onDismiss()
                        }
                    },
                )
            }
            if (showAddFilter && vm != null) {
                AddFilterWordDialog(
                    onDismiss = { showAddFilter = false },
                    onAdd = { word ->
                        vm.addBlockedWord(word)
                        Toast.makeText(context, "已添加过滤词：$word", Toast.LENGTH_SHORT).show()
                        showAddFilter = false
                    },
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

/** 弹窗里的图标按钮：点击执行，长按 Toast 显示文字说明。 */
@Composable
private fun DialogIconButton(
    label: String,
    onClick: () -> Unit,
    resId: Int? = null,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.onSurface,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (resId != null) {
            Icon(
                painter = painterResource(id = resId),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        } else if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 一键添加过滤词的小弹窗。 */
@Composable
private fun AddFilterWordDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            insideMargin = PaddingValues(20.dp),
        ) {
            Text(
                text = "添加过滤词",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                text = "标题含有该词的结果会被过滤",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextField(
                value = input,
                onValueChange = { input = it },
                label = "过滤词",
                useLabelAsPlaceholder = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                TextButton(text = "取消", onClick = onDismiss)
                Button(
                    onClick = { if (input.isNotBlank()) onAdd(input.trim()) },
                ) {
                    Text("添加")
                }
            }
        }
    }
}
