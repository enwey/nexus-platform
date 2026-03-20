import Foundation
import UIKit
import Alamofire

class RequestApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        guard let urlString = params["url"] as? String,
              let url = URL(string: urlString) else {
            throw ApiError.invalidParameter
        }
        
        let method = params["method"] as? String ?? "GET"
        let data = params["data"]
        let headers = params["header"] as? [String: String] ?? [:]
        
        var httpMethod: HTTPMethod = .get
        switch method.uppercased() {
        case "POST":
            httpMethod = .post
        case "PUT":
            httpMethod = .put
        case "DELETE":
            httpMethod = .delete
        case "HEAD":
            httpMethod = .head
        default:
            httpMethod = .get
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = httpMethod.rawValue
        request.allHTTPHeaderFields = headers
        
        if let data = data, let jsonData = try? JSONSerialization.data(withJSONObject: data) {
            request.httpBody = jsonData
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        
        do {
            let (data, response) = try await AF.request(request).serializingData().value
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
            let responseBody = String(data: data, encoding: .utf8) ?? ""
            
            return [
                "statusCode": statusCode,
                "data": responseBody,
                "errMsg": "request:ok"
            ]
        } catch {
            return [
                "errMsg": "request:fail",
                "error": error.localizedDescription
            ]
        }
    }
}

class NetworkApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        return [
            "networkType": "wifi",
            "errMsg": "getNetworkType:ok"
        ]
    }
}

class ToastApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        guard let title = params["title"] as? String else {
            throw ApiError.invalidParameter
        }
        
        await MainActor.run {
            let alert = UIAlertController(title: nil, message: title, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "确定", style: .default))
            
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let rootViewController = windowScene.windows.first?.rootViewController {
                rootViewController.present(alert, animated: true)
            }
        }
        
        return ["errMsg": "showToast:ok"]
    }
}

class ModalApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        guard let title = params["title"] as? String,
              let content = params["content"] as? String else {
            throw ApiError.invalidParameter
        }
        
        let confirmText = params["confirmText"] as? String ?? "确定"
        let cancelText = params["cancelText"] as? String ?? "取消"
        
        return await withCheckedContinuation { continuation in
            DispatchQueue.main.async {
                let alert = UIAlertController(title: title, message: content, preferredStyle: .alert)
                
                alert.addAction(UIAlertAction(title: confirmText, style: .default) { _ in
                    continuation.resume(returning: ["confirm": true, "cancel": false, "errMsg": "showModal:ok"])
                })
                
                alert.addAction(UIAlertAction(title: cancelText, style: .cancel) { _ in
                    continuation.resume(returning: ["confirm": false, "cancel": true, "errMsg": "showModal:ok"])
                })
                
                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                   let rootViewController = windowScene.windows.first?.rootViewController {
                    rootViewController.present(alert, animated: true)
                }
            }
        }
    }
}
