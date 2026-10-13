package dev.rulesearch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.FavoriteUi
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 收藏页（底部 dock 的"收藏"标签）。 */
@Composable
fun FavoritesTab(vm: SearchViewModel) {
    var opened by remember { mutableStateOf<FavoriteUi?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(pageBackground()),
    ) {
        Text(
            text = "收藏",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 64.dp, bottom = 16.dp),
        )
        if (vm.favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有收藏，在搜索结果里点「收藏」即可保存",
                    color = MiuixTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            ) {
                item {
                    // 白色圆角卡片内复用结果行
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp))
                            .background(cardBackground()),
                    ) {
                        vm.favorites.forEachIndexed { i, f ->
                            val asResult = SearchResultUi(
                                title = f.title,
                                url = f.url,
                                size = f.size,
                                time = f.time,
                                source = f.source,
                                kind = f.kind,
                            )
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
                                        fontSize = 17.sp,
                                        lineHeight = 22.sp,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                    Box(modifier = Modifier.padding(top = 6.dp)) {
                                        ResultMetaLine(asResult)
                                    }
                                }
                                // 右侧蓝色星标：点按取消收藏
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .clickable { vm.removeFavorite(f.url) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "★",
                                        fontSize = 20.sp,
                                        color = MiuixTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (i < vm.favorites.size - 1) ResultDivider()
                        }
                    }
                }
            }
        }
    }

    // 点收藏条目：同样走详情弹层（可复制/打开/取消收藏）
    opened?.let { f ->
        ResultSheet(
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
