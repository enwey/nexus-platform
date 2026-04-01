# 项目问题盘点与后续开发安排（更新于 2026-03-26）

## 本次迭代新增结论
- 已完成后台拆分：`dev-portal`（开发者）与 `ops-portal`（运营审核）独立演进
- 已形成可复用部署教学：见根目录 `DEPLOYMENT.md`
- 受限网络环境已有替代路径：`npm run infra:up:core` + 本机运行 backend
- 同事提出的三项关键改造已落地（JWT+Refresh/Redis、异步处理+预签名下载、迁移+索引+分页+状态机）

## 本次新增完成项
- 新增 `ops-portal` 独立项目与根脚本入口（`dev:ops`、`build:ops-portal`）
- `dev-portal` 去除审核路由，职责聚焦开发者场景
- backend CORS 支持双前端端口（5173/5174）
- 登录体系升级为 JWT + Refresh Token + Redis 失效控制
- 上传链路升级为异步处理并输出预签名下载地址
- 引入 Flyway 与分页接口，审核流转增加状态机约束
- 审核动作补充审计日志查询能力

## 当前任务拆分建议
### P0（已完成）
- token 持久化与刷新机制（替代内存会话）
- 上传/下载控制面与数据面解耦（后端控制 + 对象存储直连下载）
- DB migration、核心索引、分页与审核状态机

### P1
- 双后台发布流程（开发/运营独立发布配置）
- 上传异步任务接入重试与死信策略
- CDN 生产化接入（域名、缓存策略、回源策略）

### P2
- Android/iOS 全链路回归：上传 -> 审核 -> 下载 -> 运行

## 验收基线（更新）
- `dev-portal` 与 `ops-portal` 均可独立启动
- 运营后台登录后可访问 `/audit` 与 `/audit/logs`，非管理员账号被拦截
- 后端对两个前端来源的 CORS 预检返回正确头
- backend、dev-portal、ops-portal 均可完成构建
