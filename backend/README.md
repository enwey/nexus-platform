# Backend

更新日期：2026-04-03

## 模块职责

`backend` 提供账号、游戏上传/审核/分发、运营配置、发现与游戏库数据接口。

## 当前关键能力

- 认证：JWT Access + Refresh Token（Redis 会话控制）
- 游戏上传与版本管理
- 审核流程与审计日志
- 下载分发：后端代理流下载（默认）
- 发现页与游戏库数据接口
- 运营配置接口：发现运营配置、游戏运营资料
- 版本检查与强更基础能力

## 启动

```bash
mvn -f backend/pom.xml spring-boot:run
```

## 编译

```bash
mvn -f backend/pom.xml -DskipTests compile
```

## 健康检查

- `GET /api/v1/actuator/health`

## 配置说明

配置文件：`backend/src/main/resources/application.yml`

生产环境至少需替换：

- JWT 密钥
- 默认管理员密码
- 数据库/Redis/MinIO 凭据
- 公网域名配置
