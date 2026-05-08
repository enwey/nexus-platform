# Dev Portal

更新日期：2026-04-03

## 模块职责

开发者后台，主要负责：

- 开发者登录/注册
- 游戏上传与版本管理
- 开发者文档展示与模板下载

> 审核与运营配置能力已拆分到 `ops-portal`。

## 本地启动

```bash
npm run dev:portal
```

默认地址：`http://localhost:5173`

## 构建

```bash
npm run build:dev-portal
```

## 环境变量

- `VITE_PLATFORM_API_BASE_URL`：推荐使用，必须显式配置
- `VITE_API_BASE_URL`：兼容旧配置，优先级低于 `VITE_PLATFORM_API_BASE_URL`

本地开发：

```bash
cp .env.example .env.local
```

`.env.example` 默认指向本地后端 `http://127.0.0.1:8080/api/v1`，复制后即可用于本地联调。

生产构建：

- 不再回退到 `127.0.0.1`
- `vite build` 会在缺失 API 地址时直接失败，避免把错误配置发到线上

## 会话说明

- 接口统一携带 token
- 401 时自动尝试刷新；刷新失败回登录页

## 冒烟验证

```bash
npm run test:smoke
```

覆盖点：

- 生产 API 地址能被正确解析
- 缺失 API 地址时会显式失败，不会回退到本机地址
