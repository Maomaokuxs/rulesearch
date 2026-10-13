package dev.rulesearch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.PAN_BRAND_DEFS
import dev.rulesearch.app.SearchViewModel
import dev.rulesearch.app.SortMode
import dev.rulesearch.app.SizeFilter
import dev.rulesearch.app.TimeFilter
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(vm: SearchViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MiuixTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = "筛选",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
            )
            FilterSectionLabel("排序方式")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                listOf(SortMode.DEFAULT, SortMode.SIZE_DESC, SortMode.TIME_DESC).forEach { mode ->
                    FilterChip(
                        label = mode.label,
                        selected = vm.sortMode == mode,
                        onClick = { vm.sortMode = mode },
                    )
                }
            }
            if (vm.tabPanBrands.isNotEmpty()) {
                FilterSectionLabel("网盘")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    items(vm.tabPanBrands, key = { it }) { brand ->
                        val shown = brand !in vm.hiddenPanBrands
                        val label = PAN_BRAND_DEFS.firstOrNull { it.first == brand }?.second ?: brand
                        FilterChip(
                            label = label,
                            selected = shown,
                            dotColor = if (shown) brandColor(brand) else null,
                            onClick = { vm.togglePanBrandHidden(brand) },
                        )
                    }
                }
            }
            FilterSectionLabel("文件大小")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                SizeFilter.entries.forEach { f ->
                    FilterChip(
                        label = f.label,
                        selected = vm.sizeFilter == f,
                        onClick = { vm.sizeFilter = f },
                    )
                }
            }
            FilterSectionLabel("更新时间")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                TimeFilter.entries.forEach { f ->
                    FilterChip(
                        label = f.label,
                        selected = vm.timeFilter == f,
                        onClick = { vm.timeFilter = f },
                    )
                }
            }
            // 仅看有效链接：独立卡片行（图标 + 标题 + 说明 + Switch），cardGray 背景 + 16dp 圆角
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardGray())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✓",
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                ) {
                    Text(text = "仅看有效链接", fontSize = 15.sp)
                    Text(
                        text = if (vm.checkingLinks) {
                            val prog = vm.checkProgress
                            if (prog != null) "检测中 ${prog.first}/${prog.second}…"
                            else "检测中…"
                        } else "只显示检测可访问的网盘链接",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Switch(
                    checked = vm.onlyReachable,
                    onCheckedChange = { vm.setOnlyReachableFilter(it) },
                    colors = top.yukonga.miuix.kmp.basic.SwitchDefaults.switchColors(
                        checkedTrackColor = MiuixTheme.colorScheme.primary,
                        uncheckedTrackColor = COLOR_SWITCH_OFF,
                    ),
                )
            }
            // 完成按钮：蓝底白字 17px，高 48px，圆角 12px，全宽
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.primary)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "完成",
                    fontSize = 17.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** 筛选胶囊：选中态主色底 + 白字，未选中 cardGray 底 + 主文字色。 */
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MiuixTheme.colorScheme.primary
                else cardGray(),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor),
            )
        }
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (selected) Color.White else MiuixTheme.colorScheme.onSurface,
        )
    }
}
