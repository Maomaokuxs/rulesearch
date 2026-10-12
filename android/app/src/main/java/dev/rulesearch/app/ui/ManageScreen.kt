package dev.rulesearch.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.rulesearch.app.ImportResult
import dev.rulesearch.app.RuleHealth
import dev.rulesearch.app.RuleTestResult
import dev.rulesearch.app.RuleUi
import dev.rulesearch.app.RuleUpdate
import dev.rulesearch.app.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索源设置页（设置里的二级页面）：开关规则 + 导入/导出/测试/更新。 */
@Composable
fun ManageTab(vm: SearchViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var testingRule by remember { mutableStateOf<RuleUi?>(null) }
    var updates by remember { mutableStateOf<List<RuleUpdate>?>(null) }
    var checkingUpdates by remember { mutableStateOf(false) }
    // 一键检测里点"异常"徽章：看失败原因（规则名, 健康状态）
    var healthError by remember { mutableStateOf<Pair<String, RuleHealth>?>(null) }

    fun toast(msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // 导入：选 YAML 文件 → 校验 → 写入用户规则目录
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                } catch (e: Exception) {
                    null
                }
            }
            if (text.isNullOrBlank()) {
                toast("文件读取失败")
                return@launch
            }
            val res: ImportResult = vm.importRule(queryDisplayName(context, uri), text)
            toast(if (res.ok) "已导入规则：${res.name}" else "导入失败：${res.error}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "搜索源",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        // 右下角圆形工具按钮：导入 / 检测全部 / 检查更新
        val rp = vm.ruleTestProgress
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {

        if (vm.rules.isEmpty()) {
            Text(
                "搜索源加载中…",
                color = MiuixTheme.colorScheme.secondary,
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                items(vm.rules, key = { it.name }) { rule ->
                    RuleCard(
                        rule = rule,
                        enabled = vm.isRuleEnabled(rule.name),
                        health = vm.ruleHealth[rule.name],
                        onToggle = { vm.setRuleEnabled(rule.name, it) },
                        onTest = { testingRule = rule },
                        onExport = {
                            scope.launch {
                                val yaml = vm.getRuleYaml(rule.name)
                                if (yaml.isBlank()) {
                                    toast("导出失败")
                                    return@launch
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, yaml)
                                    putExtra(Intent.EXTRA_SUBJECT, "${rule.name}.yaml")
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "导出规则"),
                                )
                            }
                        },
                        onDelete = {
                            scope.launch {
                                val ok = vm.deleteUserRule(rule.name)
                                toast(if (ok) "已删除规则：${rule.name}" else "删除失败")
                            }
                        },
                        onHealthClick = { healthError = rule.name to it },
                    )
                }
            }
            Text(
                "关闭后搜索时跳过该源，可随时再打开",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.secondary,
            )
            }
            // 右下角圆形工具按钮：导入 / 检测全部 / 检查更新
            ManageFab(
                onImport = { picker.launch("*/*") },
                testLabel = when {
                    vm.testingAllRules -> "检测中"
                    vm.ruleHealth.isNotEmpty() -> "重新检测"
                    else -> "检测全部"
                },
                testEnabled = !vm.testingAllRules,
                testProgress = if (vm.testingAllRules) {
                    rp?.let { it.first.toFloat() / it.second.coerceAtLeast(1).toFloat() } ?: 0f
                } else null,
                onTestAll = { vm.testAllRules() },
                updateLabel = if (checkingUpdates) "检查中…" else "检查更新",
                onCheckUpdates = {
                    scope.launch {
                        checkingUpdates = true
                        updates = vm.checkRuleUpdates()
                        checkingUpdates = false
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        } // else
    } // Box
    } // 外层 Column

    // 单规则测试弹窗
    testingRule?.let { rule ->
        RuleTestDialog(rule = rule, vm = vm, onDismiss = { testingRule = null })
    }

    // 一键检测的失败原因弹窗
    healthError?.let { (name, health) ->
        Dialog(onDismissRequest = { healthError = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                insideMargin = PaddingValues(20.dp),
            ) {
                Text(
                    text = "检测失败：$name",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = health.error ?: "未知错误",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Button(
                    onClick = { healthError = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text("关闭")
                }
            }
        }
    }

    // 更新检查结果弹窗
    updates?.let { list ->
        UpdateDialog(
            updates = list,
            onDismiss = { updates = null },
            onApply = { u ->
                scope.launch {
                    val ok = vm.applyRuleUpdate(u.name, u.newYaml)
                    toast(if (ok) "已更新：${u.name}" else "更新失败：${u.name}")
                    if (ok) updates = updates?.filter { it.name != u.name }
                }
            },
        )
    }
}

/** 搜索源页右下角圆形工具按钮：导入 / 检测全部 / 检查更新（文字在左，图标在右）。 */
@Composable
private fun ManageFab(
    onImport: () -> Unit,
    testLabel: String,
    testEnabled: Boolean,
    testProgress: Float?,
    onTestAll: () -> Unit,
    updateLabel: String,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ManageFabAction(
                    icon = Icons.Filled.Add,
                    label = "导入",
                    onClick = {
                        expanded = false
                        onImport()
                    },
                )
                ManageFabAction(
                    icon = Icons.Filled.PlayArrow,
                    label = testLabel,
                    enabled = testEnabled,
                    progress = testProgress,
                    fixedWidth = 116.dp,
                    onClick = {
                        // 检测时不收起菜单，胶囊本身显示进度填充
                        onTestAll()
                    },
                )
                ManageFabAction(
                    icon = Icons.Filled.Refresh,
                    label = updateLabel,
                    fixedWidth = 116.dp,
                    onClick = {
                        expanded = false
                        onCheckUpdates()
                    },
                )
            }
        }
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.MoreVert,
                contentDescription = "工具",
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** FAB 展开后的带文字小按钮。progress 非空时胶囊本身作为进度条显示填充。 */
@Composable
private fun ManageFabAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    progress: Float? = null,
    fixedWidth: Dp? = null,
) {
    val contentColor = if (enabled) MiuixTheme.colorScheme.onSurface
    else MiuixTheme.colorScheme.secondary
    val fillColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.22f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (fixedWidth != null)
            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        else
            Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .then(if (fixedWidth != null) Modifier.width(fixedWidth) else Modifier)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
            .drawWithContent {
                if (progress != null) {
                    drawRect(
                        color = fillColor,
                        size = Size(
                            width = size.width * progress.coerceIn(0f, 1f),
                            height = size.height,
                        ),
                    )
                }
                drawContent()
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = contentColor,
        )
        if (progress == null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 查文件选择器返回的文件名。 */
private fun queryDisplayName(context: Context, uri: Uri): String {
    try {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
        )?.use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: "rule.yaml"
        }
    } catch (e: Exception) {
    }
    return "rule.yaml"
}

