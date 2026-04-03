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

- `VITE_API_BASE_URL`（默认 `http://localhost:8080/api/v1`）

## 会话说明

- 接口统一携带 token
- 401 时自动尝试刷新；刷新失败回登录页
