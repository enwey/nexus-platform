# DEPLOYMENT（本地部署与生产配置指南）

更新日期：2026-04-03

## 1. 前置依赖

- Node.js 18+
- npm 9+
- JDK 17+
- Maven 3.9+
- Docker Desktop

## 2. 启动基础设施

```powershell
cd D:\WorkSpace\Games\GamesApps\nexus-platform
npm.cmd run infra:up:core
```

检查状态：

```powershell
docker compose -f docker-compose\docker-compose.yml ps
```

## 3. 启动后端

```powershell
. .\scripts\env-local.ps1
mvn -f backend\pom.xml spring-boot:run
```

健康检查：

```powershell
curl.exe http://localhost:8080/api/v1/actuator/health
```

## 4. Portal API 配置

两个后台不再回退到本机 API 地址。必须显式配置以下任一变量：

- `VITE_PLATFORM_API_BASE_URL`
- `VITE_API_BASE_URL`

允许两种形式：

- 绝对地址：`https://api.example.com/api/v1`
- 同域反向代理：`/api/v1`

本地开发默认已通过各 Portal 的 `.env.development` 指向 `http://127.0.0.1:8080/api/v1`。

## 5. 启动两个后台

Windows 一键拉起基础设施、后端、开发者后台、运营后台：

```powershell
.\scripts\start-local.ps1
```

如果你只想注入本地环境变量：

- PowerShell：`.\scripts\env-local.ps1`
- CMD：`call scripts\env-local.cmd`

开发者后台：

```powershell
npm.cmd run dev:portal
```

运营后台：

```powershell
npm.cmd run dev:ops
```

访问地址：

- 开发者后台：`http://localhost:5173`
- 运营后台：`http://localhost:5174`

## 6. Android 真机联调（局域网）

1. 先确认电脑局域网 IP（示例 `192.168.2.11`）
2. 用局域网地址重新编译 APK：

```powershell
$env:PLATFORM_API_BASE_URL='http://192.168.2.11:8080/api/v1'
. .\scripts\env-local.ps1
cmd /c android-client\gradlew.bat -p android-client assembleDebug
```

3. 安装到设备：

```powershell
$adb='C:\Users\7410\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r .\android-client\app\build\outputs\apk\debug\app-debug.apk
```

## 7. 管理员账号

系统默认不再内置弱口令管理员。

如需初始化管理员，请通过环境变量显式提供：

- `PLATFORM_BOOTSTRAP_ADMIN_ENABLED=true`
- `PLATFORM_BOOTSTRAP_ADMIN_USERNAME`
- `PLATFORM_BOOTSTRAP_ADMIN_PASSWORD`
- `PLATFORM_BOOTSTRAP_ADMIN_EMAIL`

本地通过 `scripts/env-local.ps1` 启动时，会注入一组仅限本地开发的管理员配置。
`scripts/env-local.cmd` 也已经和 PowerShell 版本对齐，包含：

- `PLATFORM_ALLOW_INSECURE_DEFAULTS=true`
- `PLATFORM_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174`

## 8. 生产环境必填项

- `SPRING_PROFILES_ACTIVE=prod`
- `PLATFORM_PUBLIC_BASE_URL`
- `PLATFORM_CORS_ALLOWED_ORIGIN_PATTERNS`
- `PLATFORM_CORS_ALLOWED_METHODS`
- `PLATFORM_CORS_ALLOWED_HEADERS`
- `PLATFORM_CORS_ALLOW_CREDENTIALS`
- `PLATFORM_CORS_MAX_AGE_SECONDS`
- `PLATFORM_LEGAL_TERMS_TITLE`
- `PLATFORM_SHARE_ANDROID_INSTALL_URL`
- `PLATFORM_SHARE_IOS_INSTALL_URL`
- `PLATFORM_SHARE_OTHER_INSTALL_URL`
- `PLATFORM_SHARE_APP_SCHEME_TEMPLATE`
- `PLATFORM_LEGAL_PRIVACY_TITLE`
- `PLATFORM_LEGAL_TERMS_HTML`
- `PLATFORM_LEGAL_PRIVACY_HTML`
- `SPRING_MAIL_HOST`
- `PLATFORM_EMAIL_ENABLED=true`
- `SECURITY_JWT_SECRET`
- `PLATFORM_GAME_PACKAGE_MASTER_KEY`
- `PLATFORM_GAME_PACKAGE_RUNTIME_TICKET_SIGNING_KEY`

## 9. 当前关键说明

- 下载链路默认是后端代理流（`/game/download/{appId}`），不再依赖客户端直连 MinIO 预签名地址。
- 如果出现“游戏下载失败”，优先检查该下载接口是否返回 `200`。
- 如果出现前端 `Network error`，优先确认：后端进程、CORS 域名配置、Portal API 环境变量。
- 使用 `docker-compose/docker-compose-prod.yml` 时，建议先准备独立的 `.env.prod`，再执行：

```bash
docker compose --env-file .env.prod -f docker-compose/docker-compose-prod.yml config
docker compose --env-file .env.prod -f docker-compose/docker-compose-prod.yml up -d
```

- `docker compose ... config` 通过，说明生产模板与当前 `SecuritySanityCheck` 的必填项已经对齐。
