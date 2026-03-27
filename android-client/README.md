# Android Client

## 背景

`android-client` 是 Nexus Platform 的 Android 原生宿主工程，用来承载 WebView、桥接 SDK 注入和小游戏资源加载。

## 功能

当前 Android 工程承担以下职责：

- 加载游戏 ZIP 解压后的静态资源
- 在 WebView 中注入 `mock-sdk`
- 暴露 Android 异步桥接：`AndroidApp`
- 暴露 Android 同步桥接：`AndroidAppSync`
- 为后续 Android 原生运行时功能提供扩展入口

## 目录

```text
android-client/
├─ app/
│  ├─ src/main/java/   Android 代码
│  ├─ src/main/res/    资源文件
│  └─ build.gradle.kts
├─ gradle/wrapper/     Gradle wrapper 配置
├─ gradlew
├─ gradlew.bat
├─ ENGINEERING.md      工程说明
└─ README.md
```

## 依赖

- Android Studio Hedgehog 或更高
- JDK 17
- Android SDK 34
- Gradle 8.2 对应运行环境

## 启动

建议直接使用 Android Studio 打开 `android-client/` 目录。

## 构建

### 工程完整性检查

在仓库根目录执行：

```bash
npm run check:android-setup
```

### 构建 Debug 包

如果 wrapper 文件完整，可执行：

```bash
./gradlew assembleDebug
```

Windows 下：

```bat
gradlew.bat assembleDebug
```

默认情况下，wrapper 使用官方 Gradle 分发地址，适合直接提交到 GitHub。
如果本机需要无外网构建，可以临时改为 `D:\tools\ai_install` 下的本地分发包，但不要把这种机器专用配置提交到仓库。

## 联调

Android 联调建议顺序：

1. 先在仓库根目录执行 `npm run check:contracts`
2. 确认 `mock-sdk` 产物可被注入
3. 确认 backend 和基础设施已启动
4. 再运行 Android 客户端加载游戏资源

桥接联调时：

- 异步 API 通过 `window.AndroidApp.postMessage(...)`
- 同步 API 通过 `window.AndroidAppSync.invokeSync(...)`

## 已知问题

- 依赖 `Nakama Java SDK` 需要通过 `JitPack` 拉取。
- 当前 UI 仍是最小可运行骨架，不是完整产品界面。
