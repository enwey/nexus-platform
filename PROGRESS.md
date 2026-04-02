# 项目进展记录（更新于 2026-03-26）

## 当前状态
项目已完成“开发者后台/运营后台拆分 + 认证与分发链路安全升级”的 P0 改造，后端与双前端均可构建通过。

## 今日完成

### 部署与环境
- 增加本地环境脚本：`scripts/env-local.ps1`、`scripts/check-env.ps1`、`scripts/start-local.ps1`
- 增加本机一键部署脚本：`scripts/deploy-local-user.ps1`
- 新增 `infra:up:core`，支持受限网络环境只启动核心基础设施
- 补齐 `backend/Dockerfile`

### backend
- 清理多处源码编码问题（BOM/乱码）
- 修复编译阻塞并恢复 Maven 可构建
- 接入 JWT 访问令牌 + Refresh Token 机制，支持 `/user/refresh` 与 `/user/logout`
- 接入 Redis：访问令牌 denylist、刷新令牌轮换与失效控制
- 落地权限模型（RBAC）并补齐审核相关鉴权
- 审核通过/拒绝引入原因字段与审计日志（`/audit/logs`）
- 游戏分发升级为“上传后异步处理 + 预签名下载 URL”
- 审核状态机收敛：仅允许 `PENDING -> APPROVED/REJECTED`
- 增加分页查询接口与 `PageResult`，避免大列表全量返回
- 引入 Flyway，新增基线迁移脚本（`db/migration/V1__baseline_schema.sql`）
- 修复 CORS（支持 `5173`、`5174`）

### dev-portal
- 修复路由与 API 层编码损坏问题
- 增加登录失效统一处理（提示 + 跳转登录）
- 从 `dev-portal` 移除审核职责，收敛为开发者侧能力

### ops-portal（新增）
- 新增独立项目 `ops-portal`
- 独立端口 `5174`
- 实现管理员登录、审核列表、通过/拒绝
- 新增审核原因录入与审计日志页面

## 当前可用链路
- 开发者后台：`http://localhost:5173`
- 运营后台：`http://localhost:5174`
- 后端：`http://localhost:8080/api/v1`
- 审核链路：登录 -> 查看待审 -> 通过/拒绝（需管理员）

## 构建验证
- `mvn -U -f backend/pom.xml clean package -DskipTests`：通过
- `npm run build:dev-portal`：通过
- `npm run build:ops-portal`：通过

## 剩余风险
- CDN 目前为可选配置，生产仍需接入真实 CDN 域名与回源策略
- 异步处理已落地，但上传文件的杀毒/内容安全扫描尚未接入
- Android/iOS 端仍未完成完整真机链路回归

## 下一步建议
1. 为 `dev-portal` 和 `ops-portal` 增加独立 CI/CD 发布流水线
2. 为 Refresh Token 增加设备维度会话管理（多端登出、会话查看）
3. 补齐“上传 -> 审核 -> Android/iOS 下载运行”全链路自动化回归
