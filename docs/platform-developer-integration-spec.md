# 平台开发者接入规范（上传、审核、版本回滚）

更新日期：2026-04-03

## 1. 适用范围

适用于导出为 Web/HTML5 的小游戏项目（Cocos、LayaAir、Egret、Unity WebGL、Godot、Phaser/PixiJS 等）。

## 2. 上传包规范

### 必选要求

- ZIP 根目录必须包含 `index.html`
- ZIP 根目录必须包含 `manifest.json`
- 资源路径必须是相对路径
- 文本资源统一 UTF-8
- 不依赖本地 Node 进程

### 推荐目录

```text
my-game.zip
├── index.html
├── manifest.json
├── assets/
├── js/ 或 dist/
└── ...
```

### `manifest.json` 最小结构

```json
{
  "name": "Demo Game",
  "description": "Hosted mini game package",
  "entry": "index.html",
  "kind": "html5-mini-game"
}
```

约束：

- `entry` 必须指向 ZIP 内真实存在的启动文件
- `kind` 当前仅允许：`html5-mini-game`、`html5-mini-app`、`html5`
- 开发者上传阶段可不填写 `appId`、`version`，平台会在生成运行包时注入并规范化这两个字段

## 3. 开发者上传流程

1. 登录开发者后台
2. 上传包含 `manifest.json` 的 ZIP 并填写基础信息
3. 生成游戏条目与版本草稿
4. 提交审核

## 4. 审核与发布

### 状态建议

- `DRAFT`
- `PENDING`（待审）
- `APPROVED`
- `REJECTED`

### 审核要点

- 可启动性（首屏可见）
- 资源与路径完整性
- 文案编码与国际化
- 胶囊安全区适配

## 5. 更新与强更

- 普通更新：可后台下载、下次重启生效
- 强制更新：若不兼容旧版本，需阻断旧包启动并强制下载新包

对应能力：

- `GET /game/check-update`
- `wx.getUpdateManager()`

## 6. 版本回滚

回滚规则：

1. 只能回滚到历史已通过版本
2. 必须记录回滚原因
3. 回滚后以新的活跃版本为准

## 7. 提审清单

1. ZIP 根目录有 `index.html`
2. ZIP 根目录有合法的 `manifest.json`
3. 运行无白屏/无乱码
4. 顶部安全区无遮挡
5. 弱网失败有重试
6. 更新提示流程可验证

## 8. 常见问题

### 上传成功但运行 404

通常是 ZIP 层级错误或资源绝对路径导致。

### 真机下载失败

优先检查平台下载接口：`GET /game/download/{appId}` 是否返回 `200`。

### 发现页不显示

确认游戏已审核通过，且分类/运营配置中存在可展示数据。

## 9. 安全区与背景适配（必做）

目标：

- 让开发者可以完整控制游戏背景视觉。
- 确保交互内容不被宿主胶囊遮挡。

### 9.1 统一布局原则

1. 背景层（Background Layer）全屏绘制，可覆盖状态栏区域。
2. 内容层（Content Layer）必须从 `safeArea.top` 开始布局。
3. 禁止写死顶部偏移（如 `top=20/44`），必须读取运行时安全区。

### 9.2 推荐接入 API

- `wx.getSystemInfoSync()`：读取 `safeArea` / `windowWidth` / `windowHeight`
- `wx.getMenuButtonBoundingClientRect()`：读取胶囊矩形
- `wx.nexusLayout.getSafeArea()`：SDK 提供的统一安全区计算
- `wx.nexusLayout.getGameViewport()`：SDK 提供的内容视口
- `wx.nexusLayout.applyCanvasSafeArea(canvas)`：SDK Canvas 快速适配

### 9.3 最小示例（JS）

```js
const viewport = wx.nexusLayout.getGameViewport()

// 背景层：全屏
renderBackground({
  x: 0,
  y: 0,
  width: window.innerWidth,
  height: window.innerHeight
})

// 内容层：避开胶囊安全区
contentRoot.x = viewport.x
contentRoot.y = viewport.y
contentRoot.width = viewport.width
contentRoot.height = viewport.height
```

### 9.4 提审前必检

1. 顶部按钮、标题、分数条不与胶囊区域重叠。
2. 背景可延伸到状态栏，不出现割裂色块。
3. 横竖屏切换后，安全区重新计算并生效。
4. 至少在一台刘海屏机型和一台常规机型通过验证。
