package dev.rulesearch.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import dev.rulesearch.app.PAN_BRAND_DEFS
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel
import dev.rulesearch.app.SortMode
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索页（底部 dock 的"搜索"标签）：首页居中搜索框，搜后跳转结果页。 */
@Composable
fun SearchTab(
    vm: SearchViewModel,
    initialKeyword: String? = null,
    autoOpenDialog: Boolean = false,
) {
    val context = LocalContext.current
    var selectedResult by remember { mutableStateOf<SearchResultUi?>(null) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    // Deep link 带关键词进来：自动执行一次搜索
    LaunchedEffect(initialKeyword) {
        if (!initialKeyword.isNullOrBlank()) {
            vm.keyword = initialKeyword
            vm.search()
        }
    }

    // 调试：搜出结果后自动弹出第一条的详情弹窗（验证用）
    LaunchedEffect(vm.results, autoOpenDialog) {
        if (autoOpenDialog && vm.results.isNotEmpty() && selectedResult == null) {
            selectedResult = vm.currentTabResults.firstOrNull()
        }
    }

    // 结果页按返回：回到首页（清空结果），而不是直接退出应用
    //（弹窗打开时返回键由 Dialog 自己消费；搜索中按返回会取消搜索）
    BackHandler(enabled = vm.searched) {
        vm.clearResults()
    }

    // 首页 ↔ 结果页切换：淡入淡出
    AnimatedContent(
        targetState = vm.searched,
        transitionSpec = {
            fadeIn(tween(280)).togetherWith(fadeOut(tween(240)))
        },
        label = "home_results",
    ) { searched ->
        if (!searched) {
            // ===== 首页：搜索框居中 =====
            HomeView(vm = vm, initialKeyword = initialKeyword)
        } else {
            // ===== 结果页 =====
            ResultsView(
                vm = vm,
                initialKeyword = initialKeyword,
                onResultClick = {
                    vm.addFootprint(it)
                    selectedResult = it
                },
            )
        }
    }

    // 点卡片后的弹窗：识别网盘类型 + 检测链接有效性
    selectedResult?.let { r ->
        ResultDialog(result = r, onDismiss = { selectedResult = null }, vm = vm)
    }
}

/** 首页：搜索框居中。 */
@Composable
private fun HomeView(vm: SearchViewModel, initialKeyword: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "RuleSearch",
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
        )
        Text(
            text = "规则引擎 · 网盘磁力搜索",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )
        SearchInput(vm = vm, initialKeyword = initialKeyword)
    }
}

