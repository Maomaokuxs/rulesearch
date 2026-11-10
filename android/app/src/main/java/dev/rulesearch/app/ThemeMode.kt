package dev.rulesearch.app

/** 外观模式：跟随系统 / 浅色 / 深色（SharedPreferences "rulesearch" 的 theme_mode 键）。 */
enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
