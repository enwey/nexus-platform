# Dev Portal

## 说明
`dev-portal` 是开发者后台，负责开发者登录、游戏上传、版本查看。

## 职责范围
- 登录/注册
- 控制台
- 游戏列表
- 上传游戏

> 审核功能已拆分到独立项目 `ops-portal`。

## 启动
```bash
npm run dev:portal
```

## 构建
```bash
npm run build:dev-portal
```

## 默认接口地址
`VITE_API_BASE_URL=http://localhost:8080/api/v1`

## 会话与鉴权
- 接口层统一携带 token
- 登录状态失效时自动提示并跳转登录页
