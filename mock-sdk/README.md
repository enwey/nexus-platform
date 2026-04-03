# Mock SDK

更新日期：2026-04-03

## 模块职责

`mock-sdk` 提供 `wx.*` 兼容层，负责把游戏侧 API 调用桥接到原生宿主。

## 当前能力

- `wx` 主要接口兼容（含运行时常用 API）
- 异步桥接主链路
- Android 同步桥接支持（部分 sync API）
- UpdateManager 基础能力映射

## 构建

```bash
npm run build --prefix mock-sdk
```

## 类型检查

```bash
npm run check:sdk-types
```

## 注意

- iOS 侧以异步桥接为主
- 新增 API 时需同步更新桥接契约与文档
