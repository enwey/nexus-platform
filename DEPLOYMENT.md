# Nexus Platform 部署指南（2026-03-26）

## 1. 目标
本文档用于让新成员在 Windows 本地一次性完成部署，并明确开发者后台与运营后台的独立启动方式。

## 2. 当前架构
- 后端：Spring Boot（`backend`）
- 开发者后台：Vue（`dev-portal`，端口 `5173`）
- 运营后台：Vue（`ops-portal`，端口 `5174`）
- 基础设施：PostgreSQL / Redis / MinIO / Nakama（`docker-compose`）

## 3. 前置依赖
- Node.js 18+
- npm 9+
- Java 17+
- Maven 3.8+
- Docker Desktop

## 4. Windows 快速部署（推荐）
### 4.1 初始化本地 Java/Maven（可选）
如果系统未安装 Java/Maven，可使用仓库脚本和本地工具目录：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-env.ps1
```

### 4.2 启动基础设施
优先使用核心依赖启动（不构建 backend 镜像，避免 Docker Hub 被限制时阻塞）：

```powershell
cd D:\WorkSpace\Games\GamesApps\nexus-platform
npm.cmd run infra:up:core
```

如网络可访问 Docker Hub，也可使用：

```powershell
npm.cmd run infra:up
```

### 4.3 启动后端
```powershell
. D:\WorkSpace\Games\GamesApps\nexus-platform\scripts\env-local.ps1
mvn -f D:\WorkSpace\Games\GamesApps\nexus-platform\backend\pom.xml spring-boot:run
```

### 4.4 启动开发者后台
```powershell
cd D:\WorkSpace\Games\GamesApps\nexus-platform
npm.cmd run dev:portal
```

访问：`http://localhost:5173`

### 4.5 启动运营后台
首次启动需要安装依赖：

```powershell
cd D:\WorkSpace\Games\GamesApps\nexus-platform
npm.cmd install --prefix ops-portal
npm.cmd run dev:ops
```

访问：`http://localhost:5174`

## 5. 默认账号
后端默认会自动初始化管理员：
- 用户名：`admin`
- 密码：`admin123456`

配置位置：`backend/src/main/resources/application.yml`

## 6. 健康检查
```powershell
docker compose -f docker-compose/docker-compose.yml ps
curl.exe http://localhost:8080/api/v1/actuator/health
```

CORS 验证（运营后台）：
```powershell
curl.exe -i -X OPTIONS "http://localhost:8080/api/v1/user/login" -H "Origin: http://localhost:5174" -H "Access-Control-Request-Method: POST"
```

## 7. 登录与鉴权说明（更新）
- 登录成功返回 `token`（access token）和 `refreshToken`
- access token 用于业务 API 鉴权，过期后使用 refresh token 调用 `/user/refresh`
- `/user/logout` 会使当前 token 失效（Redis denylist / refresh token 删除）
- 建议前端在收到 401 后执行：尝试刷新 -> 刷新失败则清理登录态并跳转登录页

## 8. 上传/下载链路说明（更新）
- 上传后先进入 `PROCESSING`，由异步任务处理完成后转为 `PENDING`
- 审核通过后提供预签名下载 URL
- backend 仅做控制面，文件流由对象存储直连下载

## 9. 常见问题
### 9.1 Docker Desktop is unable to start
- 先确保 Docker Desktop 已完全启动为 `Running`
- 重新执行 `docker ps` 确认引擎可用

### 9.2 Docker Hub 无法访问（auth.docker.io 超时）
- 使用 `npm.cmd run infra:up:core`（避免构建 backend 镜像）
- backend 改为本机 `mvn spring-boot:run`

### 9.3 前端提示 CORS
- 确认后端已重启到最新代码（CORS 已放行 `5173/5174`）

### 9.4 上传时报 The specified bucket does not exist
- 后端已新增自动建桶逻辑
- 如需手工创建：
```powershell
docker exec nexus-minio mc mb --ignore-existing local/nexus-games
```

### 9.5 审核操作 401
- access token 过期时应走 refresh 流程
- refresh 失败时需要重新登录

## 10. 本次更新摘要（2026-03-26）
- 新增独立运营后台 `ops-portal`
- 开发者后台移除审核路由，职责拆分完成
- 新增根脚本：`dev:ops`、`build:ops-portal`、`infra:up:core`
- 后端 CORS 放行 5173/5174
- 上传逻辑升级为异步处理 + 预签名下载
- 登录体系升级为 JWT + Refresh Token + Redis 失效控制
- 引入 Flyway、分页接口与审核状态机约束
