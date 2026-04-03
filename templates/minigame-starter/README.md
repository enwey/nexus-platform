# Mini Game Starter（Nexus Platform）

更新日期：2026-04-03

本模板用于开发者快速产出可上传到 Nexus Platform 的小游戏 ZIP 包。

## 目标

- ZIP 根目录含 `index.html`
- 平台相关逻辑集中在 `src/platform`
- 游戏逻辑与平台逻辑解耦

## 目录

```text
minigame-starter/
├── index.html
├── manifest.json
├── src/
├── assets/
├── config/
└── scripts/
```

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
