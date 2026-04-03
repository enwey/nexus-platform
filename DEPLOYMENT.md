# DEPLOYMENT（本地部署指南）

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

## 4. 启动两个后台

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

## 5. Android 真机联调（局域网）

1. 先确认电脑局域网 IP（示例 `192.168.2.11`）
2. 用局域网地址重新编译 APK：

```powershell
$env:BACKEND_BASE_URL='http://192.168.2.11:8080/api/v1'
. .\scripts\env-local.ps1
cmd /c android-client\gradlew.bat -p android-client assembleDebug
```

3. 安装到设备：

```powershell
$adb='C:\Users\7410\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r .\android-client\app\build\outputs\apk\debug\app-debug.apk
```

## 6. 默认账号

初始化管理员：

- 用户名：`admin`
- 密码：`admin123456`

如需修改请更新 `backend/src/main/resources/application.yml`。

## 7. 当前关键说明

- 下载链路默认是后端代理流（`/game/download/{appId}`），不再依赖客户端直连 MinIO 预签名地址。
- 如果出现“游戏下载失败”，优先检查该下载接口是否返回 `200`。
- 如果出现前端 `Network error`，优先确认：后端进程、CORS 端口（5173/5174）、API_BASE_URL。
