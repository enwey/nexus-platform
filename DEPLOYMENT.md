# Nexus Platform 本地部署指南

本指南将帮助你在本地快速启动和部署整个 Nexus Platform 项目。

## 📋 前置要求

### 系统要求
- **操作系统**: Windows 10/11, macOS 10.11+, Linux (Ubuntu 20.04+)
- **内存**: 至少 8GB RAM
- **磁盘空间**: 至少 20GB 可用空间
- **网络**: 稳定的网络连接

### 软件要求
- **Docker Desktop**: 4.20.0+ (推荐)
  - 下载地址: https://www.docker.com/products/docker-desktop/
- **Docker Compose**: 2.20.0+ (通常随 Docker Desktop 自动安装)
- **Git**: 用于克隆项目（可选）
- **浏览器**: 用于访问开发者后台和监控面板
- **代码编辑器**: VS Code / IntelliJ IDEA / Android Studio / Xcode

---

## 🚀 快速开始

### 方式一：使用 Docker Desktop（推荐）

#### 1. 克隆项目
```bash
# 如果你有 Git 仓库
git clone <repository-url>
cd nexus-platform

# 或者直接使用本地项目
cd nexus-platform
```

#### 2. 启动开发环境
```bash
# 进入 docker-compose 目录
cd nexus-platform/docker-compose

# 启动所有服务（开发环境）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

#### 3. 初始化 Nakama 数据库
```bash
# 等待 PostgreSQL 启动完成（约 10 秒）
sleep 15

# 初始化 Nakama 数据库
chmod +x init-nakama.sh
./init-nakama.sh
```

#### 4. 验证服务状态
```bash
# 检查所有服务是否正常运行
docker-compose ps

# 检查服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost:7351
curl http://localhost:9000/minio/health/live
curl http://localhost:6379
```

#### 5. 访问服务
- **开发者后台**: http://localhost:5173
- **后端 API**: http://localhost:8080/api/v1
- **Prometheus 监控**: http://localhost:9090
- **Grafana 仪表板**: http://localhost:3000 (默认账号: admin / password: admin)
- **MinIO 控制台**: http://localhost:9000 (默认账号: minioadmin / minioadmin)

---

### 方式二：使用命令行 Docker（Linux/macOS）

#### 1. 安装 Docker 和 Docker Compose
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install docker.io docker-compose-plugin

# macOS (使用 Homebrew）
brew install docker
brew install docker-compose

# 验证安装
docker --version
docker-compose --version
```

#### 2. 启动服务
```bash
cd nexus-platform/docker-compose

# 启动所有服务
docker-compose up -d

# 后台运行（生产环境）
docker-compose -f docker-compose-prod.yml up -d
```

#### 3. 初始化 Nakama
```bash
# 等待 PostgreSQL 启动完成
sleep 15

# 初始化 Nakama
chmod +x init-nakama.sh
./init-nakama.sh
```

---

## 📊 服务说明

