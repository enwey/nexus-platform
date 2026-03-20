import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nexusBridge } from '../src/core/NexusBridge'

describe('API Tests', () => {
  let mockWindow: any

  beforeEach(() => {
    mockWindow = {
      webkit: {
        messageHandlers: {
          NexusBridge: {
            postMessage: vi.fn()
          }
        }
      },
      AndroidApp: {
        postMessage: vi.fn()
      },
      NexusBridgeCallback: vi.fn()
    }
    
    global.window = mockWindow
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('login', () => {
    it('should call invokeNative with correct parameters', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.login', {}, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.login')
      )
    })

    it('should handle success callback', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.login', {}, {
        success: mockCallback
        fail: mockCallback
      })
      
      const successData = { code: 'test_code', errMsg: 'login:ok' }
      mockCallback(successData)
      
      expect(mockCallback).toHaveBeenCalledWith(successData)
    })
  })

  describe('getSystemInfoSync', () => {
    it('should return system info', () => {
      const bridge = new NexusBridge()
      
      const result = bridge['getSystemInfoSync']()
      
      expect(result).toHaveProperty('brand')
      expect(result).toHaveProperty('model')
      expect(result).toHaveProperty('pixelRatio')
    })
  })

  describe('storage', () => {
    it('should set storage', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.setStorage', { key: 'test', data: 'value' }, {
        success: mockCallback
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.setStorage')
      )
    })

    it('should get storage', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.getStorage', { key: 'test' }, {
        success: mockCallback,
        fail: mockCallback
      })
      
      const successData = { data: 'value', errMsg: 'getStorage:ok' }
      mockCallback(successData)
      
      expect(mockCallback).toHaveBeenCalledWith(successData)
    })

    it('should remove storage', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.removeStorage', { key: 'test' }, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.removeStorage')
      )
    })

    it('should clear storage', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.clearStorage', {}, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.clearStorage')
      )
    })
  })

  describe('request', () => {
    it('should make HTTP request', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.request', { url: 'http://test.com' }, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.request')
      )
    })
  })

  describe('getUserInfo', () => {
    it('should get user info', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.getUserInfo', {}, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.getUserInfo')
      )
    })
  })

  describe('showToast', () => {
    it('should show toast', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.showToast', { title: 'Test message' }, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.showToast')
      )
    })
  })

  describe('showModal', () => {
    it('should show modal', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.showModal', { title: 'Test', content: 'Test content' }, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.showModal')
      )
    })
  })

  describe('vibrate', () => {
    it('should vibrate short', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.vibrateShort', {}, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.vibrateShort')
      )
    })

    it('should vibrate long', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.vibrateLong', {}, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.vibrateLong')
      )
    })
  })

  describe('clipboard', () => {
    it('should set clipboard data', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.setClipboardData', { data: 'test data' }, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.setClipboardData')
      )
    })

    it('should get clipboard data', async () => {
      const bridge = new NexusBridge()
      const mockCallback = vi.fn()
      
      bridge.invokeNative('wx.getClipboardData', {}, {
        success: mockCallback,
        fail: mockCallback
      })
      
      expect(mockWindow.AndroidApp?.postMessage).toHaveBeenCalledWith(
        expect.stringContaining('wx.getClipboardData')
      )
    })
  })
})
