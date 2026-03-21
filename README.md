# Nexus Platform

## 背景

Nexus Platform 是一个多端协同的小游戏运行与分发平台仓库。

这个仓库不是单一服务仓库，而是把以下几个部分放在同一个工作区里统一维护：

- 原生宿主：Android / iOS
- 游戏侧桥接 SDK：mock-sdk
- 平台后端：backend
- 开发者后台：dev-portal
- 本地基础设施：docker-compose
- 仓库级契约与检查脚本：contracts / scripts

## 功能

当前仓库主要承载以下能力：

- 游戏运行时桥接能力定义与校验
- 开发者后台与后端接口联调
- Android / iOS 原生宿主对 Web 游戏的承载
- 本地基础设施统一启动
- 仓库级契约检查与类型检查

## 目录

```text
nexus-platform/
├─ backend/           后端服务
├─ dev-portal/        开发者后台
├─ mock-sdk/          游戏桥接 SDK
├─ android-client/    Android 客户端
├─ ios-client/        iOS 客户端
├─ docker-compose/    本地基础设施编排
├─ contracts/         契约定义
├─ scripts/           检查脚本
└─ README.md          仓库说明
```

## 依赖

- Node.js 18+
- npm 9+
- Java 17+
- Maven 3.8+
- Docker / Docker Compose
- Android Studio（Android 开发时）
- Xcode 15+（iOS 开发时）

## 启动

### 1. 启动基础设施

```bash
npm run infra:up
```

### 2. 启动开发者后台

```bash
npm run dev:portal
```

### 3. 本地运行后端

```bash
mvn -f backend/pom.xml spring-boot:run
```

## 构建

### 仓库级检查

```bash
npm run check:contracts
npm run check:sdk-types
npm run check:android-setup
```

### 开发者后台构建

```bash
npm run build:dev-portal
```

### 后端构建

```bash
npm run build:backend
```

### 后端测试

```bash
npm run test:backend
```

## 联调

推荐的本地联调顺序：

1. 启动基础设施：`npm run infra:up`
2. 启动后端：`mvn -f backend/pom.xml spring-boot:run`
3. 启动开发者后台：`npm run dev:portal`
4. 运行仓库级检查：`npm run check:contracts`
5. 再分别联调 Android / iOS 宿主

涉及跨项目变更时：

- 新增桥接 API：先改 `contracts/bridge-api.json`
- 新增 dev-portal 调用的后端接口：先改 `contracts/backend-api.json`

## 已知问题

- Android 工程仍缺少 `gradle-wrapper.jar`，还不能直接通过 wrapper 构建。
- iOS 当前不提供同步原生桥接，同步 API 应优先在 Android 端使用。
- `dev-portal` 与 `mock-sdk` 在当前环境下执行 Vite build 时，可能因 `spawn EPERM` 失败。
- 仓库仍处于平台收束阶段，各端 UI 与业务完整性还没有全部恢复。
