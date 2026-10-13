package dev.rulesearch.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.FootprintUi
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatFootprintTime(ts: Long): String {
    if (ts <= 0) return ""
    return try {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
    } catch (e: Exception) {
        ""
    }
}

/** 足迹页（"我的"里的二级页面）：点开过的结果，按时间倒序。 */
@Composable
fun FootprintsTab(vm: SearchViewModel) {
    var opened by remember { mutableStateOf<FootprintUi?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "足迹",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            )
            if (vm.footprints.isNotEmpty()) {
                TextButton(
                    text = "清空",
                    onClick = { vm.clearFootprints() },
                )
            }
        }
        if (vm.footprints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(vm.footprints, key = { it.url }) { f ->
                    val asResult = SearchResultUi(
                        title = f.title,
                        url = f.url,
                        size = "",
                        time = formatFootprintTime(f.clickedAt),
                        source = f.source,
                        kind = f.kind,
                    )
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { opened = f }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = f.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                )
                                Box(modifier = Modifier.padding(top = 6.dp)) {
                                    ResultMetaLine(asResult)
                                }
                            }
                            TextButton(
                                text = "删除",
                                onClick = { vm.removeFootprint(f.url) },
                            )
                        }
                        ResultDivider()
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
