# Docker Compose（本地基础设施）

更新日期：2026-04-03

## 目录说明

本目录用于启动本地基础服务：

- PostgreSQL
- Redis
- MinIO
- Nakama

## 常用命令（仓库根目录执行）

```bash
npm run infra:up:core
npm run infra:up
npm run infra:logs
npm run infra:down
```

## 推荐方式

在网络受限环境中优先使用 `infra:up:core`，后端使用本机 `mvn spring-boot:run`。

## 端口默认值

- Postgres: `5432`
- Redis: `6379`
- MinIO API: `9000`
- MinIO Console: `9001`
- Nakama: 见 compose 映射
