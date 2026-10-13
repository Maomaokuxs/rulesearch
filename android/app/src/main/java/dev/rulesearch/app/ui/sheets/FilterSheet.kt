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
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "筛选",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            FilterSectionLabel("排序方式")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp),
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
                    modifier = Modifier.padding(bottom = 12.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "仅看有效链接", fontSize = 15.sp)
                    if (vm.checkingLinks) {
                        val prog = vm.checkProgress
                        Text(
                            text = if (prog != null) "检测中 ${prog.first}/${prog.second}…"
                            else "检测中…",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
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
        fontSize = 14.sp,
        color = MiuixTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** 筛选胶囊：选中态染主色底 + 主色字。 */
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
        modifier = Modifier.clip(CircleShape)
            .background(
                if (selected) MiuixTheme.colorScheme.primaryContainer
                else cardGray(),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor),
            )
        }
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
    }
}
