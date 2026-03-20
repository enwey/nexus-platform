import * as baseAPI from './api/base'
import * as extendedAPI from './api/extended'
import { nexusBridge } from './core/NexusBridge'

declare global {
  interface Window {
    wx: any
  }
}

const wx = {
  login: baseAPI.login,
  getSystemInfo: baseAPI.getSystemInfo,
  getSystemInfoSync: baseAPI.getSystemInfoSync,
  request: baseAPI.request,
  setStorage: baseAPI.setStorage,
  setStorageSync: baseAPI.setStorageSync,
  getStorage: baseAPI.getStorage,
  getStorageSync: baseAPI.getStorageSync,
  removeStorage: baseAPI.removeStorage,
  removeStorageSync: baseAPI.removeStorageSync,
  clearStorage: baseAPI.clearStorage,
  clearStorageSync: baseAPI.clearStorageSync,
  getUserInfo: extendedAPI.getUserInfo,
  shareAppMessage: extendedAPI.shareAppMessage,
  showToast: extendedAPI.showToast,
  hideToast: extendedAPI.hideToast,
  showModal: extendedAPI.showModal,
  navigateTo: extendedAPI.navigateTo,
  navigateBack: extendedAPI.navigateBack,
  downloadFile: extendedAPI.downloadFile,
  uploadFile: extendedAPI.uploadFile,
  getNetworkType: extendedAPI.getNetworkType,
  onMemoryWarning: extendedAPI.onMemoryWarning,
  getSetting: extendedAPI.getSetting,
  openSetting: extendedAPI.openSetting,
  chooseImage: extendedAPI.chooseImage,
  previewImage: extendedAPI.previewImage,
  getImageInfo: extendedAPI.getImageInfo,
  saveImageToPhotosAlbum: extendedAPI.saveImageToPhotosAlbum,
  setClipboardData: extendedAPI.setClipboardData,
  getClipboardData: extendedAPI.getClipboardData,
  vibrateShort: extendedAPI.vibrateShort,
  vibrateLong: extendedAPI.vibrateLong,
  createAnimation: extendedAPI.createAnimation,
  createCanvasContext: extendedAPI.createCanvasContext,
  createSelectorQuery: extendedAPI.createSelectorQuery,
  createIntersectionObserver: extendedAPI.createIntersectionObserver,
  onAccelerometerChange: extendedAPI.onAccelerometerChange,
  startAccelerometer: extendedAPI.startAccelerometer,
  stopAccelerometer: extendedAPI.stopAccelerometer,
  onCompassChange: extendedAPI.onCompassChange,
  startCompass: extendedAPI.startCompass,
  stopCompass: extendedAPI.stopCompass
}

if (typeof window !== 'undefined') {
  window.wx = wx
}

console.log('[WxMockSDK] SDK initialized successfully')
console.log('[WxMockSDK] Platform:', nexusBridge.getPlatform())
console.log('[WxMockSDK] Native Environment:', nexusBridge.isNativeEnvironment())

export default wx
export { nexusBridge }
