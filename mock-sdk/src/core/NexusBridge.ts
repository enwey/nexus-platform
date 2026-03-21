import type { NativeMessage, NativeResponse, Platform, Callback } from '../types'

/**
 * NexusBridge类，用于JavaScript与原生代码之间的通信
 */
class NexusBridge {
  private callbacks: Map<string, Callback> = new Map()
  private platform: Platform = 'unknown'
  private isNative: boolean = false

  constructor() {
    this.detectPlatform()
    this.setupMessageListener()
  }

  /**
   * 检测运行平台
   */
  private detectPlatform(): void {
    if (typeof window === 'undefined') {
      this.platform = 'unknown'
      return
    }

    const win = window as any

    if (win.AndroidApp) {
      this.platform = 'android'
      this.isNative = true
    } else if (win.webkit && win.webkit.messageHandlers && win.webkit.messageHandlers.NexusBridge) {
      this.platform = 'ios'
      this.isNative = true
    } else {
      this.platform = 'web'
      this.isNative = false
    }

    console.log(`[NexusBridge] Platform detected: ${this.platform}, isNative: ${this.isNative}`)
  }

  /**
   * 设置消息监听器
   */
  private setupMessageListener(): void {
    if (typeof window === 'undefined') return

    const win = window as any

    if (this.platform === 'android') {
      win.NexusBridgeCallback = (response: string) => {
        this.handleNativeResponse(JSON.parse(response))
      }
    } else if (this.platform === 'ios') {
      win.NexusBridgeCallback = (response: string) => {
        this.handleNativeResponse(JSON.parse(response))
      }
    }
  }

  /**
   * 处理原生响应
   * @param response 原生响应对象
   */
  private handleNativeResponse(response: NativeResponse): void {
    const callback = this.callbacks.get(response.callbackId)
    if (!callback) {
      console.warn(`[NexusBridge] No callback found for ${response.callbackId}`)
      return
    }

    if (response.error) {
      callback.fail?.(response.error)
    } else {
      callback.success?.(response.data)
    }

    callback.complete?.()
    this.callbacks.delete(response.callbackId)
  }

  /**
   * 生成回调ID
   * @return 回调ID
   */
  private generateCallbackId(): string {
    return `cb_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  /**
   * 调用原生API
   * @param api API名称
   * @param params 请求参数
   * @param callback 回调函数
   */
  public invokeNative(api: string, params: Record<string, any> = {}, callback?: Callback): void {
    const callbackId = this.generateCallbackId()

    if (callback) {
      this.callbacks.set(callbackId, callback)
    }

    const message: NativeMessage = {
      api,
      callbackId,
      params
    }

    if (!this.isNative) {
      console.warn(`[NexusBridge] Native environment not detected, API call mocked: ${api}`)
      this.mockResponse(callbackId, api, params)
      return
    }

    const win = window as any

    if (this.platform === 'android') {
      try {
        win.AndroidApp.postMessage(JSON.stringify(message))
      } catch (error) {
        console.error('[NexusBridge] Android invoke error:', error)
        this.handleNativeResponse({
          callbackId,
          error: {
            errMsg: `Android invoke failed: ${error}`,
            code: -1
          }
        })
      }
    } else if (this.platform === 'ios') {
      try {
        win.webkit.messageHandlers.NexusBridge.postMessage(message)
      } catch (error) {
        console.error('[NexusBridge] iOS invoke error:', error)
        this.handleNativeResponse({
          callbackId,
          error: {
            errMsg: `iOS invoke failed: ${error}`,
            code: -1
          }
        })
      }
    }
  }

  /**
   * 模拟响应（用于Web环境）
   * @param callbackId 回调ID
   * @param api API名称
   * @param params 请求参数
   */
  private mockResponse(callbackId: string, api: string, params: any): void {
    setTimeout(() => {
      const response: NativeResponse = {
        callbackId,
        data: this.getMockData(api, params)
      }
      this.handleNativeResponse(response)
    }, 100)
  }

  /**
   * 获取模拟数据
   * @param api API名称
   * @param params 请求参数
   * @return 模拟数据
   */
  private getMockData(api: string, params: any): any {
    switch (api) {
      case 'wx.login':
        return { code: 'mock_code_123', errMsg: 'login:ok' }
      case 'wx.getSystemInfoSync':
        return this.getMockSystemInfo()
      case 'wx.getStorage':
        return { data: localStorage.getItem(params.key) || '', errMsg: 'getStorage:ok' }
      case 'wx.getNetworkType':
        return { networkType: 'wifi', errMsg: 'getNetworkType:ok' }
      default:
        return { errMsg: `${api}:ok` }
    }
  }

  /**
   * 获取模拟系统信息
   * @return 系统信息对象
   */
  private getMockSystemInfo(): any {
    return {
      brand: 'mock',
      model: 'mock_device',
      pixelRatio: window.devicePixelRatio || 1,
      screenWidth: window.screen.width,
      screenHeight: window.screen.height,
      windowWidth: window.innerWidth,
      windowHeight: window.innerHeight,
      language: navigator.language,
      version: '1.0.0',
      system: 'Mock System',
      platform: this.platform,
      fontSizeSetting: 16,
      SDKVersion: '1.0.0',
      benchmarkLevel: 1,
      albumAuthorized: true,
      cameraAuthorized: true,
      locationAuthorized: true,
      microphoneAuthorized: true,
      notificationAuthorized: true,
      bluetoothAuthorized: true
    }
  }

  /**
   * 获取当前平台
   * @return 平台类型
   */
  public getPlatform(): Platform {
    return this.platform
  }

  /**
   * 是否为原生环境
   * @return 是否为原生环境
   */
  public isNativeEnvironment(): boolean {
    return this.isNative
  }
}

/**
 * 导出NexusBridge单例
 */
export const nexusBridge = new NexusBridge()
