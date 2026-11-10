package dev.rulesearch.app

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dev.rulesearch.app.ui.AppRoot
import dev.rulesearch.app.ui.LocalIsDark
import dev.rulesearch.app.ui.isDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class MainActivity : ComponentActivity() {
    /** 剪贴板嗅探到的链接（回到前台时检查一次） */
    private var clipboardLink by mutableStateOf<String?>(null)
    private var lastPromptedLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        enableEdgeToEdge()
        setContent {
            // 外观模式存在 ViewModel 里（State）：切换后读它的组合全部重组，立即变色；
            // 同一个 ViewModel 实例传给 AppRoot，避免建两份。
            val vm: SearchViewModel = viewModel()
            val dark = isDarkTheme(vm)
            CompositionLocalProvider(LocalIsDark provides dark) {
            MiuixTheme(
                colors = if (dark) darkColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF2F7CF6),
                    // secondary #505050 在黑底上对比度不足，提亮到 #9A9A9A 保可读
                    secondary = androidx.compose.ui.graphics.Color(0xFF9A9A9A),
                ) else lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF2F7CF6),
                    // secondary #E6E6E6 在白底上近乎不可见，加深到 #8A8A8E 保可读
                    secondary = androidx.compose.ui.graphics.Color(0xFF8A8A8E),
                )
            ) {
                AppRoot(
                    vm = vm,
                    // Deep link 进来（如 rulesearch://search?keyword=三体）直接开搜
                    initialKeyword = intent?.data?.getQueryParameter("keyword"),
                    // 调试用：搜出结果后自动弹出第一条的详情弹窗
                    autoOpenDialog = intent?.data?.getQueryParameter("opendialog") == "1",
                    clipboardLink = clipboardLink,
                    onClipboardConsumed = { clipboardLink = null },
                )
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sniffClipboard()
    }

    /** 嗅探剪贴板里的磁力链/http 链接，换了新链接才弹窗一次。 */
    private fun sniffClipboard() {
        val text = try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        } catch (e: Exception) {
            null
        } ?: return
        val link = when {
            text.startsWith("magnet:", ignoreCase = true) -> text
            else -> Regex("""https?://\S+""").find(text)?.value
        } ?: return
        if (link != lastPromptedLink) {
            lastPromptedLink = link
            clipboardLink = link
        }
    }
}
