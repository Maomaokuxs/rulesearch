package dev.rulesearch.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.rulesearch.app.PAN_BRAND_DEFS

/** 网盘品牌色：结果行里那个 8dp 小圆点的颜色，一眼认品牌。 */
val BRAND_COLORS: Map<String, Color> = mapOf(
    "quark" to Color(0xFFFF6A00), // 夸克
    "baidu" to Color(0xFF2F6DF6), // 百度
    "ali" to Color(0xFF704DF0), // 阿里
    "p123" to Color(0xFF12A594), // 123
    "uc" to Color(0xFFE5484D), // UC
    "xunlei" to Color(0xFF0EA5E9), // 迅雷
    "ct189" to Color(0xFFF59E0B), // 天翼
    "cmcc139" to Color(0xFF00A0E9), // 移动
    "weiyun" to Color(0xFF07C160), // 微云
    "lanzou" to Color(0xFF3BAFDA), // 蓝奏
    "yun115" to Color(0xFF12A594), // 115
    "cow" to Color(0xFFFFB020), // 奶牛快传
    "ctfile" to Color(0xFF00B578), // 城通
    "wo" to Color(0xFFFF5C39), // 沃云盘
)

/** 链接有效标记的绿色（Miuix 色板里没有语义绿，单独定一个深浅模式都可读的）。 */
val COLOR_REACHABLE = Color(0xFF34C759)

fun brandColor(key: String): Color = BRAND_COLORS[key] ?: Color(0xFF8A8A8E)

/** 品牌 key → 中文名（夸克网盘 / 百度网盘 …），找不到回退 key 本身。 */
fun brandLabel(key: String): String =
    PAN_BRAND_DEFS.firstOrNull { it.first == key }?.second ?: key

val COLOR_SWITCH_OFF = Color(0xFFE3E3E8)

/** 深浅自适应灰（用户 mock 调定深色值 #3f3f3f）：历史胶囊、分段容器、未选中 chip、多选圈、骨架屏 */
@Composable
fun cardGray(): Color = if (isSystemInDarkTheme()) Color(0xFF3F3F3F) else Color(0xFFF2F3F5)

/** 页面背景：深色纯黑 / 浅色浅灰（浅色下与白色导航栏区分） */
@Composable
fun pageBackground(): Color = if (isSystemInDarkTheme()) Color.Black else Color(0xFFF2F3F5)

/** 卡片背景：深色 #3f3f3f / 浅色白 */
@Composable
fun cardBackground(): Color = if (isSystemInDarkTheme()) Color(0xFF3F3F3F) else Color.White
