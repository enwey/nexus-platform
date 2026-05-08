# Ops Portal

更新日期：2026-04-03

## 模块职责

运营后台，主要负责：

- 审核（通过/驳回）
- 审计日志查看
- Runtime Ops Console（运行时配置与运营配置）
- 发现页运营位配置
- 单游戏运营资料编辑

## 本地启动

```bash
npm run dev:ops
```

默认地址：`http://localhost:5174`

## 构建

```bash
npm run build:ops-portal
```

## 环境变量

- `VITE_PLATFORM_API_BASE_URL`：推荐使用，必须显式配置
- `VITE_API_BASE_URL`：兼容旧配置，优先级低于 `VITE_PLATFORM_API_BASE_URL`

本地开发：

```bash
cp .env.example .env.local
```

`.env.example` 默认指向本地后端 `http://127.0.0.1:8080/api/v1`。

生产构建：

- 不再回退到 `127.0.0.1`
- `vite build` 会在缺失 API 地址时直接失败，避免错误配置进入线上

## 权限说明

- 仅管理员账号可访问核心运营页面
- 会话失效自动刷新，失败则回登录页

## 冒烟验证

```bash
npm run test:smoke
```
