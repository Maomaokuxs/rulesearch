package dev.rulesearch.app.ui

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
import top.yukonga.miuix.kmp.basic.Card
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

/** 足迹页（底部 dock 的"足迹"标签）：点开过的结果，按时间倒序。 */
@Composable
fun FootprintsTab(vm: SearchViewModel) {
    var opened by remember { mutableStateOf<FootprintUi?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
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
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(vm.footprints, key = { it.url }) { f ->
                    Card(
                        onClick = { opened = f },
                        insideMargin = PaddingValues(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = f.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                )
                                val t = formatFootprintTime(f.clickedAt)
                                val sub = if (t.isNotEmpty()) "${f.source} · $t" else f.source
                                Text(
                                    text = sub,
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            TextButton(
                                text = "删除",
                                onClick = { vm.removeFootprint(f.url) },
                            )
                        }
                    }
                }
            }
        }
    }

    // 点足迹条目：走详情弹窗（可复制/打开/收藏），不再重复记录
    opened?.let { f ->
        ResultDialog(
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
