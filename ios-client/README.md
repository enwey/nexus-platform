# Nexus Platform iOS Client

Nexus 平台的 iOS 原生客户端，用于运行微信小游戏。

## 功能特性

- 完整的微信小游戏 API 支持
- WKURLSchemeHandler 实现本地文件加载
- NexusBridge 桥接通信
- 游戏 ZIP 下载与解压
- MD5 校验
- SwiftUI 现代化界面
- 增量更新支持

## 项目结构

```
Sources/NexusPlatform/
├── App.swift                    # 应用入口
├── Views/
│   ├── GameListView.swift        # 游戏列表界面
│   └── GameView.swift          # 游戏运行界面
├── Bridge/
│   └── NexusBridge.swift       # JS 与 Native 桥接
├── API/
│   ├── ApiHandler.swift         # API 处理器协议
│   ├── SystemApis.swift        # 系统相关 API
│   ├── StorageApi.swift        # 存储相关 API
│   ├── NetworkApis.swift       # 网络相关 API
│   └── OtherApis.swift        # 其他 API
└── Utils/
    └── GameManager.swift        # 游戏管理
```

## 构建要求

- Xcode 15.0 或更高版本
- iOS 14.0 或更高版本
- Swift 5.9 或更高版本
- Swift Package Manager

## 依赖库

- Alamofire - 网络请求
- SwiftyJSON - JSON 处理

## 构建步骤

1. 克隆项目
```bash
git clone <repository-url>
cd nexus-platform/ios-client
```

2. 打开项目
```bash
# 使用 Xcode 打开 Package.swift
open Package.swift
```

3. 构建项目
```bash
# 在 Xcode 中选择目标设备并点击 Build
# 或使用命令行
swift build
```

4. 运行应用
```bash
# 在 Xcode 中点击 Run
# 或使用命令行
swift run
```

## 配置

### SDK 注入

将 Mock SDK 构建产物 `wx-mock-sdk.js` 放到应用的 Documents 目录下。

### 游戏服务器

修改 `GameManager.swift` 中的游戏下载地址。

## 使用说明

### 启动游戏

```swift
let game = Game(
    id: "game1",
    name: "示例游戏",
    description: "这是一个示例游戏",
    iconUrl: "https://example.com/icon.png",
    downloadUrl: "https://example.com/game1.zip",
    version: "1.0.0",
    md5: "abc123..."
)

GameManager.shared.loadGame(game, in: webView) { success in
    print("游戏加载\(success ? "成功" : "失败")")
}
```

### JS 调用 Native

游戏中的 JS 代码可以通过 `window.webkit.messageHandlers.NexusBridge.postMessage()` 调用原生方法：

```javascript
window.webkit.messageHandlers.NexusBridge.postMessage({
  api: 'wx.login',
  callbackId: 'cb_123',
  params: {}
})
```

### Native 回调 JS

原生通过 `window.NexusBridgeCallback()` 回调 JS：

```javascript
window.NexusBridgeCallback({
  callbackId: 'cb_123',
  data: { code: 'xxx', errMsg: 'login:ok' },
  error: null
})
```

## 支持的 API

### 系统相关
- `wx.login` - 用户登录
- `wx.getSystemInfoSync` - 获取系统信息

### 存储相关
- `wx.setStorage` / `wx.setStorageSync` - 设置存储
- `wx.getStorage` / `wx.getStorageSync` - 获取存储
- `wx.removeStorage` / `wx.removeStorageSync` - 删除存储
- `wx.clearStorage` / `wx.clearStorageSync` - 清空存储

### 网络相关
- `wx.request` - 网络请求
- `wx.getNetworkType` - 获取网络类型

### UI 相关
- `wx.showToast` - 显示提示框
- `wx.showModal` - 显示模态框

### 其他
- `wx.getUserInfo` - 获取用户信息
- `wx.shareAppMessage` - 分享
- `wx.chooseImage` - 选择图片
- `wx.setClipboardData` / `wx.getClipboardData` - 剪贴板
- `wx.vibrateShort` / `wx.vibrateLong` - 震动
- `wx.downloadFile` - 下载文件
- `wx.uploadFile` - 上传文件

## 技术架构

### WKWebView 配置

- 启用 JavaScript
- 启用内联媒体播放
- 禁用滚动反弹
- 禁用滚动
- 全屏模式

### 本地文件加载

使用 `WKURLSchemeHandler` 实现本地文件加载：

```swift
let schemeHandler = GameSchemeHandler(game: game)
config.setURLSchemeHandler(schemeHandler, forURLScheme: "nexus")
```

游戏通过 `nexus://game/` 协议访问本地文件。

### 通信机制

JS -> Native: `window.webkit.messageHandlers.NexusBridge.postMessage()`
Native -> JS: `window.NexusBridgeCallback()`

## 安全性

- WKURLSchemeHandler 限制访问
- ZIP 包 MD5 校验
- HTTPS 通信
- 文件沙盒隔离

## 性能优化

- 异步任务处理
- 资源预加载
- 缓存机制
- 增量更新

## 故障排查

### 游戏无法加载

1. 检查 ZIP 包是否正确解压
2. 检查 index.html 是否存在
3. 检查 WKURLSchemeHandler 配置
4. 查看控制台日志

### API 调用失败

1. 检查 API 是否已实现
2. 检查参数是否正确
3. 查看控制台日志
4. 检查权限是否授予

## License

MIT
