package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.rulesearch.app.SearchViewModel
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索输入框：聚焦时下拉展示历史联想 + 在线实时建议（浏览器式）。 */
@Composable
internal fun SearchInput(
    vm: SearchViewModel,
    initialKeyword: String? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showFunnel: Boolean = false,
    funnelActive: Boolean = false,
    onFunnelClick: () -> Unit = {},
    showDropdown: Boolean,
    onShowDropdownChange: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var text by remember(initialKeyword) { mutableStateOf(initialKeyword.orEmpty()) }
    var focused by remember { mutableStateOf(false) }
    val history = remember(text, showDropdown, vm.history) {
        if (showDropdown) vm.suggestionsFor(text) else emptyList()
    }
    var online by remember { mutableStateOf<List<String>>(emptyList()) }

    // 返回键：聚焦中先取消聚焦（收起下拉），再按才走外层返回逻辑
    BackHandler(enabled = showDropdown || focused) {
        onShowDropdownChange(false)
        focusManager.clearFocus()
    }

    // 键盘从显示变为隐藏时（用户按了一次返回收键盘），同步清聚焦+隐藏下拉
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var wasImeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeBottom) {
        val isVisible = imeBottom > 0
        if (wasImeVisible && !isVisible) {
            onShowDropdownChange(false)
            focusManager.clearFocus()
        }
        wasImeVisible = isVisible
    }

    fun doSearch(kw: String) {
        onShowDropdownChange(false)
        focusManager.clearFocus()
        vm.keyword = kw
        vm.search()
    }

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
        online = vm.onlineSuggestions(latest)
            .filter { s -> history.none { it.equals(s, ignoreCase = true) } }
    }

    val boxH = if (compact) 48.dp else 56.dp
    val btnSize = if (compact) 38.dp else 46.dp
    val iconSize = if (compact) 20.dp else 24.dp
    val inputFontSize = if (compact) 14.sp else 15.sp

    Column(modifier = modifier.zIndex(10f)) {
        Box(modifier = Modifier.height(boxH)) {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = "搜索资源",
                useLabelAsPlaceholder = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boxH)
                    .onFocusChanged { focused = it.isFocused; if (it.isFocused) onShowDropdownChange(true) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { doSearch(text) },
                ),
                textStyle = MiuixTheme.textStyles.main.copy(fontSize = inputFontSize),
                insideMargin = DpSize(16.dp, if (compact) 8.dp else 16.dp),
                // trailingIcon placeholder only; real button overlaid manually
                trailingIcon = {
                    val s = if (!compact) btnSize else if (showFunnel) 32.dp else 0.dp
                    if (s > 0.dp) Spacer(Modifier.size(s))
                },
            )

            // trailing action: manual overlay, vertically centered
            if (!compact) {
                // search button: blue circle, 5dp end margin
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp)
                        .size(btnSize)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary)
                        .clickable { doSearch(text) },
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
                // funnel filter icon inside the box, replacing the search button
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .size(32.dp)
                        .clickable { onFunnelClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    FunnelIcon(
                        tint = if (funnelActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // 联想下拉：输入为空只显示历史，有字只显示在线建议
        val showHistory = text.isBlank() && history.isNotEmpty()
        val showOnline = text.isNotBlank() && online.isNotEmpty()
        if (showDropdown && (showHistory || showOnline)) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                // 消费点击：点下拉内部不冒泡到外层空白点击
                modifier = Modifier.fillMaxWidth().clickable(enabled = false, onClick = {}),
                insideMargin = PaddingValues(0.dp),
            ) {
                // 下拉内容可滚动，最多 600dp（至少显示 3 行）
                Column(
                    modifier = Modifier
                        .heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                if (showHistory) history.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doSearch(s) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = s,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "删除这条历史",
                            tint = MiuixTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { vm.deleteHistoryItem(s) }
                                .padding(2.dp),
                        )
                    }
                }
                if (showOnline) online.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doSearch(s) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = s,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 28.dp),
                        )
                    }
                }
                }

            }
        }
    }
}
