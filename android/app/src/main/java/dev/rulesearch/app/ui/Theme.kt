package dev.rulesearch.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import dev.rulesearch.app.SearchViewModel
import dev.rulesearch.app.ThemeMode

/**
 * 当前是否深色：由 MainActivity 在根部按外观模式算好后提供，整 App 共用。
 * 切换外观模式后 themeMode（State）变化，读它的组合全部重组，立即变色。
 */
val LocalIsDark = compositionLocalOf { false }

/** 按外观模式算当前是否深色：跟随系统时读系统，浅色/深色强制。 */
@Composable
fun isDarkTheme(vm: SearchViewModel): Boolean = when (vm.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
