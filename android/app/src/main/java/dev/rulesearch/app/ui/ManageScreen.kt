package dev.rulesearch.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 规则管理页（我的里的二级页面）：添加/检测/已添加列表，点行进详情。 */
@Composable
fun ManageTab(vm: SearchViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedRule by remember { mutableStateOf<RuleUi?>(null) }
    var testingRule by remember { mutableStateOf<RuleUi?>(null) }
    var updates by remember { mutableStateOf<List<RuleUpdate>?>(null) }
    var checkingUpdates by remember { mutableStateOf(false) }
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "规则管理",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 16.dp),
        )

        if (vm.rules.isEmpty()) {
            Text(
                "搜索源加载中…",
                color = MiuixTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            // 第一张卡片：添加规则 / 检测全部 / 检查更新
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBackground()),
            ) {
                ManageMenuRow(
                    title = "添加规则",
                    onClick = { picker.launch("*/*") },
                )
                ManageDivider()
                val rp = vm.ruleTestProgress
                ManageMenuRow(
                    title = "检测全部",
                    trailingText = if (vm.testingAllRules) {
                        rp?.let { "检测中 ${it.first}/${it.second}…" } ?: "检测中…"
                    } else null,
                    onClick = { if (!vm.testingAllRules) vm.testAllRules() },
                )
                ManageDivider()
                ManageMenuRow(
                    title = "检查更新",
                    trailingText = if (checkingUpdates) "检查中…" else null,
                    onClick = {
                        if (!checkingUpdates) {
                            scope.launch {
                                checkingUpdates = true
                                updates = vm.checkRuleUpdates()
                                checkingUpdates = false
                            }
                        }
                    },
                )
            }

            // 分组标题
            Text(
                text = "已添加",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            )

            // 第二张卡片：规则列表
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBackground()),
            ) {
                vm.rules.forEachIndexed { index, rule ->
                    if (index > 0) ManageDivider()
                    RuleListRow(
                        rule = rule,
                        enabled = vm.isRuleEnabled(rule.name),
                        onClick = { selectedRule = rule },
                        onToggle = { vm.setRuleEnabled(rule.name, it) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 规则详情弹窗：开关 + 测试/导出/删除
    selectedRule?.let { rule ->
        RuleDetailDialog(
            rule = rule,
            enabled = vm.isRuleEnabled(rule.name),
            health = vm.ruleHealth[rule.name],
            onToggle = { vm.setRuleEnabled(rule.name, it) },
            onTest = {
                selectedRule = null
                testingRule = rule
            },
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
                    if (ok) selectedRule = null
                }
            },
            onHealthClick = { healthError = rule.name to it },
            onDismiss = { selectedRule = null },
        )
    }

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

/** 卡片内 hairline 分割线。 */
@Composable
private fun ManageDivider() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
            .height(0.5.dp).background(MiuixTheme.colorScheme.outline),
    )
}

/** 第一张卡片里的菜单行：标题 + 右侧箭头。 */
@Composable
private fun ManageMenuRow(
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                text = "›",
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.secondary,
            )
        }
    }
}

/** 规则列表行：规则名 + 启用状态。 */
@Composable
private fun RuleListRow(
    rule: RuleUi,
    enabled: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rule.name,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** 规则详情弹窗：开关 + 类型/版本/健康 + 测试/导出/删除。 */
@Composable
private fun RuleDetailDialog(
    rule: RuleUi,
    enabled: Boolean,
    health: RuleHealth?,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onHealthClick: (RuleHealth) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            insideMargin = PaddingValues(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = rule.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
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
                        if (rule.source == "user") {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 6.dp)
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
                    // 健康状态
                    if (health != null) {
                        val ok = health.ok
                        Text(
                            text = if (ok) "检测正常 · ${health.count} 条" else "检测异常 · 看原因",
                            fontSize = 12.sp,
                            color = if (ok) Color(0xFF34C759) else MiuixTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                                .then(
                                    if (!ok && health.error != null)
                                        Modifier.clickable { onHealthClick(health) }
                                    else Modifier,
                                ),
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("测试")
                }
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("导出")
                }
            }
            if (rule.source == "user") {
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("删除规则")
                }
            }
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

/** 单规则测试弹窗：点测试直接跑默认关键词，看耗时/条数/错误。 */
@Composable
private fun RuleTestDialog(rule: RuleUi, vm: SearchViewModel, onDismiss: () -> Unit) {
    var running by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<RuleTestResult?>(null) }
    val keyword = vm.keyword.trim().ifBlank { "三体" }

    LaunchedEffect(rule.name) {
        result = vm.testRule(rule.name, keyword)
        running = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            insideMargin = PaddingValues(20.dp),
        ) {
            Text(
                text = "测试：" + rule.name,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            Text(
                text = "关键词：" + keyword,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (running) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "测试中…",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.secondary,
                    )
                }
            }
            result?.let { r ->
                Text(
                    text = "用时 " + r.elapsedMs + "ms · " + r.results.size + " 条结果",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                r.error?.let {
                    Text(
                        text = "错误：" + it,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                r.results.take(8).forEach { item ->
                    Text(
                        text = "• " + item.title,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
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
