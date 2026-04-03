# Nexus Platform

更新日期：2026-04-03

Nexus Platform 是一个小游戏平台工程仓库，包含：后端、开发者后台、运营后台、Android 宿主、iOS 宿主与 Mock SDK。

## 当前仓库结构

- `backend`：Spring Boot 后端（默认 `http://localhost:8080/api/v1`）
- `dev-portal`：开发者后台（默认 `http://localhost:5173`）
- `ops-portal`：运营后台（默认 `http://localhost:5174`）
- `android-client`：Android 宿主与运行时
- `ios-client`：iOS 宿主
- `mock-sdk`：`wx` 兼容 API SDK
- `docker-compose`：PostgreSQL / Redis / MinIO / Nakama
- `docs`：平台文档与引擎接入文档

## 一键常用命令（仓库根目录）

```bash
npm run infra:up:core
npm run dev:portal
npm run dev:ops
npm run build:dev-portal
npm run build:ops-portal
```

后端启动：

```bash
.\scripts\env-local.ps1
mvn -f backend\pom.xml spring-boot:run
```

## 推荐本地联调顺序

1. 启动基础设施：`npm run infra:up:core`
2. 启动后端：`mvn -f backend/pom.xml spring-boot:run`
3. 启动开发者后台：`npm run dev:portal`
4. 启动运营后台：`npm run dev:ops`
5. Android 真机联调时，重新编译 APK 并注入局域网后端地址（见 `android-client/README.md`）

## 当前能力概览

- 开发者端：上传游戏、查看游戏、提审版本、查看版本
- 运营端：审核、审计日志、发现页运营配置、运行时运营控制台
- 后端：JWT + Refresh、下载流代理（默认）、版本检查、发现页真实数据、运营配置接口
- Android：游客可用、登录后同步行为数据、发现页/游戏库真实数据、运行页胶囊区适配、UpdateManager 基础能力

## 文档入口

- [部署指南](DEPLOYMENT.md)
- [进度记录](PROGRESS.md)
- [任务计划](plan.md)
- [项目任务拆解](docs/project-task-plan.md)
- [平台开发者接入规范](docs/platform-developer-integration-spec.md)
- [引擎推荐](docs/minigame-engine-recommendation.md)
- [多引擎接入手册](docs/minigame-engine-guides.md)
