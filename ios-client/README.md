# iOS Client

更新日期：2026-04-03

## 模块职责

`ios-client` 是 iOS 原生宿主工程，负责 WebView 承载、bridge 注入与本地资源加载。

## 当前状态

- 已具备基础容器能力
- 与 Android 同步的完整功能仍在持续对齐中

## 目录

```text
ios-client/
├─ NexusPlatform/
│  └─ Sources/NexusPlatform/
├─ Package.swift
└─ README.md
```

## 打开与构建

```bash
cd ios-client
open Package.swift
```

```bash
swift build
```

## 真机宿主工程

已新增可运行的 iOS 宿主工程生成脚本：

```bash
cd ios-client
ruby scripts/generate_host_project.rb
open NexusPlatformApp.xcodeproj
```

生成后可直接在 Xcode 中选择 `NexusPlatformApp` scheme，并连接真实 iPhone 运行。

宿主工程不再内置默认后端地址。
生成前必须显式注入 `PLATFORM_API_BASE_URL`，建议使用你这台 Mac 的局域网地址：

```bash
cd ios-client
PLATFORM_API_BASE_URL=http://你的Mac局域网IP:8080/api/v1 ruby scripts/generate_host_project.rb
```

如果没有注入地址，生成脚本会直接失败，避免真机连到错误环境。

说明：
- `Debug` 宿主配置保留本地联调所需的 ATS 例外，便于使用局域网或本机调试。
- `Release` / `Archive` 宿主配置不再包含 `localhost` / `127.0.0.1` 的 HTTP 例外，避免这些调试放行进入发布产物。

## 模拟器本地联调

模拟器现在支持默认连接本机 backend：

```bash
cd ios-client
./scripts/run-ios-simulator.sh
```

默认会使用：

```text
http://127.0.0.1:8080/api/v1
```

你也可以手动覆盖：

```bash
cd ios-client
PLATFORM_API_BASE_URL=http://127.0.0.1:8080/api/v1 ./scripts/run-ios-simulator.sh "iPhone 17 Pro"
```

要求：

1. backend 已启动
2. backend 可通过 `http://127.0.0.1:8080/api/v1/actuator/health` 访问
3. 本机已安装对应 iOS Simulator runtime

如果你直接在 Xcode 里选择 `Debug + iOS Simulator` 运行，宿主工程默认也会走：

```text
http://127.0.0.1:8080/api/v1
```

不需要再手动改 scheme。

## 命令行真机安装

先确保：

1. iPhone 已通过 Xcode 完成配对
2. Xcode 已登录 Apple ID
3. Mac 与 iPhone 在同一局域网
4. backend 监听 `0.0.0.0:8080`
5. 使用 Mac 的局域网 IP，而不是 `127.0.0.1`

然后执行：

```bash
cd ios-client
PLATFORM_API_BASE_URL=http://你的Mac局域网IP:8080/api/v1 \
DEVELOPMENT_TEAM=你的TeamID \
./scripts/run-ios-device.sh 你的设备UDID
```

可用下面命令查看设备 UDID：

```bash
xcrun xcdevice list
```

## 联调建议

1. 先确保 backend 与基础设施正常
2. 再做 iOS runtime 对齐回归
3. 优先验证：加载、桥接、下载、更新提示
4. 不要在 Xcode scheme、项目文件或脚本里保存固定的远端 API 地址
