package dev.rulesearch.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text

// 系统下载器会"接"磁力链接但实际下不了，排除掉
private const val SYS_DL_UI = "com.android.providers.downloads.ui"
private const val SYS_DL = "com.android.providers.downloads"
private const val XUNLEI_PKG = "com.xunlei.downloadprovider"

/** 真正能处理磁力链接的应用（排除系统下载器）。 */
fun magnetHandlers(context: Context) =
    context.packageManager
        .queryIntentActivities(
            Intent(Intent.ACTION_VIEW, "magnet:?xt=urn:btih:0".toUri()),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        .filter {
            it.activityInfo.packageName != SYS_DL_UI &&
                it.activityInfo.packageName != SYS_DL
        }

/**
 * 打开链接。磁力：有下载器则弹选择器，没有则回调 onNoDownloader；
 * 普通链接直接走浏览器。返回是否真正打开了。
 */
fun openLink(context: Context, url: String, onNoDownloader: () -> Unit): Boolean {
    if (url.startsWith("magnet:")) {
        if (magnetHandlers(context).isEmpty()) {
            onNoDownloader()
            return false
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW, url.toUri()),
                "用下载器打开",
            ),
        )
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
    return true
}

/** 没有磁力下载工具时的提示弹窗：复制磁力 / 去装迅雷。 */
@Composable
fun NoDownloaderDialog(link: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "没有磁力下载工具",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "手机上没检测到能下载磁力链接的应用，装一个下载器后再试。你也可以先把磁力链接复制出来。",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(link))
                            Toast.makeText(context, "磁力链接已复制", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                    ) {
                        Text("复制磁力链接")
                    }
                    Button(
                        onClick = {
                            // 先试应用商店，没有再走浏览器
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "market://details?id=$XUNLEI_PKG".toUri(),
                                    ),
                                )
                            } catch (e: Exception) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://www.xunlei.com/".toUri(),
                                    ),
                                )
                            }
                            onDismiss()
                        },
                    ) {
                        Text("安装迅雷")
                    }
                }
            }
        }
    }
}
