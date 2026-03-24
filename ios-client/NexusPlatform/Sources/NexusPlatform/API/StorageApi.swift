import Foundation

class StorageApi: ApiHandler {
    private let userDefaults = UserDefaults.standard
    
    func handle(api: String, params: [String: Any]) async throws -> Any {
        switch api {
        case "wx.setStorageSync":
        case "wx.setStorage":
            return try setStorage(params)
        case "wx.getStorageSync":
        case "wx.getStorage":
            return try getStorage(params)
        case "wx.removeStorageSync":
        case "wx.removeStorage":
            return try removeStorage(params)
        case "wx.clearStorageSync":
        case "wx.clearStorage":
            return clearStorage()
        default:
            throw ApiError.notImplemented
        }
    }
    
    private func setStorage(_ params: [String: Any]) throws -> [String: String] {
        guard let key = params["key"] as? String else {
            throw ApiError.invalidParameter
        }
        
        if let data = params["data"] {
            let jsonData = try JSONSerialization.data(withJSONObject: data)
            userDefaults.set(jsonData, forKey: key)
        }
        
        return ["errMsg": "setStorage:ok"]
    }
    
    private func getStorage(_ params: [String: Any]) throws -> [String: Any] {
        guard let key = params["key"] as? String else {
            throw ApiError.invalidParameter
        }
        
        guard let jsonData = userDefaults.data(forKey: key),
              let data = try? JSONSerialization.jsonObject(with: jsonData) else {
            return ["errMsg": "getStorage:fail"]
        }
        
        return [
            "data": data,
            "errMsg": "getStorage:ok"
        ]
    }
    
    private func removeStorage(_ params: [String: Any]) throws -> [String: String] {
        guard let key = params["key"] as? String else {
            throw ApiError.invalidParameter
        }
        
        userDefaults.removeObject(forKey: key)
        return ["errMsg": "removeStorage:ok"]
    }
    
    private func clearStorage() -> [String: String] {
        let dictionary = userDefaults.dictionaryRepresentation()
        dictionary.keys.forEach { key in
            userDefaults.removeObject(forKey: key)
        }
        return ["errMsg": "clearStorage:ok"]
    }
}

enum ApiError: Error {
    case notImplemented
    case invalidParameter
}
