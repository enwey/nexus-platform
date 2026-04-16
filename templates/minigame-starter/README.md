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
