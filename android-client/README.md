# Nexus Platform Android Client

Nexus 平台的 Android 原生客户端，用于运行微信小游戏。

## 功能特性

- 完整的微信小游戏 API 支持
- WebViewAssetLoader 实现本地文件加载
- NexusBridge 桥接通信
- 游戏 ZIP 下载与解压
- MD5 校验
- 增量更新支持

## 项目结构

```
app/src/main/java/com/nexus/platform/
├── MainActivity.kt              # 主界面
├── GameActivity.kt             # 游戏运行界面
├── bridge/
│   └── NexusBridge.kt         # JS 与 Native 桥接
├── api/
│   ├── ApiHandler.kt           # API 处理器接口
│   ├── SystemApis.kt          # 系统相关 API
│   ├── StorageApi.kt          # 存储相关 API
│   ├── NetworkApis.kt         # 网络相关 API
│   └── OtherApis.kt           # 其他 API
└── utils/
    ├── GameManager.kt          # 游戏管理
    └── ZipUtils.kt            # ZIP 工具
```

## 构建要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

## 构建步骤

1. 克隆项目
```bash
git clone <repository-url>
cd nexus-platform/android-client
```

2. 打开项目
```bash
# 使用 Android Studio 打开项目
# 或使用命令行构建
./gradlew assembleDebug
```

3. 运行应用
```bash
./gradlew installDebug
```

## 配置

### SDK 注入

将 Mock SDK 构建产物 `wx-mock-sdk.iife.js` 放到 `app/src/main/assets/` 目录下。

### 游戏服务器

修改 `GameManager.kt` 中的游戏下载地址。

## 使用说明

### 启动游戏

```kotlin
val game = Game(
    id = "game1",
    name = "示例游戏",
    description = "这是一个示例游戏",
    iconUrl = "https://example.com/icon.png",
    downloadUrl = "https://example.com/game1.zip",
    version = "1.0.0",
    md5 = "abc123..."
)

GameActivity.start(context, game)
```

### JS 调用 Native

游戏中的 JS 代码可以通过 `window.AndroidApp.postMessage()` 调用原生方法：

```javascript
window.AndroidApp.postMessage(JSON.stringify({
  api: 'wx.login',
  callbackId: 'cb_123',
  params: {}
}))
```

### Native 回调 JS

原生通过 `window.NexusBridgeCallback()` 回调 JS：

```javascript
window.NexusBridgeCallback(JSON.stringify({
  callbackId: 'cb_123',
  data: { code: 'xxx', errMsg: 'login:ok' },
  error: null
}))
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

### WebView 配置

- 启用 JavaScript
- 启用 DOM Storage
- 启用硬件加速
- 禁用文件访问
- 禁用内容访问

### 本地文件加载

使用 `WebViewAssetLoader` 实现本地文件加载：

```kotlin
val assetLoader = WebViewAssetLoader.Builder()
    .addPathHandler("/assets/", InternalStoragePathHandler(context, unzipDir))
    .build()
```

### 通信机制

JS -> Native: `window.AndroidApp.postMessage()`
Native -> JS: `window.NexusBridgeCallback()`

## 安全性

- WebView 白名单限制
- ZIP 包 MD5 校验
- HTTPS 通信
- 文件沙盒隔离

## 性能优化

- 硬件加速渲染
- 资源预加载
- 缓存机制
- 增量更新

## 故障排查

### 游戏无法加载

1. 检查 ZIP 包是否正确解压
2. 检查 index.html 是否存在
3. 检查 WebViewAssetLoader 配置
4. 查看日志输出

### API 调用失败

1. 检查 API 是否已实现
2. 检查参数是否正确
3. 查看日志输出
4. 检查权限是否授予

## License

MIT
