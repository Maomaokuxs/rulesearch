# 在 Fedora 上构建 APK

> 构建机：Fedora 44（用户机器）。VM 上无 JDK 且磁盘小，不在 VM 上构建。

## 一、一次性环境准备

```bash
# 1. JDK 21（Gradle 9 / AGP 8.13 需要）
# 仓库里没有 JDK 21，用 Adoptium Temurin 仓库：
# sudo dnf install -y adoptium-temurin-java-repository
# sudo dnf config-manager setopt adoptium-temurin-java-repository.enabled=1
# sudo dnf install -y temurin-21-jdk

# 2. Python 3.13（Chaquopy 要求：构建机 Python 主版本必须与 APK 内一致，即 3.13）
uv python install 3.13
ls ~/.local/share/uv/python/  # 确认目录名里的版本号
export PATH="$HOME/.local/share/uv/python/cpython-3.13.13-linux-x86_64-gnu/bin:$PATH"
# 把上面这行写入 ~/.bashrc（版本号按实际安装的改）

# 3. Android SDK（约 1GB）
mkdir -p ~/Android/Sdk/cmdline-tools && cd ~/Android/Sdk/cmdline-tools
curl -O https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
unzip -q commandlinetools-linux-*_latest.zip && mv cmdline-tools latest && rm commandlinetools-linux-*_latest.zip
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
# 同样写入 ~/.bashrc
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

# 4. Gradle（只用来生成 wrapper，一次性）
curl -s https://get.sdkman.io | bash && source ~/.sdkman/bin/sdkman-init.sh
用解压版：curl -sL -o gradle-9.8.0-bin.zip https://services.gradle.org/distributions/gradle-9.5.0-bin.zip && unzip gradle-9.5.0-bin.zip -d ~/tools
```

## 二、项目准备

```bash
cd ~/workspace/rulesearch   # 或项目实际位置
git init 2>/dev/null; git add -A 2>/dev/null  # 如需版本管理

# Miuix：git submodule 引入并锁定 v0.9.4（它不在 Maven Central 上）
git submodule add https://github.com/compose-miuix-ui/miuix miuix
cd miuix && git checkout v0.9.4 && cd ..

# 告诉 Gradle SDK 位置
echo "sdk.dir=$HOME/Android/Sdk" > android/local.properties

# 生成 wrapper（一次性）
cd android && gradle wrapper --gradle-version 9.8.0
```

## 三、构建

```bash
cd android
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

首次构建很慢（20–40 分钟）：下载 Gradle 发行版、AGP、Kotlin、Chaquopy 的 Python 3.13（arm64）、
还要从源码编译 Miuix。是正常的，之后增量构建就快了。

只打 arm64（`abiFilters` 已配 `arm64-v8a`），Redmi K60 Ultra 就是 arm64。

## 四、安装到手机

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

手机开 USB 调试连电脑，或用 `adb connect` 无线连接。

## 五、改规则后

规则在**构建时**打进 APK（`syncPython` 任务自动同步 `engine/`、`rules/`、`hooks/`）。
改完规则重新 `./gradlew :app:assembleDebug` + `adb install -r` 即可。

桌面验证规则不用走 APK，直接：

```bash
python -m engine search "关键词" --rules rules/
```

## 常见问题

- `python3.13 not found`：确认 `python3.13 --version` 在 PATH 上，或在
  `android/app/build.gradle.kts` 里取消注释 `buildPython(...)` 写绝对路径。
- Miuix 相关解析失败：确认 `miuix/` submodule 已初始化且 checkout 到 tag。
- 构建机内存不足：`android/gradle.properties` 里 `org.gradle.jvmargs=-Xmx4g` 按需调大。
