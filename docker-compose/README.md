# Docker Compose

## 说明
`docker-compose` 目录用于本地基础设施编排。

## 服务
- PostgreSQL
- Redis
- MinIO
- Nakama
- backend（可选，依赖镜像构建）

## 常用命令（仓库根目录）
```bash
npm run infra:up
npm run infra:up:core
npm run infra:logs
npm run infra:down
```

## 推荐
在受限网络环境中优先使用 `infra:up:core`，后端改为本机 `mvn spring-boot:run`。
