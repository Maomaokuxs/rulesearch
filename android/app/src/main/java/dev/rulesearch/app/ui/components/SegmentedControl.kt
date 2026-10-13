package dev.rulesearch.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 漏斗筛选图标：Canvas 手绘线形漏斗（material-icons-core 里没有）。 */
@Composable
internal fun FunnelIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        // 漏斗主体：上宽下窄的梯形 + 底部出水管
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.55f)
            lineTo(w * 0.58f, h * 0.85f)
            lineTo(w * 0.42f, h * 0.85f)
            lineTo(w * 0.42f, h * 0.55f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
    }
}

/** 分段控件（mock 样式）：浅灰圆角容器，选中项白底黑字+阴影。 */
@Composable
internal fun SegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    // 深色：选中项比卡片 #3f3f3f 亮一档；浅色：白底黑字
    val selectedBg = if (dark) Color(0xFF5A5A5A) else Color.White
    val selectedText = if (dark) Color.White else Color.Black
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardGray())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (selected) Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .background(if (selected) selectedBg else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = if (selected) selectedText else MiuixTheme.colorScheme.secondary,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}
