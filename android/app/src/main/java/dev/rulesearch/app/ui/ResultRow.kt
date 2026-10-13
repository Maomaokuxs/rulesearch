package dev.rulesearch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rulesearch.app.SearchResultUi
import dev.rulesearch.app.panBrandOf
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 标题里的关键词高亮：命中部分染主色加粗，其余不动。 */
fun highlightTitle(title: String, keyword: String, highlight: Color): AnnotatedString {
    val kw = keyword.trim()
    if (kw.isEmpty()) return AnnotatedString(title)
    return buildAnnotatedString {
        append(title)
        var i = title.indexOf(kw, ignoreCase = true)
        while (i >= 0) {
            addStyle(
                SpanStyle(color = highlight, fontWeight = FontWeight.Medium),
                i,
                i + kw.length,
            )
            i = title.indexOf(kw, i + kw.length, ignoreCase = true)
        }
    }
}

/** 结果行第二行：磁力显示来源站点 · 大小/时间；网盘只显示大小/时间（无品牌标示）。 */
@Composable
fun ResultMetaLine(r: SearchResultUi) {
    val brand = panBrandOf(r.url)
    Row(verticalAlignment = Alignment.CenterVertically) {
        val meta = r.size.ifEmpty { r.time }
        if (brand == null) {
            // 磁力：保留来源站点名
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(MiuixTheme.colorScheme.secondary.copy(alpha = 0.45f)),
            )
            Spacer(Modifier.width(6.dp))
            TextLine(r.source)
            if (meta.isNotEmpty()) {
                TextLine(" · $meta")
            }
        } else {
            // 网盘：不显示品牌圆点和品牌名，直接显示大小/时间
            if (meta.isNotEmpty()) {
                TextLine(meta)
            }
        }
    }
}

@Composable
private fun TextLine(s: String) {
    top.yukonga.miuix.kmp.basic.Text(
        text = s,
        fontSize = 12.sp,
        color = MiuixTheme.colorScheme.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 极简结果行：标题（关键词高亮）+ 元信息行；右侧按状态显示 有效/失效 或多选勾选圈。 */
@Composable
fun ResultRow(
    r: SearchResultUi,
    keyword: String,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    /** null=未检测，true=可访问，false=疑似失效 */
    linkOk: Boolean? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val primary = MiuixTheme.colorScheme.primary
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            // 顶部对齐：标题 1 行/2 行时右侧指示器位置不动
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = highlightTitle(r.title, keyword, primary),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(modifier = Modifier.padding(top = 6.dp)) {
                    ResultMetaLine(r)
                }
            }
            // 右侧指示区定宽 48dp：三种状态（无/勾选圈/有效失效）标题列宽度一致
            Box(
                modifier = Modifier.width(48.dp).padding(top = 2.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                when {
                    selectionMode -> SelectCircle(selected = selected)
                    linkOk != null -> top.yukonga.miuix.kmp.basic.Text(
                        text = if (linkOk) "有效" else "失效",
                        fontSize = 12.sp,
                        color = if (linkOk) COLOR_REACHABLE else MiuixTheme.colorScheme.error,
                        maxLines = 1,
                    )
                }
            }
        }
        ResultDivider()
    }
}

/** 行与行之间的细分割线。 */
@Composable
fun ResultDivider() {
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(MiuixTheme.colorScheme.secondary.copy(alpha = 0.15f)),
    )
}

/** 多选模式右端的勾选圈。 */
@Composable
fun SelectCircle(selected: Boolean) {
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape)
            .background(
                if (selected) MiuixTheme.colorScheme.primary
                else cardGray(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
