# Mock SDK

## 背景

`mock-sdk` 是运行在游戏代码侧的桥接 SDK，用来向游戏暴露 `wx.*` 风格接口，并把调用转发到原生宿主或 Web mock 环境。

## 功能

主要能力包括：

- 统一 `wx` API 入口
- 区分 Android / iOS / Web 运行环境
- 异步桥接转发
- Web 环境 mock 数据支持
- 运行时兼容层封装

## 目录

```text
mock-sdk/
├─ src/
│  ├─ api/         基础 API 与扩展 API
│  ├─ core/        核心桥接实现
│  ├─ types/       类型定义
│  └─ index.ts     SDK 入口
├─ tests/
├─ package.json
└─ README.md
```

## 依赖

- Node.js 18+
- npm 9+
- TypeScript
- Vite
- Vitest

## 启动

如果只做开发调试：

```bash
npm run dev --prefix mock-sdk
```

## 构建

### 类型检查

```bash
npm run check:sdk-types
```

### 构建产物

```bash
npm run build --prefix mock-sdk
```

## 联调

联调时通常由原生宿主注入：

- Android：注入到 WebView
- iOS：注入到 WKWebView
- Web：直接使用 mock 行为

新增桥接 API 前，请先更新：

- `contracts/bridge-api.json`

## 已知问题

- Android 已支持同步桥接。
- iOS 当前不支持同步原生桥接，应优先使用异步 API。
- 当前环境下执行 Vite build 可能受 `spawn EPERM` 限制影响。
