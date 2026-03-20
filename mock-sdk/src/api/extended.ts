import { nexusBridge } from '../core/NexusBridge'

export const getUserInfo = (options?: {
  withCredentials?: boolean
  lang?: string
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.getUserInfo', {
    withCredentials: options?.withCredentials || false,
    lang: options?.lang || 'en'
  }, {
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

export const shareAppMessage = (options: {
  title?: string
  path?: string
  imageUrl?: string
}): void => {
  nexusBridge.invokeNative('wx.shareAppMessage', options)
}

export const showToast = (options: {
  title: string
  icon?: 'success' | 'loading' | 'none'
  duration?: number
  mask?: boolean
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.showToast', {
    title: options.title,
    icon: options.icon || 'none',
    duration: options.duration || 1500,
    mask: options.mask || false
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
}

export const hideToast = (): void => {
  nexusBridge.invokeNative('wx.hideToast', {})
}

export const showModal = (options: {
  title: string
  content: string
  showCancel?: boolean
  cancelText?: string
  cancelColor?: string
  confirmText?: string
  confirmColor?: string
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.showModal', {
    title: options.title,
    content: options.content,
    showCancel: options.showCancel !== false,
    cancelText: options.cancelText || '取消',
    cancelColor: options.cancelColor || '#000000',
    confirmText: options.confirmText || '确定',
    confirmColor: options.confirmColor || '#576B95'
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

export const navigateTo = (options: {
  url: string
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.navigateTo', {
    url: options.url
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
}

export const navigateBack = (options?: {
  delta?: number
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.navigateBack', {
    delta: options?.delta || 1
  }, {
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
}

export const downloadFile = (options: {
  url: string
  header?: Record<string, string>
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.downloadFile', {
    url: options.url,
    header: options.header || {}
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

export const uploadFile = (options: {
  url: string
  filePath: string
  name: string
  header?: Record<string, string>
  formData?: Record<string, string>
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.uploadFile', {
    url: options.url,
    filePath: options.filePath,
    name: options.name,
    header: options.header || {},
    formData: options.formData || {}
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

export const getNetworkType = (options?: {
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.getNetworkType', {}, {
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

export const onMemoryWarning = (callback: (res: any) => void): void => {
  nexusBridge.invokeNative('wx.onMemoryWarning', {})
  const win = window as any
  win._memoryWarningCallback = callback
}

export const getSetting = (options?: {
  withSubscriptions?: boolean
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.getSetting', {
    withSubscriptions: options?.withSubscriptions || false
  }, {
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

export const openSetting = (options?: {
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.openSetting', {}, {
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

export const chooseImage = (options: {
  count?: number
  sizeType?: Array<'original' | 'compressed'>
  sourceType?: Array<'album' | 'camera'>
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.chooseImage', {
    count: options.count || 9,
    sizeType: options.sizeType || ['original', 'compressed'],
    sourceType: options.sourceType || ['album', 'camera']
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

export const previewImage = (options: {
  current: string | number
  urls: string[]
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.previewImage', {
    current: options.current,
    urls: options.urls
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
}

export const getImageInfo = (options: {
  src: string
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.getImageInfo', {
    src: options.src
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

export const saveImageToPhotosAlbum = (options: {
  filePath: string
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.saveImageToPhotosAlbum', {
    filePath: options.filePath
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
}

export const setClipboardData = (options: {
  data: string
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.setClipboardData', {
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
}

export const getClipboardData = (options?: {
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.getClipboardData', {}, {
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

export const vibrateShort = (options?: {
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.vibrateShort', {}, {
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
}

export const vibrateLong = (options?: {
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.vibrateLong', {}, {
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
}

export const createAnimation = (options?: {
  duration?: number
  timingFunction?: string
  delay?: number
  transformOrigin?: string
}): any => {
  return nexusBridge.invokeNative('wx.createAnimation', options || {})
}

export const createCanvasContext = (canvasId: string, component?: any): any => {
  return nexusBridge.invokeNative('wx.createCanvasContext', { canvasId, component })
}

export const createSelectorQuery = (): any => {
  return nexusBridge.invokeNative('wx.createSelectorQuery', {})
}

export const createIntersectionObserver = (component?: any, options?: {
  thresholds?: number[]
  observeAll?: boolean
}): any => {
  return nexusBridge.invokeNative('wx.createIntersectionObserver', { component, options })
}

export const onAccelerometerChange = (callback: (res: any) => void): void => {
  nexusBridge.invokeNative('wx.onAccelerometerChange', {})
  const win = window as any
  win._accelerometerCallback = callback
}

export const startAccelerometer = (options?: {
  interval?: number
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.startAccelerometer', {
    interval: options?.interval || 'normal'
  }, {
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
}

export const stopAccelerometer = (options?: {
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.stopAccelerometer', {}, {
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
}

export const onCompassChange = (callback: (res: any) => void): void => {
  nexusBridge.invokeNative('wx.onCompassChange', {})
  const win = window as any
  win._compassCallback = callback
}

export const startCompass = (options?: {
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.startCompass', {}, {
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
}

export const stopCompass = (options?: {
  success?: () => void
  fail?: (err: any) => void
  complete?: () => void
}): void => {
  nexusBridge.invokeNative('wx.stopCompass', {}, {
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
}
