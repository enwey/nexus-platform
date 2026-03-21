# Docker Compose

## 背景

`docker-compose` 目录提供 Nexus Platform 本地联调用的基础设施编排。

## 功能

主要包含以下服务：

- PostgreSQL
- Redis
- MinIO
- Nakama
- backend

## 目录

```text
docker-compose/
├─ docker-compose.yml
├─ docker-compose-prod.yml
├─ init-nakama.sh
├─ nakama.yml
├─ prometheus.yml
└─ README.md
```

## 依赖

- Docker
- Docker Compose

## 启动

在仓库根目录执行：

```bash
npm run infra:up
```

## 构建

这个目录本身不负责构建应用代码，主要负责启动和停止本地基础设施。

### 查看日志

```bash
npm run infra:logs
```

### 关闭服务

```bash
npm run infra:down
```

## 联调

推荐与以下项目联动：

- backend
- dev-portal
- android-client
- ios-client

默认暴露端口：

- PostgreSQL：5432
- Redis：6379
- MinIO：9000
- Nakama API：7351
- backend：8080

## 已知问题

- 当前 compose 配置主要面向开发环境。
- 如果修改端口、服务名或容器网络，需要同步更新 backend 配置和环境变量。
