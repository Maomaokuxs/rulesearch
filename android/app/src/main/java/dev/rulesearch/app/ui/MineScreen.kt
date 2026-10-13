package dev.rulesearch.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** "我的"页（底部 dock 的"我的"标签）：足迹 / 搜索源 / 内容过滤 / 关于，二级页面结构。 */
@Composable
fun MineTab(vm: SearchViewModel) {
    var page by remember { mutableStateOf<String?>(null) } // null=菜单

    BackHandler(enabled = page != null) { page = null }

    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(180))) },
        label = "mine_nav",
    ) { p ->
        when (p) {
            "footprints" -> MineSubPage(onBack = { page = null }) { FootprintsTab(vm = vm) }
            "rules" -> MineSubPage(onBack = { page = null }) { ManageTab(vm = vm) }
            "blocklist" -> MineSubPage(onBack = { page = null }) { BlocklistPage(vm = vm) }
            "about" -> MineSubPage(onBack = { page = null }) { AboutPage() }
            else -> MineMenu(
                vm = vm,
                onFootprints = { page = "footprints" },
                onRules = { page = "rules" },
                onBlocklist = { page = "blocklist" },
                onAbout = { page = "about" },
            )
        }
    }
}

/** "我的"一级菜单：页底浅灰 + 三组白卡片（mock 样式）。 */
@Composable
private fun MineMenu(
    vm: SearchViewModel,
    onFootprints: () -> Unit,
    onRules: () -> Unit,
    onBlocklist: () -> Unit,
    onAbout: () -> Unit,
) {
    val health = vm.ruleHealth
    val healthSub = if (health.isNotEmpty()) {
        " · ${health.count { it.value.ok }}/${health.size} 正常"
    } else {
        ""
    }
    Column(
        modifier = Modifier.fillMaxSize().background(pageBackground()),
    ) {
        Text(
            text = "我的",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 64.dp, bottom = 16.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. 足迹
            MineCard(onClick = onFootprints) {
                MineMenuRow(
                    title = "足迹",
                    trailing = {
                        Box(
                            modifier = Modifier.clip(CircleShape)
                                .background(Color(0xFFC7C7CC))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "${vm.footprints.size}",
                                fontSize = 12.sp,
                                color = Color.White,
                            )
                        }
                    },
                )
            }
            // 2. 规则管理 + 内容过滤（一张卡片两行）
            MineCard(onClick = null) {
                MineMenuRow(
                    title = "规则管理",
                    trailing = {
                        Text(
                            text = "${vm.rules.size} 条规则$healthSub",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.secondary,
                        )
                    },
                    onClick = onRules,
                )
                Box(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                        .height(0.5.dp).background(MiuixTheme.colorScheme.outline),
                )
                MineMenuRow(
                    title = "内容过滤",
                    trailing = {
                        Text(
                            text = "${vm.blockedWords.size} 个词",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.secondary,
                        )
                    },
                    onClick = onBlocklist,
                )
            }
            // 3. 关于
            MineCard(onClick = onAbout) {
                MineMenuRow(title = "关于", trailing = null)
            }
        }
    }
}

/** 白色圆角卡片容器。 */
@Composable
private fun MineCard(
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val mod = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(cardBackground())
    Column(
        modifier = if (onClick != null) mod.clickable(onClick = onClick) else mod,
    ) {
        content()
    }
}

@Composable
private fun MineMenuRow(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val mod = Modifier.fillMaxWidth().heightIn(min = 54.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    Row(
        modifier = if (onClick != null) mod.clickable(onClick = onClick) else mod,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
        } else {
            Text(
                text = "〉",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.secondary,
            )
        }
    }
}

/** "我的"二级页面：顶部返回行 + 内容。 */
@Composable
private fun MineSubPage(onBack: () -> Unit, content: @Composable () -> Unit) {
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

/** 关于页。 */
@Composable
private fun AboutPage() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "RuleSearch",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 48.dp),
        )
        Text(
            text = "规则引擎式网盘 / 磁力聚合搜索",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.secondary,
        )
        Text(
            text = "手机端独立运行，不依赖电脑",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = "github.com/Maomaokuxs/rulesearch",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/Maomaokuxs/rulesearch".toUri(),
                        ),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
