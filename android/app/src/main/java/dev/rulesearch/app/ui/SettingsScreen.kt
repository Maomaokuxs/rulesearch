package dev.rulesearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 设置页（底部 dock 的"设置"标签）：二级菜单，子页返回键回到菜单。 */
@Composable
fun SettingsTab(vm: SearchViewModel) {
    var page by remember { mutableStateOf<String?>(null) } // null=菜单, "rules"=搜索源, "blocklist"=过滤词

    BackHandler(enabled = page != null) { page = null }

    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(180))) },
        label = "settings_nav",
    ) { p ->
        when (p) {
            "rules" -> SettingsSubPage(onBack = { page = null }) { ManageTab(vm = vm) }
            "blocklist" -> SettingsSubPage(onBack = { page = null }) { BlocklistPage(vm = vm) }
            else -> SettingsMenu(
                vm = vm,
                onRules = { page = "rules" },
                onBlocklist = { page = "blocklist" },
            )
        }
    }
}

/** 设置一级菜单。 */
@Composable
private fun SettingsMenu(
    vm: SearchViewModel,
    onRules: () -> Unit,
    onBlocklist: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "设置",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Card(
            onClick = onRules,
            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            SettingsMenuRow(
                title = "搜索源设置",
                subtitle = "${vm.rules.size} 条规则，可开关、导入、检测、更新",
            )
        }
        Card(
            onClick = onBlocklist,
            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            SettingsMenuRow(
                title = "内容过滤",
                subtitle = "${vm.blockedWords.size} 个过滤词 · 累计已过滤 ${vm.totalFiltered} 条",
            )
        }
    }
}

@Composable
private fun SettingsMenuRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = "〉",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.secondary,
        )
    }
}

/** 设置二级页面：顶部返回行 + 内容。 */
@Composable
private fun SettingsSubPage(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "〈 返回",
                color = MiuixTheme.colorScheme.primary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            content()
        }
    }
}

/** 内容过滤整页版：新增 + 列表 + 删除。 */
@Composable
private fun BlocklistPage(vm: SearchViewModel) {
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