### PostgreSQL
- **端口**: 5432
- **数据库**: nexus_platform
- **用户名**: nexus
- **密码**: nexus_password
- **数据卷**: postgres (持久化数据）

### Nakama 游戏服务器
- **API 端口**: 7350
- **控制台端口**: 7351
- **数据库**: nexus_platform (共享 PostgreSQL）
- **默认凭据**:
  - Server Key: defaultkeychanged
  - HTTP Key: defaulthttpkeychanged
  - Admin Email: admin@example.com
  - Admin Password: password

### MinIO 对象存储
- **API 端口**: 9000
- **控制台端口**: 9001
- **Bucket**: nexus-games
- **默认凭据**:
  - Access Key: minioadmin
  - Secret Key: minioadmin

### Redis 缓存
- **端口**: 6379
- **无密码**: 开发环境

### Backend 后端服务
- **API 端口**: 8080
- **上下文路径**: /api/v1
- **健康检查**: http://localhost:8080/actuator/health

### 开发者后台
- **端口**: 5173
- **技术栈**: Vue 3 + Vite

### Prometheus 监控
- **端口**: 9090
- **目标**:
  - Backend: http://localhost:8080/actuator/prometheus
  - PostgreSQL: postgres:5432
  - Redis: redis:6379
  - Nakama: nakama:7351

### Grafana 可视化
- **端口**: 3000
- **默认账号**: admin / password: admin
- **数据源**: Prometheus (http://localhost:9090)

---

## 🔧 配置说明

### 修改默认密码（生产环境必须）

#### PostgreSQL
编辑 `docker-compose/docker-compose.yml`:
```yaml
postgres:
  environment:
    POSTGRES_PASSWORD: your_secure_password
```

#### Nakama
编辑 `docker-compose/akama.yml`:
```yaml
NAKAMA_SERVER_KEY: your_32_char_hex_key
NAKAMA_HTTP_KEY: your_32_char_hex_key
NAKAMA_ADMIN_EMAIL: your_admin@example.com
NAKAMA_ADMIN_PASSWORD: your_secure_password
```

#### MinIO
编辑 `docker-compose/docker-compose.yml`:
```yaml
minio:
  environment:
    MINIO_ROOT_USER: your_admin_user
    MINIO_ROOT_PASSWORD: your_secure_password
```

### 环境变量配置

#### Backend
编辑 `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    password: your_secure_password
  minio:
    access-key: your_admin_user
    secret-key: your_secure_password
  nakama:
    server-key: your_32_char_hex_key
    http-key: your_32_char_hex_key
```

---

## 📱 开发者后台使用

### 1. 启动开发服务器
```bash
cd nexus-platform/dev-portal
npm install
npm run dev
```

### 2. 访问开发者后台
打开浏览器访问: http://localhost:5173

### 3. 注册开发者账号
- 点击"注册"按钮
- 填写用户名、密码、邮箱
- 完成注册后登录

### 4. 上传游戏
- 登录后进入"我的游戏"
- 点击"上传游戏"
- 选择 ZIP 文件
- 填写游戏信息
- 点击"上传"

### 5. 管理游戏
- 查看游戏列表
- 查看游戏状态（草稿/待审核/已通过/已拒绝）
- 删除游戏
- 编辑游戏信息

---

## 🎮 游戏开发指南

### 1. 准备游戏 ZIP 包
游戏 ZIP 包应包含以下结构：
```
game.zip
├── index.html          # 游戏入口
├── game.js            # 游戏逻辑
├── assets/            # 游戏资源
│   ├── images/
│   ├── audio/
│   └── data/
└── game.json          # 游戏配置
```

### 2. 使用 Mock SDK
游戏代码中可以直接使用 `wx` 对象：
```javascript
wx.login({
  success: (res) => {
    console.log('登录成功', res)
  }
})

wx.getSystemInfo({
  success: (res) => {
    console.log('系统信息', res)
  }
})

wx.setStorage({
  key: 'playerData',
  data: { score: 100, level: 1 },
  success: () => {
    console.log('存储成功')
  }
})

wx.getStorage({
  key: 'playerData',
  success: (res) => {
    console.log('读取存储', res.data)
  }
})
```

### 3. 使用 Nakama 社交功能
```javascript
wx.getFriendCloudStorage({
  success: (res) => {
    console.log('云存档', res.data)
  }
})

wx.setUserCloudStorage({
  key: 'playerData',
  value: { score: 100, level: 1 },
  success: () => {
    console.log('云存档成功')
  }
})

wx.createLeaderboard({
  leaderboardId: 'high_scores',
  success: () => {
    console.log('创建排行榜')
  }
})

wx.getLeaderboard({
  leaderboardId: 'high_scores',
  limit: 10,
  success: (res) => {
    console.log('排行榜', res.data)
  }
})
```

---

## 🔍 故障排查

### 服务无法启动

#### 1. 检查端口占用
```bash
# Windows
netstat -ano | findstr :5432
netstat -ano | findstr :7350
netstat -ano | findstr :7351
netstat -ano | findstr :9000
netstat -ano | findstr :9001
netstat -ano | findstr :6379
netstat -ano | findstr :8080
netstat -ano | findstr :5173
netstat -ano | findstr :9090
netstat -ano | findstr :3000

# Linux/macOS
lsof -i :5432
lsof -i :7350
lsof -i :9000
lsof -i :6379
lsof -i :8080
lsof -i :5173
lsof -i :9090
lsof -i :3000
```

如果端口被占用，停止占用进程或修改端口配置。

#### 2. 检查 Docker 状态
```bash
docker ps
docker-compose ps
```

#### 3. 查看服务日志
```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs postgres
docker-compose logs nakama
docker-compose logs minio
docker-compose logs backend

# 查看最后 100 行日志
docker-compose logs --tail=100 postgres
```

#### 4. 检查磁盘空间
```bash
# Windows
dir

# Linux/macOS
df -h

# Docker 磁盘使用
docker system df
```

### 数据库连接失败

#### 1. 检查 PostgreSQL 是否正常运行
```bash
docker exec nexus-postgres pg_isready
```

#### 2. 测试数据库连接
```bash
docker exec -it nexus-postgres psql -U nexus -d nexus_platform
```

#### 3. 检查数据库凭据
确保 `docker-compose/docker-compose.yml` 中的凭据正确。

### Nakama 无法连接

#### 1. 检查 Nakama 是否正常运行
```bash
docker exec nexus-nakama /nakama/nakama check
```

#### 2. 查看 Nakama 日志
```bash
docker-compose logs nakama
```

#### 3. 重新初始化 Nakama
```bash
docker exec -i nexus-nakama /nakama/nakama migrate reset
./init-nakama.sh
```

### MinIO 无法访问

#### 1. 检查 MinIO 是否正常运行
```bash
curl http://localhost:9000/minio/health/live
```

#### 2. 登录 MinIO 控制台
访问: http://localhost:9000
账号: minioadmin / minioadmin

#### 3. 创建 Bucket
```bash
# 使用 MinIO 客户端或 API
docker exec nexus-minio mc mb nexus-games
```

### 开发者后台无法访问

#### 1. 检查后端服务
```bash
curl http://localhost:8080/actuator/health
```

#### 2. 检查前端服务
```bash
cd nexus-platform/dev-portal
npm run dev
```

#### 3. 清除浏览器缓存
- 清除浏览器缓存和 Cookie
- 尝试使用无痕模式
- 检查浏览器控制台是否有错误

---

## 🎯 性能优化建议

### 1. 资源限制
```yaml
# 编辑 docker-compose/docker-compose.yml，添加资源限制
services:
  postgres:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
```

### 2. 数据库优化
```yaml
# PostgreSQL 连接池配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### 3. 缓存配置
```yaml
# Redis 缓存配置
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600
      cache-null-values: false
```

---

## 🔒 安全加固建议

### 1. 修改所有默认密码
- PostgreSQL 密码
- Nakama Server Key 和 HTTP Key
- Nakama Admin Email 和 Password
- MinIO Access Key 和 Secret Key

### 2. 启用 HTTPS（生产环境）
- 配置反向代理（Nginx/Apache）
- 申请 SSL 证书
- 配置防火墙规则

### 3. 网络隔离
- 使用 Docker 网络
- 配置服务间通信
- 限制外部访问

### 4. 定期备份数据
```bash
# 手动备份
cd nexus-platform/docker-compose
chmod +x backup.sh
./backup.sh

# 定期备份（使用 cron）
# 添加到 crontab
0 2 * * * /path/to/backup.sh
```

---

## 📊 监控和日志

### Prometheus 监控指标
- **系统资源**: CPU、内存、磁盘、网络
- **应用指标**: 请求响应时间、错误率、吞吐量
- **数据库指标**: 连接数、查询时间、慢查询
- **业务指标**: 用户数、游戏数、下载量

### Grafana 仪表板
访问: http://localhost:3000

**推荐仪表板**:
1. **System Overview**: 系统概览
2. **Service Health**: 服务健康状态
3. **Request Metrics**: 请求指标
4. **Database Metrics**: 数据库指标
5. **Business Metrics**: 业务指标

### 日志查看
```bash
# 查看所有服务日志
docker-compose logs -f --tail=100

# 查看特定服务日志
docker logs nexus-postgres
docker logs nexus-backend
```

---

## 🧪 测试

### 运行单元测试
```bash
# Mock SDK 测试
cd nexus-platform/mock-sdk
npm test

# 后端测试
cd nexus-platform/backend
mvn test
```

### 运行集成测试
```bash
# 后端集成测试
cd nexus-platform/backend
mvn verify
```

### API 测试
```bash
# 测试用户注册
curl -X POST http://localhost:8080/api/v1/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","email":"test@example.com"}'

# 测试用户登录
curl -X POST http://localhost:8080/api/v1/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 测试游戏列表
curl http://localhost:8080/api/v1/game/list

# 测试游戏上传
curl -X POST http://localhost:8080/api/v1/game/upload \
  -F "file=@game.zip" \
  -F "name=Test Game" \
  -F "description=Test Description" \
  -F "developerId=1"
```

---

## 📱 移动端测试

### Android 客户端
1. 在 Android Studio 中打开 `android-client` 项目
2. 连接 Android 设备或使用模拟器
3. 运行应用
4. 测试游戏加载和 API 调用

### iOS 客户端
1. 在 Xcode 中打开 `ios-client` 项目
2. 连接 iOS 设备或使用模拟器
3. 运行应用
4. 测试游戏加载和 API 调用

---

## 🎯 常用命令

### 服务管理
```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启特定服务
docker-compose restart backend

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f

# 进入服务容器
docker exec -it nexus-postgres psql -U nexus -d nexus_platform
docker exec -it nexus-backend sh
```

### 数据库管理
```bash
# 备份数据库
docker exec nexus-postgres pg_dump -U nexus nexus_platform > backup.sql

# 恢复数据库
docker exec -i nexus-postgres psql -U nexus -d nexus_platform < backup.sql

# 清空数据库（谨慎使用）
docker exec -i nexus-postgres psql -U nexus -d nexus_platform -c "DROP SCHEMA public CASCADE; DROP SCHEMA public;"

# 查看 PostgreSQL 大小
docker exec nexus-postgres psql -U nexus -d nexus_platform -c "SELECT pg_size_pretty(pg_database_size('nexus_platform'));"

# 查看 PostgreSQL 连接数
docker exec nexus-postgres psql -U nexus -d nexus_platform -c "SELECT count(*) FROM pg_stat_activity;"
```

### MinIO 管理
```bash
# 列出所有 buckets
docker exec nexus-minio mc ls

# 列出 bucket 内容
docker exec nexus-minio mc ls nexus-games

# 上传文件到 MinIO
docker exec nexus-minio mc cp localfile.txt nexus-games/

# 下载文件从 MinIO
docker exec nexus-minio mc cp nexus-games/remotefile.txt localfile.txt

# 删除文件
docker exec nexus-minio mc rm nexus-games/file.txt

# 设置 bucket 策略
docker exec nexus-minio mc anonymous set download nexus-games
```

### Nakama 管理
```bash
# 查看 Nakama 状态
docker exec nexus-nakama /nakama/nakama check

# 导出 Nakama 数据
docker exec nexus-nakama /nakama/nakama migrate export > export.sql

# 重置 Nakama 数据库
docker exec nexus-nakama /nakama/nakama migrate reset

# 查看 Nakama 日志
docker logs nexus-nakama
```

---

## 🚀 快速启动脚本

### Windows (PowerShell)
```powershell
# 快速启动脚本
$scriptPath = "$PSScriptRoot\start-nexus.ps1"

if (-Not (Test-Path $scriptPath)) {
    Write-Host "Starting Nexus Platform..."
    
    Set-Location nexus-platform/docker-compose
    docker-compose up -d
    
    Start-Sleep -Seconds 15
    
    Write-Host "Initializing Nakama..."
    & "$PSScriptRoot\init-nakama.ps1"
    
    Write-Host "Services started successfully!"
    Write-Host ""
    Write-Host "Access URLs:"
    Write-Host "  - Developer Portal: http://localhost:5173"
    Write-Host "  - Backend API: http://localhost:8080/api/v1"
    Write-Host "  - Grafana: http://localhost:3000"
    Write-Host "  - MinIO Console: http://localhost:9000"
    
    Write-Host "Press any key to stop services..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    if ($null.KeyChar -eq 27) { # ESC key
        docker-compose down
    }
}
```

### Linux/macOS (Bash)
```bash
#!/bin/bash
# 快速启动脚本
cd nexus-platform/docker-compose

echo "Starting Nexus Platform..."

# 启动所有服务
docker-compose up -d

# 等待服务启动
echo "Waiting for services to start..."
sleep 15

# 初始化 Nakama
echo "Initializing Nakama..."
chmod +x init-nakama.sh
./init-nakama.sh

echo "Services started successfully!"
echo ""
echo "Access URLs:"
echo "  - Developer Portal: http://localhost:5173"
echo "  - Backend API: http://localhost:8080/api/v1"
echo "  - Grafana: http://localhost:3000"
echo "  - MinIO Console: http://localhost:9000"
echo ""
echo "Press Ctrl+C to stop services..."

# 捕获退出信号，停止所有服务
trap 'docker-compose down' EXIT
```

---

## 📚 学习资源

### 官方文档
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Nakama](https://heroiclabs.com/docs)
- [MinIO](https://min.io/docs/minio/linux/reference/)
- [Docker](https://docs.docker.com/)
- [Vue 3](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)

### 项目文档
- [plan.md](../plan.md) - 完整开发计划
- [PROGRESS.md](../nexus-platform/PROGRESS.md) - 项目进度报告
- [mock-sdk/README.md](../nexus-platform/mock-sdk/README.md) - SDK 文档
- [android-client/README.md](../nexus-platform/android-client/README.md) - Android 文档
- [ios-client/README.md](../nexus-platform/ios-client/README.md) - iOS 文档
- [backend/README.md](../nexus-platform/backend/README.md) - 后端文档
- [dev-portal/README.md](../nexus-platform/dev-portal/README.md) - 开发者后台文档
- [docker-compose/README.md](../nexus-platform/docker-compose/README.md) - Docker 部署文档

---

## 🎉 开始使用

### 1. 快速启动（推荐）
```bash
cd nexus-platform/docker-compose
docker-compose up -d
```

### 2. 等待服务就绪
等待约 30 秒让所有服务完全启动。

### 3. 访问服务
打开浏览器访问以下 URL：
- **开发者后台**: http://localhost:5173
- **Grafana 监控**: http://localhost:3000

### 4. 开始开发
现在你可以：
1. 在开发者后台注册账号
2. 上传游戏 ZIP 包
3. 测试游戏加载
4. 使用社交功能

---

## 💡 提示

1. **首次启动**建议先启动 PostgreSQL，等待 10 秒后再启动其他服务
2. **Nakama 初始化**只需要执行一次，之后会自动同步
3. **端口冲突**如果端口被占用，请先停止占用进程
4. **内存不足**如果遇到内存不足，可以减少 Docker 资源限制
5. **磁盘空间**确保有足够的磁盘空间存储数据和日志

---

## 📞 获取帮助

如果遇到问题，请：
1. 查看服务日志：`docker-compose logs -f`
2. 检查服务状态：`docker-compose ps`
3. 参考故障排查部分
4. 查看项目文档

---

**祝部署顺利！🚀**
