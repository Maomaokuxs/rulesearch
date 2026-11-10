package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 首页：搜索框居中，下方横滑历史 chips。点搜索框进独立搜索页。 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun HomeView(
    vm: SearchViewModel,
    onSearchBoxClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
) {
    fun go(kw: String) {
        vm.keyword = kw
        vm.search()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "RuleSearch",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 38.sp,
        )
        // 静态搜索框：只做展示，点开独立搜索页（下拉已删，不再被盖）
        SearchBoxStatic(
            displayText = "",
            onClick = onSearchBoxClick,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            boxH = 56.dp,
            showSearchButton = true,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
        // 历史搜索：下拉已删，胶囊不再被盖
        if (vm.history.isNotEmpty()) {
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
                    .background(cardBackground())
                    .combinedClickable(
                        onClick = {
                            if (editMode) onExitEdit() else onSearch(h)
                        },
                        onLongClick = { if (!editMode) onEnterEdit() },
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
