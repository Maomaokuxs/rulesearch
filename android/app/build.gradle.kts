import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
}

android {
    namespace = "dev.rulesearch.app"
    // Miuix v0.9.4 要求 compileSdk 37+（AAR metadata 校验）；API 37 的 platform 目录叫 android-37.0，
    // 故用 release/minorApiLevel 写法
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.rulesearch.app"
        minSdk = 26 // Chaquopy 17 要求 minSdk >= 24；26 起可用自适应图标
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            // Python 3.13 只支持 arm64-v8a / x86_64；真机只留 arm64 构建最快
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 个人自用：release 沿用 debug 签名，可直接覆盖安装
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
        // 构建机需要 Python 3.13（主版本必须一致），见 ../gradle.properties 说明
        // buildPython("/path/to/python3.13")
        pip {
            install("requests")
            install("beautifulsoup4")
            install("pyyaml")
        }
    }
}

// 把仓库根目录的 engine/、rules/、hooks/ 同步进 Chaquopy 的 python 源码目录。
// 每次构建前自动执行，生成的 src/main/python 不进版本库。
val syncPython by tasks.registering(Sync::class) {
    from(rootDir.resolve("../engine")) { into("engine") }
    from(rootDir.resolve("../rules")) { into("rules") }
    from(rootDir.resolve("../hooks")) { into("hooks") }
    from("src/python-src")
    into("src/main/python")
}
tasks.named("preBuild") { dependsOn(syncPython) }

// Chaquopy 的 merge*PythonSources 任务消费 src/main/python（syncPython 的输出），
// Gradle 9.8 严格校验要求显式声明依赖，否则报 implicit dependency 错误。
afterEvaluate {
    tasks.matching { it.name.matches(Regex("merge.*PythonSources")) }.configureEach {
        dependsOn(syncPython)
    }
}

dependencies {
    // Miuix：版本号由 composite build 的 dependencySubstitution 提供
    implementation("top.yukonga.miuix.kmp:miuix-ui")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.animation:animation")
}
