package dev.rulesearch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.rulesearch.app.FavoriteUi
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 收藏页（底部 dock 的"收藏"标签）。 */
@Composable
fun FavoritesTab(vm: SearchViewModel) {
    var opened by remember { mutableStateOf<FavoriteUi?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "收藏",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )
        if (vm.favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有收藏，在搜索结果里点「收藏」即可保存",
                    color = MiuixTheme.colorScheme.secondary,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(vm.favorites, key = { it.url }) { f ->
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
                                val meta = f.size.ifEmpty { f.time }
                                val sub = if (meta.isNotEmpty()) "${f.source} · $meta" else f.source
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
                                onClick = { vm.removeFavorite(f.url) },
                            )
                        }
                    }
                }
            }
        }
    }

    // 点收藏条目：同样走详情弹窗（可复制/打开/取消收藏）
    opened?.let { f ->
        ResultDialog(
            result = SearchResultUi(
                title = f.title,
                url = f.url,
                size = f.size,
                time = f.time,
                source = f.source,
                kind = f.kind,
            ),
            onDismiss = { opened = null },
            vm = vm,
        )
    }
}
