pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Chaquopy 插件仓库
        maven("https://chaquo.com/maven")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "rulesearch"
include(":app")

// Miuix 不在 Maven Central 上，用 composite build 从源码引入。
// 首次使用前执行：git submodule update --init
// 锁版本：miuix submodule 固定在 v0.9.4 tag。
includeBuild("../miuix") {
    dependencySubstitution {
        substitute(module("top.yukonga.miuix.kmp:miuix-ui")).using(project(":miuix-ui"))
    }
}
