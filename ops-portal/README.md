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

## 权限说明

- 仅管理员账号可访问核心运营页面
- 会话失效自动刷新，失败则回登录页
