import Foundation
import UIKit
import AudioToolbox

class VibrateApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        switch api {
        case "wx.vibrateShort":
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            return ["errMsg": "vibrateShort:ok"]
        case "wx.vibrateLong":
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            try await Task.sleep(nanoseconds: 400_000_000)
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            return ["errMsg": "vibrateLong:ok"]
        default:
            throw ApiError.notImplemented
        }
    }
}

class ClipboardApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        switch api {
        case "wx.setClipboardData":
            guard let data = params["data"] as? String else {
                throw ApiError.invalidParameter
            }
            UIPasteboard.general.string = data
            return ["errMsg": "setClipboardData:ok"]
        case "wx.getClipboardData":
            let data = UIPasteboard.general.string ?? ""
            return [
                "data": data,
                "errMsg": "getClipboardData:ok"
            ]
        default:
            throw ApiError.notImplemented
        }
    }
}

class UserInfoApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        return [
            "userInfo": [
                "nickName": "测试用户",
                "avatarUrl": "",
                "gender": 0,
                "language": "zh_CN",
                "city": "",
                "province": "",
                "country": "中国"
            ],
            "errMsg": "getUserInfo:ok"
        ]
    }
}

class ShareApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        return ["errMsg": "shareAppMessage:ok"]
    }
}

class ImageApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        switch api {
        case "wx.chooseImage":
            return [
                "tempFilePaths": [String](),
                "errMsg": "chooseImage:ok"
            ]
        case "wx.previewImage":
            return ["errMsg": "previewImage:ok"]
        case "wx.getImageInfo":
            return [
                "width": 100,
                "height": 100,
                "path": "",
                "orientation": 0,
                "type": "jpg",
                "errMsg": "getImageInfo:ok"
            ]
        case "wx.saveImageToPhotosAlbum":
            return ["errMsg": "saveImageToPhotosAlbum:ok"]
        default:
            throw ApiError.notImplemented
        }
    }
}

class FileApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        switch api {
        case "wx.downloadFile":
            return [
                "tempFilePath": "/path/to/temp/file",
                "statusCode": 200,
                "errMsg": "downloadFile:ok"
            ]
        case "wx.uploadFile":
            return [
                "data": "",
                "statusCode": 200,
                "errMsg": "uploadFile:ok"
            ]
        default:
            throw ApiError.notImplemented
        }
    }
}