/** 结果页。 */
@Composable
private fun ResultsView(
    vm: SearchViewModel,
    initialKeyword: String?,
    onResultClick: (SearchResultUi) -> Unit,
) {
    var showFilterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchInput(
            vm = vm,
            // 结果页的搜索框带入当前关键词
            initialKeyword = vm.keyword.ifBlank { initialKeyword },
            modifier = Modifier.padding(top = 8.dp),
        )

        when {
            vm.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            vm.error != null -> {
                Text("出错：${vm.error}")
            }
            vm.results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "没有搜到相关资源，换个关键词试试",
                        color = MiuixTheme.colorScheme.secondary,
                    )
                }
            }
            else -> {
                TabRow(
                    tabs = vm.tabTitles,
                    selectedTabIndex = vm.selectedTab,
                    onTabSelected = vm::selectTab,
                )
                // 双列卡片网格：卡片宽高由屏幕决定，不随标题长短变化
                // 左下角圆形工具按钮：展开后弹出筛选/多选
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(vm.currentTabResults, key = { it.url }) { r ->
                            ResultCard(
                                r = r,
                                selectionMode = vm.selectionMode,
                                selected = r.url in vm.selectedUrls,
                                onClick = {
                                    if (vm.selectionMode) vm.toggleSelect(r.url)
                                    else onResultClick(r)
                                },
                            )
                        }
                    }
                    ToolsFab(
                        vm = vm,
                        onFilterClick = { showFilterDialog = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    )
                }
            }
        }

        // 多选操作条
        if (vm.selectionMode) {
            SelectionBar(vm = vm)
        }
    }

    // 筛选框：排序方式 + 网盘类型过滤 + 链接状态
    if (showFilterDialog) {
        Dialog(onDismissRequest = { showFilterDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                insideMargin = PaddingValues(8.dp),
            ) {
                Text(
                    text = "筛选",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                // 内容过多时可上下滚动，避免被截断
                Column(
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                Text(
                    text = "排序方式",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                SortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.sortMode = mode
                                showFilterDialog = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 15.sp,
                            color = if (vm.sortMode == mode) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
                if (vm.tabPanBrands.isNotEmpty()) {
                    Text(
                        text = "网盘类型（取消勾选即过滤掉）",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    vm.tabPanBrands.forEach { brand ->
                        val hidden = brand in vm.hiddenPanBrands
                        val label = PAN_BRAND_DEFS.firstOrNull { it.first == brand }?.second ?: brand
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.togglePanBrandHidden(brand) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                state = ToggleableState(!hidden),
                                onClick = { vm.togglePanBrandHidden(brand) },
                            )
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "链接状态",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setOnlyReachableFilter(!vm.onlyReachable) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        state = ToggleableState(vm.onlyReachable),
                        onClick = { vm.setOnlyReachableFilter(!vm.onlyReachable) },
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "只显示可访问的链接",
                            fontSize = 15.sp,
                        )
                        val prog = vm.checkProgress
                        if (vm.checkingLinks && prog != null) {
                            Text(
                                text = "检测中 ${prog.first}/${prog.second}…",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.secondary,
                            )
                        } else if (vm.checkingLinks) {
                            Text(
                                text = "检测中…",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                }
                Button(
                    onClick = { showFilterDialog = false },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("完成")
                }
            }
        }
    }
}

/** 右下角圆形工具按钮：点开展开筛选/多选，再点收起。 */
@Composable
private fun ToolsFab(
    vm: SearchViewModel,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FabAction(
                    icon = Icons.Filled.List,
                    label = "筛选",
                    onClick = {
                        expanded = false
                        onFilterClick()
                    },
                )
                FabAction(
                    icon = if (vm.selectionMode) Icons.Filled.Close else Icons.Filled.Check,
                    label = if (vm.selectionMode) "取消多选" else "多选",
                    onClick = {
                        expanded = false
                        vm.toggleSelectionMode()
                    },
                )
            }
        }
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.MoreVert,
                contentDescription = "工具",
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** FAB 展开后的带文字小按钮（右下角：文字在左，图标在右）。 */
@Composable
private fun FabAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text = label, fontSize = 14.sp)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
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
        android.widget.Toast.makeText(context, "已复制 $count 条链接", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun shareSelected() {
        val text = vm.selectedResults().joinToString("\n\n") { "${it.title}\n${it.url}" }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "分享链接"))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
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
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp).padding(end = 4.dp),
            )
            Text("分享")
        }
    }
}

/** 搜索输入框：聚焦时下拉展示历史联想 + 在线实时建议（浏览器式）。 */
@Composable
private fun SearchInput(
    vm: SearchViewModel,
    initialKeyword: String? = null,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var text by remember(initialKeyword) { mutableStateOf(initialKeyword.orEmpty()) }
    var focused by remember { mutableStateOf(false) }
    val history = remember(text, focused, vm.history) {
        if (focused) vm.suggestionsFor(text) else emptyList()
    }
    var online by remember { mutableStateOf<List<String>>(emptyList()) }

    fun doSearch(kw: String) {
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

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = "搜索资源",
                useLabelAsPlaceholder = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .onFocusChanged { focused = it.isFocused },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { doSearch(text) },
                ),
            )
            // 搜索按钮改成图标：圆形，和输入框同高
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary)
                    .clickable { doSearch(text) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // 联想下拉：历史在前（可单条删除），在线建议在后
        if (focused && (history.isNotEmpty() || online.isNotEmpty())) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                history.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doSearch(s) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
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
                if (history.isNotEmpty() && online.isNotEmpty()) {
                    Text(
                        text = "在线建议",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                online.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doSearch(s) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
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
                if (history.isNotEmpty()) {
                    TextButton(
                        text = "清空历史",
                        onClick = { vm.clearHistory() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    r: SearchResultUi,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    // Miuix Card 默认 insideMargin 为 0，文字会贴边被 16dp 圆角裁掉，必须显式给内边距
    Card(
        onClick = onClick,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Box 撑满卡片宽度，勾选圆圈才能固定在右上角，不跟标题长短跑
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                // 多选时标题右侧给勾选圆圈留位置，不被压住
                modifier = Modifier.padding(end = if (selectionMode) 28.dp else 0.dp),
            ) {
                Text(
                    text = r.title,
                    minLines = 2, // 固定两行：卡片高度统一
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                // 左下角显示文件大小（无大小时显示时间，都没有则留空行保持高度统一）
                val meta = r.size.ifEmpty { r.time }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = meta,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.secondary,
                        minLines = 1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // 多选模式：右上角勾选状态
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.surfaceContainerHighest,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
