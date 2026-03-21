# iOS Client

## 背景

`ios-client` 是 Nexus Platform 的 iOS 原生宿主工程，用于在 iOS 环境中承载小游戏内容。

## 功能

当前 iOS 工程负责：

- 使用 WKWebView 加载游戏内容
- 注入 `mock-sdk`
- 提供 iOS 端异步桥接能力
- 使用 URL scheme / 本地文件方式加载资源

## 目录

```text
ios-client/
├─ NexusPlatform/
│  └─ Sources/NexusPlatform/
│     ├─ API/       原生 API 实现
│     ├─ Bridge/    JS 与 Native 桥接
│     ├─ Views/     视图层
│     └─ Utils/     工具类
├─ Package.swift
└─ README.md
```

## 依赖

- Xcode 15+
- iOS 14+
- Swift 5.9+
- Swift Package Manager

## 启动

```bash
cd ios-client
open Package.swift
```

## 构建

```bash
swift build
```

## 联调

iOS 联调时建议：

1. 先保证 backend 和基础设施已启动
2. 确认 `mock-sdk` 可被注入
3. 在 Xcode 中运行宿主并加载游戏内容

桥接调用方式：

```javascript
window.webkit.messageHandlers.NexusBridge.postMessage(...)
```

## 已知问题

- iOS 当前不提供同步原生桥接。
- `wx.getSystemInfoSync` 与存储类 sync API 应视为 Android 优先能力。
- 后续如继续演进 iOS 运行时，建议保持异步桥接模型。
