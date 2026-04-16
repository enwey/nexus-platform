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
- 游戏安全包主密钥 `PLATFORM_GAME_PACKAGE_MASTER_KEY`
- 默认管理员密码
- 数据库/Redis/MinIO 凭据
- 公网域名配置

### 游戏安全包

小游戏上传后，后端会把开发者上传的明文 ZIP 重打包为 `NEXUS_SECURE_ZIP_V1` 安全包，再向移动端分发。

服务端会同时保留两份对象：

1. 开发者原始上传 ZIP（source package）
2. 当前分发中的运行时安全包（runtime package）

这样后续切换加密格式时，可以基于原始包重新生成新的安全包，而不需要依赖旧安全包反向迁移。

运行时解密链路采用两段式：

1. 客户端先申请短时 `runtime ticket`
2. 再带上 ticket 与设备 ID 兑换一次性内容密钥

ticket 会绑定当前用户、游戏版本和设备 ID，并在 Redis 中按短 TTL 保存。
新版本宿主还会通过请求头上报自己支持的安全包格式，服务端可以据此决定是否下发当前格式的运行时授权。

生产环境必须设置：

```bash
export PLATFORM_GAME_PACKAGE_MASTER_KEY="replace-with-a-strong-random-secret-at-least-32-chars"
export PLATFORM_GAME_PACKAGE_RUNTIME_TICKET_SIGNING_KEY="replace-with-a-strong-random-secret-at-least-32-chars"
```

如果使用生产配置且仍然保留默认开发密钥，服务启动时会直接失败，避免把弱密钥带到线上。

可选运行时配置：

```bash
export PLATFORM_GAME_PACKAGE_RUNTIME_TICKET_TTL_SECONDS=120
```
