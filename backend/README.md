# Backend

## 说明
`backend` 是 Nexus Platform 的核心业务后端，负责用户认证、游戏上传下载、审核流程与基础服务集成。

## 关键能力
- 用户注册/登录/刷新/登出
- 认证机制：JWT Access Token + Refresh Token + Redis 失效控制
- 管理员初始化（默认 `admin/admin123456`）
- 游戏上传、列表、审核、下载
- 上传后异步处理，审核通过后生成预签名下载 URL
- MinIO 对象存储接入（上传时自动创建 bucket）
- 审核日志记录与查询（`/audit/logs`）
- CORS 放行开发/运营双前端（5173/5174）
- Flyway 数据库迁移

## 启动
```bash
mvn -f backend/pom.xml spring-boot:run
```

## 构建
```bash
mvn -f backend/pom.xml clean package -DskipTests
```

## 配置
主要配置在 `src/main/resources/application.yml`。

## 注意事项
- 生产环境请修改 JWT 密钥、管理员初始密码和 MinIO/数据库凭据
- 建议生产启用 HTTPS 与 CDN 下载域名
- 社交相关 Nakama 接口目前为稳定占位实现
