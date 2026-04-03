# Android Client

更新日期：2026-04-03

## 模块职责

`android-client` 是小游戏 Android 宿主，负责：

- 主端 UI（游戏库、发现、账号、运行页）
- WebView 运行容器
- `wx` bridge（`AndroidApp` / `AndroidAppSync`）
- 下载、解压、校验、更新与本地缓存

## 构建要求

- Android SDK 34
- JDK 17
- Gradle Wrapper（项目内置）

## 本地构建

```bash
cmd /c android-client\gradlew.bat -p android-client assembleDebug
```

## 真机联调（重要）

默认 `BACKEND_BASE_URL` 是 `http://10.0.2.2:8080/api/v1`（模拟器专用）。

真机必须使用局域网地址重新编译：

```powershell
$env:BACKEND_BASE_URL='http://<你的电脑局域网IP>:8080/api/v1'
. .\scripts\env-local.ps1
cmd /c android-client\gradlew.bat -p android-client assembleDebug
```

否则会出现“页面获取不到数据/游戏下载失败”。

## 安装 APK

```powershell
$adb='C:\Users\7410\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r .\android-client\app\build\outputs\apk\debug\app-debug.apk
```
