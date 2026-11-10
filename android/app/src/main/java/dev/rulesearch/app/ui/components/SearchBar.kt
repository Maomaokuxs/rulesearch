package dev.rulesearch.app.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索输入框（独立搜索页专用，48dp）。
 * 下拉已删（2026-11-10）：历史/建议改走独立搜索页，本文件只剩纯输入框，
 * 不再有 Popup/自定义 layout/高度随键盘调整/zIndex 等悬浮逻辑。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
    val boxH = 48.dp
    val btnSize = 38.dp
    val iconSize = 20.dp
    val searchBoxState = with(sharedTransitionScope) {
        rememberSharedContentState(key = "searchBox")
    }
    // 只对装 TextField 的这个 Box 做共享过渡（搜索页 ↔ 结果页同 key、同 48dp）。
    // sharedBounds + RemeasureToBounds：按动画尺寸逐帧重测量，文字全程不缩放。
    // 门控已删（2026-11-10）：sharedBounds 必须无条件挂上——退出侧内容按上一帧布局
    // 参与过渡，门控 false 时退出侧布局里根本没有该元素，第一次过渡只剩淡入淡出、
    // 看起来"过快"，且 search() 里按新旧关键词重算门控会把搜索页→结果页的过渡也掐掉。
    Box(
        modifier = modifier.fillMaxWidth().height(boxH).then(
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = searchBoxState,
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(tween(durationMillis = 300)),
                    exit = fadeOut(tween(durationMillis = 300)),
                    boundsTransform = { _, _ -> tween(durationMillis = 300) },
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                )
            }
        )
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            label = "搜索资源",
            useLabelAsPlaceholder = true,
            modifier = Modifier.fillMaxWidth().height(boxH)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(text) }),
            textStyle = MiuixTheme.textStyles.main.copy(fontSize = 14.sp),
            insideMargin = DpSize(16.dp, 8.dp),
            trailingIcon = { Spacer(Modifier.size(btnSize)) },
        )
        // 蓝色圆形搜索按钮：手动 overlay 在右侧
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 5.dp)
                .size(btnSize)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary)
                .clickable { onSearch(text) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "搜索",
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/**
 * 静态搜索框（首页 56dp / 结果页 48dp）：只做展示，点开独立搜索页。
 * 外观与 Miuix TextField 一致（16dp 圆角、secondaryContainer 底），
 * 但不是输入框——不聚焦、不弹键盘，从根上没有下拉可盖的东西。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchBoxStatic(
    displayText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    boxH: Dp = 56.dp,
    showSearchButton: Boolean = false,
    showFunnel: Boolean = false,
    funnelActive: Boolean = false,
    onFunnelClick: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
) {
    val btnSize = if (boxH >= 56.dp) 46.dp else 38.dp
    val iconSize = if (boxH >= 56.dp) 24.dp else 20.dp
    val fontSize = if (boxH >= 56.dp) 15.sp else 14.sp
    val searchBoxState = with(sharedTransitionScope) {
        rememberSharedContentState(key = "searchBox")
    }
    // 共享过渡无条件挂上（同 SearchInput 的门控说明）；1dp 描边让首页框边缘一眼可见：
    // onSurface 25% alpha，浅色下是深灰、深色下是浅灰，两边都看得清。
    Box(
        modifier = modifier.fillMaxWidth().height(boxH).then(
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = searchBoxState,
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(tween(durationMillis = 300)),
                    exit = fadeOut(tween(durationMillis = 300)),
                    boundsTransform = { _, _ -> tween(durationMillis = 300) },
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                )
            }
        )
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(boxH)
                .padding(start = 16.dp, end = if (showFunnel) 6.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayText.ifBlank { "搜索资源" },
                fontSize = fontSize,
                color = if (displayText.isBlank()) MiuixTheme.colorScheme.onSecondaryContainer
                else MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showSearchButton) {
                Box(
                    modifier = Modifier.size(btnSize)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "搜索",
                        tint = MiuixTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(iconSize),
                    )
                }
            } else if (showFunnel) {
                // 漏斗自己消费点击（开筛选），不触发外层 onClick（进搜索页）
                Box(
                    modifier = Modifier.size(32.dp)
                        .clickable { onFunnelClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    FunnelIcon(
                        tint = if (funnelActive) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
