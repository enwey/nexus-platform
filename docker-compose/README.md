# Nexus Platform Docker Compose

Nexus 平台的 Docker Compose 配置，用于快速部署整个系统。

## 服务组件

- PostgreSQL - 数据库
- Nakama - 游戏服务器（社交、排行榜、云存档）
- MinIO - 对象存储
- Redis - 缓存
- Backend - Java 后端服务

## 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 内存
- 至少 20GB 磁盘空间

### 启动所有服务

```bash
cd nexus-platform/docker-compose

# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

### 初始化 Nakama

```bash
# 初始化 Nakama 数据库
chmod +x init-nakama.sh
./init-nakama.sh
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL | 5432 | 数据库 |
| Nakama Console | 7350 | 管理控制台 |
| Nakama API | 7351 | API 接口 |
| MinIO Console | 9000 | 对象存储控制台 |
| MinIO API | 9001 | 对象存储 API |
| Backend | 8080 | 后端 API |
| Redis | 6379 | 缓存 |

## 配置说明

### PostgreSQL

默认凭据：
- 用户名: nexus
- 密码: nexus_password
- 数据库: nexus_platform

### Nakama

默认凭据（生产环境请修改）：
- Server Key: defaultkeychanged
- HTTP Key: defaulthttpkeychanged
- Admin Email: admin@example.com
- Admin Password: password

详细配置请参考 `akama.yml`。

### MinIO

默认凭据：
- Access Key: minioadmin
- Secret Key: minioadmin
- Bucket: nexus-games

访问地址：
- 控制台: http://localhost:9000
- API: http://localhost:9001

### Redis

- 端口: 6379
- 无密码（开发环境）

### Backend

- API 地址: http://localhost:8080/api/v1
- 上下文路径: /api/v1

## 数据持久化

### PostgreSQL 数据卷

```yaml
volumes:
  postgres:
    driver: local
```

数据存储在 Docker 命名卷中。

### MinIO 数据卷

```yaml
volumes:
  minio:
    driver: local
```

游戏文件存储在 Docker 命名卷中。

## 网络配置

所有服务连接到 `nexus-network` 桥接网络。

```yaml
networks:
  nexus-network:
    driver: bridge
```

## 环境变量

### Backend 环境变量

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/nexus_platform
  SPRING_DATASOURCE_USERNAME: nexus
  SPRING_DATASOURCE_PASSWORD: nexus_password
  SPRING_REDIS_HOST: redis
  SPRING_REDIS_PORT: 6379
  MINIO_ENDPOINT: http://minio:9000
  MINIO_ACCESS_KEY: minioadmin
  MINIO_SECRET_KEY: minioadmin
  MINIO_BUCKET_NAME: nexus-games
```

### Nakama 环境变量

```yaml
environment:
  NAKAMA_NAME: mygame
  NAKAMA_SERVER_KEY: defaultkeychanged
  NAKAMA_DATABASE_ADDRESS: postgres:5432
  NAKAMA_DATABASE_USERNAME: nexus
  NAKAMA_DATABASE_PASSWORD: nexus_password
  NAKAMA_DATABASE_NAME: nexus_platform
```

## 常用命令

### 查看服务状态

```bash
docker-compose ps
```

### 查看服务日志

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs postgres
docker-compose logs nakama
docker-compose logs minio
docker-compose logs backend
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart postgres
docker-compose restart nakama
docker-compose restart minio
docker-compose restart backend
```

### 进入服务容器

```bash
# 进入 PostgreSQL 容器
docker exec -it nexus-postgres psql -U nexus -d nexus_platform

# 进入 Nakama 容器
docker exec -it nexus-nakama sh

# 进入 MinIO 容器
docker exec -it nexus-minio sh

# 进入 Backend 容器
docker exec -it nexus-backend sh
```

### 备份数据

```bash
# 备份 PostgreSQL 数据
docker exec nexus-postgres pg_dump -U nexus nexus_platform > backup.sql

# 备份 MinIO 数据
docker exec nexus-minio mc mirror minio/ ./minio-backup
```

### 恢复数据

```bash
# 恢复 PostgreSQL 数据
docker exec -i nexus-postgres psql -U nexus -d nexus_platform < backup.sql

# 恢复 MinIO 数据
docker exec -i nexus-minio mc mirror ./minio-backup minio/
```

## 故障排查

### 服务无法启动

1. 检查端口是否被占用
2. 检查 Docker 是否正常运行
3. 查看服务日志：`docker-compose logs`
4. 检查磁盘空间是否充足

### 数据库连接失败

1. 检查 PostgreSQL 是否正常运行
2. 检查数据库凭据是否正确
3. 检查网络连接

### Nakama 无法连接

1. 检查 Nakama 是否正常运行
2. 检查数据库连接配置
3. 检查端口是否正确
4. 查看 Nakama 日志

### MinIO 无法访问

1. 检查 MinIO 是否正常运行
2. 检查端口是否正确
3. 检查凭据是否正确
4. 查看 MinIO 日志

## 生产环境部署

### 安全建议

1. 修改所有默认密码
2. 使用强密码
3. 配置防火墙规则
4. 启用 HTTPS
5. 定期备份数据

### 性能优化

1. 根据负载调整资源限制
2. 配置数据库连接池
3. 启用 Redis 缓存
4. 配置 CDN 加速

### 监控建议

1. 配置日志收集
2. 设置监控告警
3. 监控服务健康状态
4. 监控资源使用情况

## License

MIT
