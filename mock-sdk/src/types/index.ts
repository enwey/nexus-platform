export interface NativeMessage {
  api: string
  callbackId: string
  params: Record<string, any>
}

export interface NativeResponse {
  callbackId: string
  data?: any
  error?: {
    errMsg: string
    code?: number
  }
}

export type Platform = 'android' | 'ios' | 'web' | 'unknown'

export interface Callback {
  success?: (data: any) => void
  fail?: (error: any) => void
  complete?: () => void
}

export interface SyncNativeBridge {
  invokeSync(message: string): string
}

export interface SystemInfo {
  brand: string
  model: string
  pixelRatio: number
  screenWidth: number
  screenHeight: number
  windowWidth: number
  windowHeight: number
  language: string
  version: string
  system: string
  platform: string
  fontSizeSetting: number
  SDKVersion: string
  benchmarkLevel: number
  albumAuthorized: boolean
  cameraAuthorized: boolean
  locationAuthorized: boolean
  microphoneAuthorized: boolean
  notificationAuthorized: boolean
  bluetoothAuthorized: boolean
}

export interface LoginResult {
  code: string
  errMsg: string
}

export interface RequestOption {
  url: string
  data?: any
  header?: Record<string, string>
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'HEAD' | 'OPTIONS'
  dataType?: 'json' | 'string'
  responseType?: 'text' | 'arraybuffer'
  success?: (res: any) => void
  fail?: (err: any) => void
  complete?: () => void
}

export interface StorageOption {
  key: string
  data?: any
  success?: (res?: any) => void
  fail?: (err: any) => void
  complete?: () => void
}
