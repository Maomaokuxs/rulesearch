// 版本组合经过验证可共存：
//   Gradle 9.8.0（与 miuix 的 included build 一致；miuix 的 baselineprofile 模块要求 Gradle >= 9.6）
//   AGP 9.2.1（Chaquopy 17 支持 7.3.x–9.2.x；AGP 8.x 与 Gradle 9.6+ 不兼容，故不用 8.13.2）
//   Kotlin 2.4.10
//   Chaquopy 17.0.0 / Python 3.13
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.chaquo.python") version "17.0.0" apply false
}
