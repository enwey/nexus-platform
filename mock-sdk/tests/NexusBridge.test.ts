import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { nexusBridge } from '../src/core/NexusBridge'

describe('NexusBridge', () => {
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
      }
    } as any
    
    global.window = mockWindow
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('detectPlatform', () => {
    it('should detect Android platform', () => {
      mockWindow.AndroidApp = {}
      mockWindow.webkit = undefined
      
      const bridge = new NexusBridge()
      expect(bridge.getPlatform()).toBe('android')
    })

    it('should detect iOS platform', () => {
      mockWindow.AndroidApp = undefined
      mockWindow.webkit = {
        messageHandlers: {
          NexusBridge: {}
        }
      }
      
      const bridge = new NexusBridge()
      expect(bridge.getPlatform()).toBe('ios')
    })

    it('should detect web platform', () => {
      mockWindow.AndroidApp = undefined
      mockWindow.webkit = undefined
      
      const bridge = new NexusBridge()
      expect(bridge.getPlatform()).toBe('web')
    })
  })

  describe('isNativeEnvironment', () => {
    it('should return true for Android', () => {
      mockWindow.AndroidApp = {}
      
      const bridge = new NexusBridge()
      expect(bridge.isNativeEnvironment()).toBe(true)
    })

    it('should return true for iOS', () => {
      mockWindow.AndroidApp = undefined
      mockWindow.webkit = {
        messageHandlers: {
          NexusBridge: {}
        }
      }
      
      const bridge = new NexusBridge()
      expect(bridge.isNativeEnvironment()).toBe(true)
    })

    it('should return false for web', () => {
      mockWindow.AndroidApp = undefined
      mockWindow.webkit = undefined
      
      const bridge = new NexusBridge()
      expect(bridge.isNativeEnvironment()).toBe(false)
    })
  })
})
