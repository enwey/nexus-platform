# Mini Game Starter（Nexus Platform）

更新日期：2026-04-15

本模板用于开发者快速产出可上传到 Nexus Platform 的小游戏 ZIP 包。

## 目标

- ZIP 根目录含 `index.html`
- 平台相关逻辑集中在 `src/platform`
- 游戏逻辑与平台逻辑解耦

## 目录

```text
minigame-starter/
├── assets/
│   ├── audio/
│   ├── logo/
│   └── sprites/
├── config/
├── index.html
├── manifest.json
└── scripts/
└── src/
    ├── algorithms/
    ├── config/
    ├── game/
    ├── i18n/
    ├── platform/
    ├── scenes/
    ├── systems/
    └── ui/
```

## Manifest 能力

模板支持开发者在 `manifest.json` 中声明游戏展示信息，移动端在小游戏上传并下载到本地后，可以读取这些字段并用于展示。

- `metadata.icon`: 游戏包内 logo 路径，建议使用 PNG / JPG / WebP，放在 `assets/logo/`
- `metadata.defaultLocale`: 默认语言
- `metadata.locales`: 多语言名称与描述

示例：

```json
{
  "name": "Neon Lane",
  "description": "Arcade lane-dodging demo.",
  "metadata": {
    "icon": "assets/logo/game-logo.png",
    "defaultLocale": "zh-CN",
    "locales": {
      "zh-CN": {
        "name": "霓虹闪避",
        "description": "高帧率街机闪避小游戏。"
      },
      "zh-TW": {
        "name": "霓虹閃避",
        "description": "高幀率街機閃避小遊戲。"
      },
      "en": {
        "name": "Neon Lane",
        "description": "Arcade lane-dodging demo."
      }
    }
  }
}
```

Android / iOS 客户端会优先读取已安装小游戏包中的这些字段，用于覆盖默认游戏 logo、名称和描述。

## 打包

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package.ps1
```

输出：`release/minigame-starter.zip`

## 安全包分发

上传到 Nexus Platform 之后，平台不会直接向移动端分发开发者上传的明文 ZIP。

实际线上链路会执行：

1. 平台校验上传 ZIP
2. 服务端生成 `NEXUS_SECURE_ZIP_V1` 安全包
3. 使用随机内容密钥对小游戏源码 ZIP 做 AES-256-GCM 加密
4. Android / iOS 客户端先申请短时 runtime ticket
5. 客户端再带 ticket 与设备 ID 兑换一次性解密密钥
6. 客户端原生层解密后再解压并启动游戏

这意味着：

- CDN / 下载链路拿到的是安全包，而不是可直接阅读的小游戏源码
- 设备本地默认存的是加密外壳包
- 解密发生在 Android / iOS 原生运行时，而不是前端 JS 层
- 服务端会保留开发者原始 ZIP，便于后续升级安全包格式时重新打包

当前推荐运行策略：

- 首次安装联网授权并完成安装
- 安装成功后，本地保留已安装版本
- 后续已安装版本允许离线运行
- 更新、重装、清缓存、换设备时再重新联网

服务端生产环境还需要配置强随机主密钥：

```bash
PLATFORM_GAME_PACKAGE_MASTER_KEY=replace-with-a-strong-random-secret-at-least-32-chars
PLATFORM_GAME_PACKAGE_RUNTIME_TICKET_SIGNING_KEY=replace-with-a-strong-random-secret-at-least-32-chars
```

未替换默认开发密钥时，生产安全检查会拒绝启动。

如果线上走 HTTPS，还可以进一步开启宿主证书指纹校验：

```bash
BACKEND_CERT_SHA256=<backend leaf certificate sha256 hex>
```

## 上传前检查

1. `index.html` 在 ZIP 根目录
2. 资源路径全部相对路径
3. 文本编码 UTF-8
4. 首屏可交互

## 资源建议

- 算法相关逻辑：`src/algorithms/`
- 游戏规则、场景编排：`src/scenes/`
- 可复用系统：`src/systems/`
- UI 文案与多语言：`src/i18n/`、`src/ui/`
- 游戏素材：`assets/logo/`、`assets/sprites/`、`assets/audio/`
