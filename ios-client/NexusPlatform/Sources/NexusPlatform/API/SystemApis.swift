import Foundation
import UIKit

class SystemInfoApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        let device = UIDevice.current
        let screen = UIScreen.main
        let scale = screen.scale
        
        return [
            "brand": "Apple",
            "model": device.model,
            "pixelRatio": scale,
            "screenWidth": Int(screen.bounds.width * scale),
            "screenHeight": Int(screen.bounds.height * scale),
            "windowWidth": Int(screen.bounds.width * scale),
            "windowHeight": Int(screen.bounds.height * scale),
            "language": Locale.current.languageCode ?? "en",
            "version": "1.0.0",
            "system": "iOS \(device.systemVersion)",
            "platform": "ios",
            "fontSizeSetting": 16,
            "SDKVersion": "1.0.0",
            "benchmarkLevel": 1,
            "albumAuthorized": true,
            "cameraAuthorized": true,
            "locationAuthorized": true,
            "microphoneAuthorized": true,
            "notificationAuthorized": true,
            "bluetoothAuthorized": true
        ]
    }
}

class LoginApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        return [
            "code": "mock_code_\(Int(Date().timeIntervalSince1970))",
            "errMsg": "login:ok"
        ]
    }
}