/** 单条规则卡片：开关 + 类型徽章 + 测试/导出/删除。 */
@Composable
private fun RuleCard(
    rule: RuleUi,
    enabled: Boolean,
    health: RuleHealth? = null,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onHealthClick: (RuleHealth) -> Unit = {},
) {
    Card(
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (rule.source == "user") {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .background(
                                    MiuixTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "自建",
                                fontSize = 10.sp,
                                color = MiuixTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                // 类型徽章放左下角
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                MiuixTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (rule.type == "magnet") "磁力" else "网盘",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (rule.version.isNotBlank()) {
                        Text(
                            text = "v${rule.version}",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 8.dp, top = 6.dp),
                        )
                    }
                    // 一键检测的健康状态徽章（异常时可点看错误）
                    if (health != null) {
                        val ok = health.ok
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp, top = 6.dp)
                                .background(
                                    if (ok) MiuixTheme.colorScheme.primaryContainer
                                    else MiuixTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(6.dp),
                                )
                                .then(
                                    if (!ok && health.error != null)
                                        Modifier.clickable { onHealthClick(health) }
                                    else Modifier,
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (ok) "正常" else "异常·看原因",
                                fontSize = 11.sp,
                                color = if (ok) MiuixTheme.colorScheme.onPrimaryContainer
                                else MiuixTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
        // 操作行
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            TextButton(text = "测试", onClick = onTest)
            TextButton(text = "导出", onClick = onExport)
            if (rule.source == "user") {
                TextButton(text = "删除", onClick = onDelete)
            }
        }
    }
}

/** 单规则测试弹窗：输关键词试跑，看耗时/条数/错误。 */
@Composable
private fun RuleTestDialog(rule: RuleUi, vm: SearchViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RuleTestResult?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            insideMargin = PaddingValues(20.dp),
        ) {
            Text(
                text = "测试：${rule.name}",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            TextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = "关键词",
                useLabelAsPlaceholder = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Button(
                onClick = {
                    scope.launch {
                        running = true
                        result = null
                        result = vm.testRule(rule.name, keyword.trim())
                        running = false
                    }
                },
                enabled = !running && keyword.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (running) "测试中…" else "开始测试")
            }
            result?.let { r ->
                Text(
                    text = "用时 ${r.elapsedMs}ms · ${r.results.size} 条结果",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                r.error?.let {
                    Text(
                        text = "错误：$it",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                r.results.take(8).forEach { item ->
                    Text(
                        text = "• ${item.title}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** 规则更新检查结果弹窗。 */
@Composable
private fun UpdateDialog(
    updates: List<RuleUpdate>,
    onDismiss: () -> Unit,
    onApply: (RuleUpdate) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            insideMargin = PaddingValues(20.dp),
        ) {
            Text(
                text = "规则更新",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            val available = updates.filter { it.error == null && it.newVersion.isNotBlank() }
            val failed = updates.filter { it.error != null }
            if (available.isEmpty() && failed.isEmpty()) {
                Text(
                    text = "所有规则都是最新",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            available.forEach { u ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = u.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${u.currentVersion.ifBlank { "?" }} → ${u.newVersion}",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.secondary,
                        )
                    }
                    TextButton(text = "更新", onClick = { onApply(u) })
                }
            }
            failed.forEach { u ->
                Text(
                    text = "${u.name}：${u.error}",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text("关闭")
            }
        }
    }
}

