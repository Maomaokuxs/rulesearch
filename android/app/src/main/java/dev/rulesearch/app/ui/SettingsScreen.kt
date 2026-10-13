package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
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
    val userWords = vm.blockedWords
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectionMode) "已选 ${selectedWords.size} 项" else "内容过滤",
                fontWeight = FontWeight.ExtraBold,
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
                }
            }
        }
        Text(
            text = "标题含有以下词语的结果会被过滤，不区分大小写。",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        // 统计卡片：漏斗图标 + 大数字 + 说明
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val funnelColor = MiuixTheme.colorScheme.primary
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val c = funnelColor
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.1f, h * 0.15f)
                            lineTo(w * 0.9f, h * 0.15f)
                            lineTo(w * 0.62f, h * 0.52f)
                            lineTo(w * 0.62f, h * 0.88f)
                            lineTo(w * 0.38f, h * 0.88f)
                            lineTo(w * 0.38f, h * 0.52f)
                            close()
                        }
                        drawPath(path, c)
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "${vm.totalFiltered}条",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "累计已过滤结果",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.secondary,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                label = "新增过滤词",
                useLabelAsPlaceholder = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable {
                        vm.addBlockedWord(input)
                        input = ""
                    },
            ) {
                Text(
                    text = "添加",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 自建词语：可删除
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "我的过滤词 · ${userWords.size} 个",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.secondary,
                    )
                    if (userWords.isNotEmpty()) {
                        Text(
                            text = "清空",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                                .clickable { showClearConfirm = true },
                        )
                    }
                }
            }
            if (userWords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBackground())
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无过滤词，添加后生效",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.secondary,
                        )
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBackground())
                            .padding(vertical = 4.dp),
                    ) {
                        Column {
                            userWords.forEachIndexed { i, w ->
                                val selected = w in selectedWords
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
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
                                        )
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = w,
                                        fontSize = 17.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    // 勾选框放尾部；多选/正常两种模式行高一致
                                    if (selectionMode) {
                                        Box(
                                            modifier = Modifier
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
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(cardGray())
                                                .padding(0.dp)
                                                .clickable { vm.removeBlockedWord(w) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "×",
                                                fontSize = 16.sp,
                                                color = MiuixTheme.colorScheme.secondary,
                                            )
                                        }
                                    }
                                }
                                if (i < userWords.size - 1) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(1.dp)
                                            .background(MiuixTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                    )
                                }
                            }
                        }
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
