# Nexus 小游戏多引擎接入文档

## 通用接入流程（所有引擎）

1. 在引擎中导出 `Web Mobile` 或 `HTML5` 版本。
2. 确保导出目录根部有 `index.html`。
3. 将导出目录整体压缩为一个 `.zip`。
4. 在开发者后台上传 ZIP，提交审核。
5. 审核通过后，Android/iOS 客户端即可下载并运行。

## 通用目录与发布规范

### ZIP 结构要求

```text
my-game.zip
├── index.html
├── assets/
├── js/ (或 build/、dist/)
└── manifest.json (可选)
```

### 强制规范

- 入口文件必须是 `index.html`
- 不允许绝对本地路径（如 `C:\`）
- 不允许依赖本地 Node 服务
- 所有资源应使用相对路径
- 建议默认编码为 UTF-8（避免安卓端乱码）

## 第一梯队引擎接入

### 1) Cocos Creator

推荐版本：3.x（2.x 也可迁移）

导出建议：

1. 构建发布目标选择 `Web Mobile`
2. 勾选压缩纹理与资源压缩（按项目体量）
3. 关闭仅本地调试依赖项
4. 输出目录打包为 ZIP 上传

适配建议：

- UI 顶部避让使用 `wx.getMenuButtonBoundingClientRect()`
- 系统信息使用 `wx.getSystemInfoSync()`
- 更新提示接入 `wx.getUpdateManager()`

### 2) LayaAir

导出建议：

1. 发布平台选择 `Web`
2. 开启资源版本管理（文件哈希）
3. 输出后验证 `index.html` 与入口脚本路径
4. 打 ZIP 上传

适配建议：

- 避免运行时动态拼接跨域 URL
- 优先通过平台 API 请求业务后端

### 3) Egret

导出建议：

1. 构建 HTML5 包
2. 确认资源清单与脚本引用正确
3. 输出目录打 ZIP 上传

适配建议：

- 老项目迁移时优先检查资源路径大小写
- 若使用旧版第三方插件，先做一次浏览器回归

## 第二梯队引擎接入

### 4) Unity WebGL

导出建议：

1. Build Target 选择 `WebGL`
2. Compression Format 推荐 `Gzip` 或 `Brotli`
3. 勾选 Data Caching（按项目需要）
4. 导出后打 ZIP 上传

注意事项：

- 包体偏大，建议首包尽量控制
- 关注移动端内存占用和纹理尺寸
- 使用平台本地 HTTP 服务加载 Wasm，避免 file:// 方案

### 5) Godot Web Export

导出建议：

1. Export 选择 `Web`
2. 配置 Canvas/线程参数（按项目需求）
3. 导出目录打 ZIP 上传

注意事项：

- 优先测试中低端机型帧率
- 减少首屏阻塞资源

### 6) Unreal Engine（实验性）

说明：

- 可作为技术探索，不建议移动端商业项目首选
- 包体和性能压力较大，审核前需专项压测

## 第三梯队框架接入

### 7) Phaser 3 / PixiJS

推荐场景：

- 超休闲、强活动型、快速迭代项目

接入建议：

1. 使用 Vite/Webpack 打包到 `dist/`
2. 确保 `dist/index.html` 在 ZIP 根目录
3. 使用平台 `wx` 兼容 API 做登录、存储、网络

### 8) Three.js / Babylon.js

推荐场景：

- 轻量 3D 互动、跑酷、竞速

接入建议：

- 控制模型面数和贴图尺寸
- 减少运行时 shader 编译抖动

### 9) Construct 3 / Defold

推荐场景：

- 非重编码团队快速产出

接入建议：

- 导出后先在浏览器做资源完整性检查
- 再上传平台做移动端真机验证

## 平台 API 最小接入集

建议至少实现与验证以下接口调用：

1. `wx.getSystemInfoSync()`
2. `wx.getMenuButtonBoundingClientRect()`
3. `wx.request(...)`
4. `wx.setStorageSync / wx.getStorageSync`
5. `wx.getUpdateManager()`

## 提测清单（提交审核前）

1. 首屏可见且可交互
2. 页面无乱码（简体/繁体切换正常）
3. 横竖屏策略符合设计
4. 胶囊按钮区域无 UI 遮挡
5. 断网/弱网提示可用
6. 更新提示流程可触发
7. 下载失败有重试提示

## 常见问题

### Q1: 上传后提示 `index.html` 不存在

原因：ZIP 根目录层级错误。请将游戏文件夹“内容”打包，而不是把外层目录打包。

### Q2: Android 端出现文字乱码

原因：资源编码不统一。请将文本资源、JSON、HTML 统一为 UTF-8。

### Q3: Unity/Godot 游戏加载慢

建议：降低首包体积、拆分资源、延迟加载非首屏资源。

### Q4: 顶部按钮被平台胶囊挡住

建议：按 `wx.getMenuButtonBoundingClientRect()` 结果做布局避让，不要写死像素。
