# Nexus 小游戏多引擎接入手册

更新日期：2026-04-03

## 通用接入步骤

1. 引擎导出 Web/HTML5
2. 确认导出目录根部存在 `index.html`
3. 打包为单个 ZIP
4. 上传开发者后台并提审
5. 审核通过后可在 Android/iOS 宿主运行

## 通用规范

- 入口必须是 `index.html`
- 使用相对路径
- 统一 UTF-8
- 避免依赖本地服务

## 按引擎建议

### Cocos Creator

- 构建目标：`Web Mobile`
- 推荐：开启资源压缩与版本管理

### LayaAir

- 发布目标：`Web`
- 推荐：检查资源哈希与入口引用

### Egret

- 导出 HTML5 后先浏览器回归

### Unity WebGL

- 推荐 `Gzip/Brotli`
- 关注首包体积与内存峰值

### Godot Web

- 根据机型控制线程与资源加载策略

### Phaser / Pixi / Three.js

- 轻量项目优先，适合快迭代

## 最小 API 验证集

1. `wx.getSystemInfoSync()`
2. `wx.getMenuButtonBoundingClientRect()`
3. `wx.request()`
4. `wx.setStorageSync/getStorageSync`
5. `wx.getUpdateManager()`

## 提测前核对

1. 首屏可交互
2. 文案无乱码
3. 胶囊区无遮挡
4. 断网与重试可用
5. 更新提示可触发
