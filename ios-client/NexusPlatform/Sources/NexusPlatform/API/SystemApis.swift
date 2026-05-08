import Foundation
import UIKit

class SystemInfoApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        let language = AppLanguageStore.currentSync()
        let snapshot = await MainActor.run { () -> (model: String, scale: CGFloat, bounds: CGRect, systemVersion: String) in
            let device = UIDevice.current
            let screen = UIScreen.main
            return (device.model, screen.scale, screen.bounds, device.systemVersion)
        }

        return [
            "brand": "Apple",
            "model": snapshot.model,
            "pixelRatio": snapshot.scale,
            "screenWidth": Int(snapshot.bounds.width * snapshot.scale),
            "screenHeight": Int(snapshot.bounds.height * snapshot.scale),
            "windowWidth": Int(snapshot.bounds.width * snapshot.scale),
            "windowHeight": Int(snapshot.bounds.height * snapshot.scale),
            "language": language.runtimeLocaleTag,
            "version": "1.0.0",
            "system": "iOS \(snapshot.systemVersion)",
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

class MenuButtonRectApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        let horizontalPadding: CGFloat = 12
        let width: CGFloat = 88
        let height: CGFloat = 32
        let metrics = await MainActor.run { () -> (screenWidth: CGFloat, topInset: CGFloat) in
            let screenWidth = UIScreen.main.bounds.width
            let topInset = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap(\.windows)
                .first(where: \.isKeyWindow)?
                .safeAreaInsets.top ?? 0
            return (screenWidth, topInset)
        }
        let left = metrics.screenWidth - horizontalPadding - width
        let top = max(metrics.topInset + 8, 8)

        return [
            "left": left,
            "top": top,
            "right": left + width,
            "bottom": top + height,
            "width": width,
            "height": height
        ]
    }
}
