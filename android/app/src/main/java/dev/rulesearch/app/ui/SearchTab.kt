package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel

/** 搜索页（底部 dock 的"搜索"标签）：首页居中搜索框，搜后进结果页。 */
@Composable
fun SearchTab(
    vm: SearchViewModel,
    initialKeyword: String? = null,
    autoOpenDialog: Boolean = false,
) {
    var selectedResult by remember { mutableStateOf<SearchResultUi?>(null) }

    // Deep link 带关键词进来：自动执行一次搜索
    LaunchedEffect(initialKeyword) {
        if (!initialKeyword.isNullOrBlank()) {
            vm.keyword = initialKeyword
            vm.search()
        }
    }

    // 调试：搜出结果后自动弹出第一条的详情（验证用）
    LaunchedEffect(vm.results, autoOpenDialog) {
        if (autoOpenDialog && vm.results.isNotEmpty() && selectedResult == null) {
            selectedResult = vm.currentTabResults.firstOrNull()
        }
    }

    // 结果页按返回：回到首页（清空结果），而不是直接退出应用
    //（弹层打开时返回键由 bottom sheet 自己消费；搜索中按返回会取消搜索）
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
            HomeView(vm = vm, initialKeyword = initialKeyword)
        } else {
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

    // 点结果行后的底部详情弹层
    selectedResult?.let { r ->
        ResultSheet(result = r, onDismiss = { selectedResult = null }, vm = vm)
    }
}
