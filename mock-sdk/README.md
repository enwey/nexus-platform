# WxMockSDK

微信小游戏 Mock SDK，用于 Nexus 平台的原生容器中。

## 功能特性

- 完整的微信小游戏 API 模拟
- 自动检测运行环境（Android/iOS/Web）
- 支持原生桥接通信
- 优雅降级：未实现的 API 返回 null 并打印警告
- 支持 IIFE 格式，可直接在浏览器中使用

## 安装

```bash
npm install
npm run build
```

## 使用方法

### 在原生容器中使用

将构建产物 `dist/wx-mock-sdk.iife.js` 注入到 WebView 中：

```html
<script src="wx-mock-sdk.iife.js"></script>
```

### 在游戏中使用

SDK 会自动将 `wx` 对象挂载到全局，可以直接使用：

```javascript
wx.login({
  success: (res) => {
    console.log('登录成功', res)
  }
})

wx.getSystemInfo({
  success: (res) => {
    console.log('系统信息', res)
  }
})

wx.setStorage({
  key: 'test',
  data: { value: 'hello' },
  success: () => {
    console.log('存储成功')
  }
})
```

## 支持的 API

### 基础 API
- `wx.login` - 用户登录
- `wx.getSystemInfo` / `wx.getSystemInfoSync` - 获取系统信息
- `wx.request` - 网络请求
- `wx.setStorage` / `wx.setStorageSync` - 设置存储
- `wx.getStorage` / `wx.getStorageSync` - 获取存储
- `wx.removeStorage` / `wx.removeStorageSync` - 删除存储
- `wx.clearStorage` / `wx.clearStorageSync` - 清空存储

### 扩展 API
- `wx.getUserInfo` - 获取用户信息
- `wx.shareAppMessage` - 分享
- `wx.showToast` / `wx.hideToast` - 提示框
- `wx.showModal` - 模态框
- `wx.navigateTo` / `wx.navigateBack` - 页面导航
- `wx.downloadFile` - 下载文件
- `wx.uploadFile` - 上传文件
- `wx.getNetworkType` - 获取网络类型
- `wx.onMemoryWarning` - 内存警告监听
- `wx.getSetting` / `wx.openSetting` - 设置
- `wx.chooseImage` / `wx.previewImage` / `wx.getImageInfo` - 图片处理
- `wx.saveImageToPhotosAlbum` - 保存图片到相册
- `wx.setClipboardData` / `wx.getClipboardData` - 剪贴板
- `wx.vibrateShort` / `wx.vibrateLong` - 震动
- `wx.createAnimation` - 创建动画
- `wx.createCanvasContext` - 创建画布上下文
- `wx.createSelectorQuery` - 创建节点查询
- `wx.createIntersectionObserver` - 创建交叉观察器
- `wx.onAccelerometerChange` / `wx.startAccelerometer` / `wx.stopAccelerometer` - 加速度计
- `wx.onCompassChange` / `wx.startCompass` / `wx.stopCompass` - 罗盘

## 原生桥接协议

### JS 到 Native

SDK 通过以下方式与原生通信：

**Android:**
```javascript
window.AndroidApp.postMessage(JSON.stringify({
  api: 'wx.methodName',
  callbackId: 'cb_xxx',
  params: { ... }
}))
```

**iOS:**
```javascript
window.webkit.messageHandlers.NexusBridge.postMessage({
  api: 'wx.methodName',
  callbackId: 'cb_xxx',
  params: { ... }
})
```

### Native 到 JS

原生通过以下方式回调 JS：

```javascript
window.NexusBridgeCallback(JSON.stringify({
  callbackId: 'cb_xxx',
  data: { ... },
  error: { errMsg: '...', code: -1 }
}))
```

## 开发

```bash
npm run dev      # 启动开发服务器
npm run build    # 构建生产版本
npm run type-check # 类型检查
```

## 项目结构

```
src/
├── core/
│   └── NexusBridge.ts    # 核心桥接通信
├── api/
│   ├── base.ts           # 基础 API
│   └── extended.ts       # 扩展 API
├── types/
│   └── index.ts          # 类型定义
└── index.ts              # 主入口
```

## 注意事项

1. SDK 会自动检测运行环境，在 Web 环境下会使用 mock 数据
2. 未实现的 API 会返回 null 并打印警告，不会抛出异常
3. 所有 API 都遵循微信小游戏 API 规范
4. 建议在生产环境中使用压缩后的版本

## License

MIT
