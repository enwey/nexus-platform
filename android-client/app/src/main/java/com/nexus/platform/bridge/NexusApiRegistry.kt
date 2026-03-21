package com.nexus.platform.bridge

import android.content.Context
import com.nexus.platform.api.*

internal fun createApiHandlers(context: Context): Map<String, ApiHandler> {
    return mapOf(
        "wx.login" to LoginApi(context),
        "wx.request" to RequestApi(context),
        "wx.getSystemInfoSync" to SystemInfoApi(context),
        "wx.setStorageSync" to StorageApi(context),
        "wx.setStorage" to StorageApi(context),
        "wx.getStorageSync" to StorageApi(context),
        "wx.getStorage" to StorageApi(context),
        "wx.removeStorageSync" to StorageApi(context),
        "wx.removeStorage" to StorageApi(context),
        "wx.clearStorageSync" to StorageApi(context),
        "wx.clearStorage" to StorageApi(context),
        "wx.getUserInfo" to UserInfoApi(context),
        "wx.shareAppMessage" to ShareApi(context),
        "wx.showToast" to ToastApi(context),
        "wx.hideToast" to UnsupportedApi(),
        "wx.showModal" to ModalApi(context),
        "wx.navigateTo" to UnsupportedApi(),
        "wx.navigateBack" to UnsupportedApi(),
        "wx.downloadFile" to FileApi(context),
        "wx.uploadFile" to FileApi(context),
        "wx.getNetworkType" to NetworkApi(context),
        "wx.onMemoryWarning" to UnsupportedApi(),
        "wx.getSetting" to UnsupportedApi(),
        "wx.openSetting" to UnsupportedApi(),
        "wx.chooseImage" to ImageApi(context),
        "wx.previewImage" to ImageApi(context),
        "wx.getImageInfo" to ImageApi(context),
        "wx.saveImageToPhotosAlbum" to ImageApi(context),
        "wx.setClipboardData" to ClipboardApi(context),
        "wx.getClipboardData" to ClipboardApi(context),
        "wx.vibrateShort" to VibrateApi(context),
        "wx.vibrateLong" to VibrateApi(context),
        "wx.createAnimation" to UnsupportedApi(),
        "wx.createCanvasContext" to UnsupportedApi(),
        "wx.createSelectorQuery" to UnsupportedApi(),
        "wx.createIntersectionObserver" to UnsupportedApi(),
        "wx.onAccelerometerChange" to UnsupportedApi(),
        "wx.startAccelerometer" to UnsupportedApi(),
        "wx.stopAccelerometer" to UnsupportedApi(),
        "wx.onCompassChange" to UnsupportedApi(),
        "wx.startCompass" to UnsupportedApi(),
        "wx.stopCompass" to UnsupportedApi()
    )
}
