# Nexus 小游戏引擎推荐

更新日期：2026-04-03

## 第一梯队（优先推荐）

1. Cocos Creator
2. LayaAir
3. Egret

特点：微信小游戏迁移成本最低，生态成熟，文档与模板优先覆盖。

## 第二梯队（可支持）

1. Unity WebGL
2. Godot Web Export
3. Unreal HTML5（实验性）

特点：可支持，但需重点关注包体、内存和加载速度。

## 第三梯队（轻量快速迭代）

1. Phaser 3 / PixiJS
2. Three.js / Babylon.js
3. Construct 3 / Defold

特点：适合轻量/活动型项目，接入快。

## 平台统一要求

- ZIP 根目录含 `index.html`
- 资源相对路径
- UTF-8 编码
- 支持 `wx` 兼容接口
- 建议实现 `UpdateManager` 更新提示

## 对外建议文案

Nexus Platform 支持主流小游戏引擎无缝接入。开发者只需导出 Web/HTML5 包并上传，即可完成发布与运行。
