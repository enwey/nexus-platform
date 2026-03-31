# Nexus Platform

## 背景
Nexus Platform 是一个多端协同的小游戏运行与分发平台仓库。

## 子项目
- `backend`：平台后端服务
- `dev-portal`：开发者后台（5173）
- `ops-portal`：运营审核后台（5174）
- `docker-compose`：本地基础设施编排
- `mock-sdk` / `android-client` / `ios-client`：运行时与客户端相关模块

## 一键入口（仓库根目录）
```bash
npm run infra:up
npm run infra:up:core
npm run dev:portal
npm run dev:ops
npm run build:dev-portal
npm run build:ops-portal
npm run build:backend
```

## 推荐联调顺序
1. 启动基础设施：`npm run infra:up:core`
2. 启动后端：`mvn -f backend/pom.xml spring-boot:run`
3. 启动开发者后台：`npm run dev:portal`
4. 启动运营后台：`npm run dev:ops`

## 访问地址
- 开发者后台：`http://localhost:5173`
- 运营后台：`http://localhost:5174`
- 后端健康检查：`http://localhost:8080/api/v1/actuator/health`

## 文档
- 部署教学：`DEPLOYMENT.md`
- 进度：`PROGRESS.md`
- 任务计划：`docs/project-task-plan.md`
- 小游戏引擎推荐：`docs/minigame-engine-recommendation.md`
- 多引擎接入手册：`docs/minigame-engine-guides.md`
- 平台开发者接入规范：`docs/platform-developer-integration-spec.md`


