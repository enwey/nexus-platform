﻿# Dev Portal

## 背景

`dev-portal` 是 Nexus Platform 的开发者后台，用于开发者或运营人员完成平台管理流程。

## 功能

主要包括：

- 登录与注册
- 游戏列表查看
- 游戏上传
- 游戏审核
- 开发者维度管理

## 目录

```text
dev-portal/
├─ src/
│  ├─ api/         后端请求封装
│  ├─ views/       页面视图
│  ├─ router/      路由配置
│  ├─ stores/      状态管理
│  ├─ assets/      静态资源
│  └─ main.js      入口文件
├─ public/
├─ package.json
└─ README.md
```

## 依赖

- Node.js 18+
- npm 9+

## 启动

在仓库根目录执行：

```bash
npm run dev:portal
```

或直接执行：

```bash
npm run dev --prefix dev-portal
```

## 构建

```bash
npm run build:dev-portal
```

## 联调

推荐与 backend 一起联调：

1. `npm run infra:up`
2. `mvn -f backend/pom.xml spring-boot:run`
3. `npm run dev:portal`

环境变量默认使用：

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

如果新增或修改了调用接口，必须同步更新：

- `contracts/backend-api.json`

## 已知问题

- 当前环境下执行 Vite build 可能受 `spawn EPERM` 限制影响。
- 页面结构已脱离默认模板，但 UI 仍属于平台收束阶段的基础版本。
