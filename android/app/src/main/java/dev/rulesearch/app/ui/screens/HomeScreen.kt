package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.zIndex
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 首页：搜索框居中，下方横滑历史 chips。 */
@Composable
internal fun HomeView(vm: SearchViewModel, initialKeyword: String?) {
    fun go(kw: String) {
        vm.keyword = kw
        vm.search()
    }

    val focusManager = LocalFocusManager.current
    var showDropdown by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 220.dp)
            // 点空白处隐藏下拉
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showDropdown = false
                focusManager.clearFocus()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "RuleSearch",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 38.sp,
        )
        SearchInput(
            vm = vm,
            initialKeyword = initialKeyword,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            showDropdown = showDropdown,
            onShowDropdownChange = { showDropdown = it },
        )
        // 历史搜索：多行胶囊，长按进入编辑（右上角叉删除），返回键退出编辑
        if (vm.history.isNotEmpty() && !showDropdown) {
            var histEdit by remember { mutableStateOf(false) }
            BackHandler(enabled = histEdit) { histEdit = false }
            HistoryFlow(
                history = vm.history,
                editMode = histEdit,
                onEnterEdit = { histEdit = true },
                onExitEdit = { histEdit = false },
                onSearch = { go(it) },
                onDelete = { vm.deleteHistoryItem(it) },
            )
        }
    }
}


/** 历史记录多行胶囊：长按进入编辑模式，右上角叉删除。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryFlow(
    history: List<String>,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        history.forEach { h ->
            Box(
                modifier = Modifier.clip(CircleShape)
                    .background(cardGray())
                    .combinedClickable(
                        onClick = {
                            if (editMode) onExitEdit() else onSearch(h)
                        },
                        onLongClick = { if (!editMode) onEnterEdit() },
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = h,
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // 编辑模式：右上角删除叉
                if (editMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-10).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.error)
                            .clickable { onDelete(h) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "×",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onError,
                        )
                    }
                }
            }
        }
    }
}
