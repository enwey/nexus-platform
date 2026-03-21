import { nexusBridge } from '../core/NexusBridge'
import type { LoginResult, RequestOption, StorageOption, SystemInfo } from '../types'

/**
 * 用户登录
 * @param options 登录选项
 */
export const login = (options?: {
  timeout?: number
  success?: (res: LoginResult) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.login', {}, {
    success: (data) => {
      options?.success?.(data)
    },
    fail: (error) => {
      options?.fail?.(error)
    },
    complete: () => {
      options?.complete?.()
    }
  })
}

/**
 * 同步获取系统信息
 * @return 系统信息
 */
export const getSystemInfoSync = (): SystemInfo => {
  if (nexusBridge.isNativeEnvironment()) {
    const result = nexusBridge.invokeNative('wx.getSystemInfoSync', {}) as any
    return result
  }
  return nexusBridge['getMockSystemInfo']()
}

/**
 * 异步获取系统信息
 * @param options 选项
 */
export const getSystemInfo = (options?: {
  success?: (res: SystemInfo) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  const systemInfo = getSystemInfoSync()
  options?.success?.(systemInfo)
  options?.complete?.()
}

/**
 * 发起网络请求
 * @param options 请求选项
 */
export const request = (options: RequestOption): void => {
  nexusBridge.invokeNative('wx.request', {
    url: options.url,
    data: options.data,
    header: options.header,
    method: options.method || 'GET',
    dataType: options.dataType || 'json',
    responseType: options.responseType || 'text'
  }, {
    success: (data) => {
      options.success?.(data)
    },
    fail: (error) => {
      options.fail?.(error)
    },
    complete: () => {
      options.complete?.()
    }
  })
}

/**
 * 异步设置本地存储
 * @param options 存储选项
 */
export const setStorage = (options: StorageOption): void => {
  if (nexusBridge.isNativeEnvironment()) {
    nexusBridge.invokeNative('wx.setStorage', {
      key: options.key,
      data: options.data
    }, {
      success: () => {
        options.success?.()
      },
      fail: (error) => {
        options.fail?.(error)
      },
      complete: () => {
        options.complete?.()
      }
    })
  } else {
    try {
      localStorage.setItem(options.key, JSON.stringify(options.data))
      options.success?.()
      options.complete?.()
    } catch (error) {
      options.fail?.({ errMsg: 'setStorage:fail', error })
      options.complete?.()
    }
  }
}

/**
 * 同步设置本地存储
 * @param key 键名
 * @param data 数据
 */
export const setStorageSync = (key: string, data: any): void => {
  if (nexusBridge.isNativeEnvironment()) {
    nexusBridge.invokeNative('wx.setStorageSync', { key, data })
  } else {
    localStorage.setItem(key, JSON.stringify(data))
  }
}

/**
 * 异步获取本地存储
 * @param options 存储选项
 */
export const getStorage = (options: StorageOption): void => {
  if (nexusBridge.isNativeEnvironment()) {
    nexusBridge.invokeNative('wx.getStorage', {
      key: options.key
    }, {
      success: (data) => {
        options.success?.(data)
      },
      fail: (error) => {
        options.fail?.(error)
      },
      complete: () => {
        options.complete?.()
      }
    })
  } else {
    try {
      const data = JSON.parse(localStorage.getItem(options.key) || 'null')
      options.success?.({ data, errMsg: 'getStorage:ok' })
      options.complete?.()
    } catch (error) {
      options.fail?.({ errMsg: 'getStorage:fail', error })
      options.complete?.()
    }
  }
}

/**
 * 同步获取本地存储
 * @param key 键名
 * @return 数据
 */
export const getStorageSync = (key: string): any => {
  if (nexusBridge.isNativeEnvironment()) {
    const result = nexusBridge.invokeNative('wx.getStorageSync', { key }) as any
    return result.data
  }
  try {
    return JSON.parse(localStorage.getItem(key) || 'null')
  } catch {
    return null
  }
}

/**
 * 异步删除本地存储
 * @param options 存储选项
 */
export const removeStorage = (options: StorageOption): void => {
  if (nexusBridge.isNativeEnvironment()) {
    nexusBridge.invokeNative('wx.removeStorage', {
      key: options.key
    }, {
      success: () => {
        options.success?.()
      },
      fail: (error) => {
        options.fail?.(error)
      },
      complete: () => {
        options.complete?.()
      }
    })
  } else {
    try {
      localStorage.removeItem(options.key)
      options.success?.()
      options.complete?.()
    } catch (error) {
      options.fail?.({ errMsg: 'removeStorage:fail', error })
      options.complete?.()
    }
  }
}

/**
 * 同步删除本地存储
 * @param key 键名
 */
export const removeStorageSync = (key: string): void => {
  if (nexusBridge.isNativeEnvironment()) {
    nexusBridge.invokeNative('wx.removeStorageSync', { key })
  } else {
    localStorage.removeItem(key)
  }
}

/**
 * 异步清空本地存储
 * @param options 选项
 */
export const clearStorage = (options?: {
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  if (nexusBridge.isNativeEnvironment()) {
    nexusBridge.invokeNative('wx.clearStorage', {}, {
      success: () => {
        options?.success?.()
      },
      fail: (error) => {
        options?.fail?.(error)
      },
      complete: () => {
        options?.complete?.()
      }
    })
  } else {
    try {
      localStorage.clear()
      options?.success?.()
      options?.complete?.()
    } catch (error) {
      options?.fail?.({ errMsg: 'clearStorage:fail', error })
      options?.complete?.()
    }
  }
}

/**
 * 同步清空本地存储
 */
export const clearStorageSync = (): void => {
  if (nexusBridge.isNativeEnvironment()) {
    nexusBridge.invokeNative('wx.clearStorageSync', {})
  } else {
    localStorage.clear()
  }
}
