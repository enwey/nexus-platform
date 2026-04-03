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

## 联调建议

1. 先确保 backend 与基础设施正常
2. 再做 iOS runtime 对齐回归
3. 优先验证：加载、桥接、下载、更新提示
