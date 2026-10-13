package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.rulesearch.app.FootprintUi
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun isToday(ts: Long): Boolean {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR)
    val year = cal.get(Calendar.YEAR)
    cal.timeInMillis = ts
    return cal.get(Calendar.DAY_OF_YEAR) == today && cal.get(Calendar.YEAR) == year
}

private fun isYesterday(ts: Long): Boolean {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yDay = cal.get(Calendar.DAY_OF_YEAR)
    val yYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = ts
    return cal.get(Calendar.DAY_OF_YEAR) == yDay && cal.get(Calendar.YEAR) == yYear
}

private fun formatFootprintFullDate(ts: Long): String {
    if (ts <= 0) return ""
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ts))
    } catch (e: Exception) {
        ""
    }
}

/** 足迹页（"我的"里的二级页面）：点开过的结果，按日期分组，长按多选删除。 */
@Composable
fun FootprintsTab(vm: SearchViewModel) {
    var opened by remember { mutableStateOf<FootprintUi?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedUrls by remember { mutableStateOf(setOf<String>()) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 按日期分组：今天 / 昨天 / 更早的按具体日期
    val today = remember(vm.footprints) { vm.footprints.filter { isToday(it.clickedAt) } }
    val yesterday = remember(vm.footprints) { vm.footprints.filter { isYesterday(it.clickedAt) } }
    val earlierByDate = remember(vm.footprints) {
        vm.footprints.filter { !isToday(it.clickedAt) && !isYesterday(it.clickedAt) }
            .groupBy { formatFootprintFullDate(it.clickedAt) }
            .toList()
            .sortedByDescending { it.first }
    }
    val groups = remember(today, yesterday, earlierByDate) {
        buildList {
            if (today.isNotEmpty()) add("今天" to today)
            if (yesterday.isNotEmpty()) add("昨天" to yesterday)
            earlierByDate.forEach { (date, items) ->
                if (date.isNotEmpty() && items.isNotEmpty()) add(date to items)
            }
        }
    }

    fun exitSelection() {
        selectionMode = false
        selectedUrls = emptySet()
    }

    // 多选模式下按返回：退出多选，不退出页面
    BackHandler(enabled = selectionMode) { exitSelection() }

    fun deleteSelected() {
        for (url in selectedUrls) vm.removeFootprint(url)
        exitSelection()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题行：多选模式下显示"已选 N 项"
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 24.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectionMode) "已选 ${selectedUrls.size} 项" else "足迹",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
            )
            if (selectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        text = "删除",
                        onClick = { deleteSelected() },
                    )
                    TextButton(
                        text = "取消",
                        onClick = { exitSelection() },
                    )
                }
            }
        }

        if (vm.footprints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有足迹，点开搜索结果会自动记录",
                    color = MiuixTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                groups.forEach { (groupName, items) ->
                    item(key = "group_$groupName") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 日期分组头：14sp 次级灰
                            Text(
                                text = groupName,
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.secondary,
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBackground()),
                            ) {
                            items.forEachIndexed { index, f ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                                            .height(0.5.dp)
                                            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    )
                                }
                                val selected = f.url in selectedUrls
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (selectionMode) {
                                                    selectedUrls = if (selected) selectedUrls - f.url
                                                    else selectedUrls + f.url
                                                } else {
                                                    opened = f
                                                }
                                            },
                                            onLongClick = {
                                                if (!selectionMode) {
                                                    selectionMode = true
                                                    selectedUrls = setOf(f.url)
                                                }
                                            },
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // 多选模式：复选框
                                    if (selectionMode) {
                                        Box(
                                            modifier = Modifier.size(22.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (selected) MiuixTheme.colorScheme.primary
                                                    else Color.Transparent,
                                                )
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (selected) {
                                                Text(
                                                    text = "✓",
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            MiuixTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                        ),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = f.title,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 17.sp,
                                            lineHeight = 22.sp,
                                        )
                                        Row(
                                            modifier = Modifier.padding(top = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (f.source.isNotEmpty()) {
                                                Text(
                                                    text = f.source,
                                                    fontSize = 12.sp,
                                                    color = MiuixTheme.colorScheme.secondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            }
                        }
                    }
                }

                // 底部"清空足迹"红色按钮
                item(key = "clear_all") {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBackground())
                            .clickable { showClearConfirm = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "清空足迹",
                            fontSize = 15.sp,
                            color = Color(0xFFE53935),
                        )
                    }
                }
            }
        }
    }

    // 清空确认弹窗
    if (showClearConfirm) {
        Dialog(onDismissRequest = { showClearConfirm = false }) {
            Column(
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(cardBackground())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "清空全部足迹？", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "此操作不可恢复",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            .clickable { showClearConfirm = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "取消", fontSize = 15.sp)
                    }
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE53935))
                            .clickable {
                                vm.clearFootprints()
                                exitSelection()
                                showClearConfirm = false
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "清空", fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // 点足迹条目：走详情弹层（可复制/打开/收藏），不再重复记录
    opened?.let { f ->
        ResultSheet(
            result = SearchResultUi(
                title = f.title,
                url = f.url,
                size = "",
                time = "",
                source = f.source,
                kind = f.kind,
            ),
            onDismiss = { opened = null },
            vm = vm,
        )
    }
}
