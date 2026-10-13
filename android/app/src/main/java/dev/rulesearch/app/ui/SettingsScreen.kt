package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 内容过滤页（"我的"里的二级页面）：新增 + 列表 + 删除。 */
@Composable
fun BlocklistPage(vm: SearchViewModel) {
    var input by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    // 多选删除模式：长按列表项进入
    var selectionMode by remember { mutableStateOf(false) }
    var selectedWords by remember { mutableStateOf<Set<String>>(emptySet()) }
    fun exitSelection() {
        selectionMode = false
        selectedWords = emptySet()
    }
    // 多选模式下按返回先退出多选，不直接回上级菜单
    BackHandler(enabled = selectionMode) { exitSelection() }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectionMode) "已选 ${selectedWords.size} 项" else "内容过滤",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    TextButton(
                        text = "删除",
                        onClick = {
                            vm.removeBlockedWords(selectedWords)
                            exitSelection()
                        },
                    )
                    TextButton(
                        text = "取消",
                        onClick = { exitSelection() },
                    )
                } else if (vm.blockedWords.isNotEmpty()) {
                    TextButton(
                        text = "清空",
                        onClick = { showClearConfirm = true },
                    )
                }
            }
        }
        Text(
            text = "标题含有以下词语的结果会被过滤，不区分大小写",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.secondary,
        )
        Text(
            text = "累计已过滤 ${vm.totalFiltered} 条结果",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                label = "新增过滤词",
                useLabelAsPlaceholder = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Button(
                onClick = {
                    vm.addBlockedWord(input)
                    input = ""
                },
            ) {
                Text("添加")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(vm.blockedWords, key = { it }) { w ->
                val selected = w in selectedWords
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    selectedWords =
                                        if (selected) selectedWords - w
                                        else selectedWords + w
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    selectedWords = setOf(w)
                                }
                            },
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = w,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 勾选框放尾部；行高固定 48dp，两种模式纵向一致
                    if (selectionMode) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(20.dp)
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
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    } else {
                        TextButton(
                            text = "删除",
                            onClick = { vm.removeBlockedWord(w) },
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        Dialog(onDismissRequest = { showClearConfirm = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                insideMargin = PaddingValues(20.dp),
            ) {
                Text(
                    text = "清空确认",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = "确定清空全部自建过滤词吗？内置词表不受影响。",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(
                        text = "取消",
                        onClick = { showClearConfirm = false },
                    )
                    Button(
                        onClick = {
                            vm.clearBlockedWords()
                            showClearConfirm = false
                        },
                    ) {
                        Text("清空")
                    }
                }
            }
        }
    }
}
