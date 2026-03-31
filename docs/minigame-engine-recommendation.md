# Nexus 小游戏引擎推荐框架

## 目标

为开发者提供一套统一、可执行、可评估的引擎选型标准，确保以下目标同时成立：

- 接入成本低：尽量不改现有微信小游戏代码
- 发布效率高：导出 Web/HTML5 后可直接打包上传
- 运行体验稳：Android/iOS WebView 内可稳定运行
- 可运维：版本更新、强更、灰度与回滚可落地

## 三梯队推荐

### 第一梯队（默认推荐，优先引入）

1. Cocos Creator（2D/3D 综合能力最强）
2. LayaAir（性能取向，适合重度 2D/轻中度 3D）
3. Egret（存量项目迁移价值高）

推荐策略：

- 平台文档、模板、示例工程优先覆盖第一梯队
- 商务与生态合作优先面向第一梯队开发者
- 测试基线（兼容性回归）必须包含第一梯队

### 第二梯队（可支持，按需优化）

1. Unity WebGL
2. Godot Web Export
3. Unreal Engine HTML5（仅实验性/不推荐商业化移动端）

推荐策略：

- 标记为“高级接入”
- 强制给出包体与性能约束说明
- 对 Unity/Godot 提供专门 FAQ（内存、Wasm、压缩）

### 第三梯队（轻量生态，鼓励创新）

1. Phaser 3 / PixiJS
2. Three.js / Babylon.js
3. Construct 3 / Defold

推荐策略：

- 提供快速上手模板
- 强调“低门槛、快迭代、小包体”
- 引导接入平台 API（登录、存储、网络、更新）

## 平台统一技术基线

所有引擎都遵循同一发布标准：

1. ZIP 根目录必须存在 `index.html`
2. 资源路径使用相对路径
3. 不依赖 Node.js 运行时
4. 首屏可在 3 秒内完成可交互（建议目标）
5. 支持 `wx` 兼容接口（通过 Nexus Mock SDK）
6. 推荐实现 `wx.getUpdateManager()` 更新提示流程

## 选型决策表

| 场景 | 推荐引擎 | 原因 |
| --- | --- | --- |
| 微信小游戏迁移 | Cocos / Laya / Egret | API 与项目结构迁移成本最低 |
| 3D 中重度玩法 | Unity WebGL / Laya 3D | 工具链成熟，开发效率高 |
| 独立开发者超休闲 | Phaser / Construct | 包体小、开发快、迭代快 |
| 前端团队快速试错 | Pixi / Phaser / Three.js | 前端工程体系可直接复用 |

## 开发者文档入口建议

建议在开发者后台与官网统一展示：

1. `引擎选型指南`（本文件）
2. `按引擎接入文档`（见 `docs/minigame-engine-guides.md`）
3. `通用上传规范`（ZIP 结构、包体限制、审核要求）
4. `调试与故障排查`（白屏、资源 404、Wasm 加载失败、编码问题）

## 对外宣发文案（可直接使用）

Nexus Platform 支持主流小游戏引擎无缝接入。  
无论您使用 Cocos Creator、LayaAir、Egret、Unity WebGL、Godot，还是 Phaser/PixiJS，均可通过 Web/HTML5 导出后直接上传发布。  
保持原有游戏逻辑，接入成本接近零，最快当天完成迁移上线。
