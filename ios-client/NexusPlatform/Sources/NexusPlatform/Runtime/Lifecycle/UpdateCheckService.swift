import Foundation

struct GameUpdateInfo: Sendable {
    let hasUpdate: Bool
    let forceUpdate: Bool
    let latestVersion: String?
    let downloadURL: URL?
    let md5: String?
}

protocol GameUpdateCheckServiceProtocol: Sendable {
    func checkUpdate(appID: String, localVersion: String) async throws -> GameUpdateInfo
}

enum GameUpdateCheckError: LocalizedError {
    case invalidURL
    case badResponse
    case cannotParse
    case backend(message: String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "更新检查地址无效"
        case .badResponse:
            return "更新检查服务不可用"
        case .cannotParse:
            return "更新检查返回格式错误"
        case .backend(let message):
            return message.isEmpty ? "更新检查失败" : message
        }
    }
}

struct BackendEnvironment {
    let apiBaseURL: URL

    static func current() -> BackendEnvironment {
        if let raw = ProcessInfo.processInfo.environment["BACKEND_BASE_URL"],
           let url = URL(string: raw) {
            return BackendEnvironment(apiBaseURL: url)
        }
        return BackendEnvironment(apiBaseURL: URL(string: "http://localhost:8080/api/v1")!)
    }
}

struct BackendGameUpdateService: GameUpdateCheckServiceProtocol {
    private let baseURL: URL
    private let session: URLSession

    init(environment: BackendEnvironment = .current(), session: URLSession = .shared) {
        self.baseURL = environment.apiBaseURL
        self.session = session
    }

    func checkUpdate(appID: String, localVersion: String) async throws -> GameUpdateInfo {
        var components = URLComponents(url: baseURL.appendingPathComponent("game/check-update"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "appId", value: appID),
            URLQueryItem(name: "localVersion", value: localVersion)
        ]

        guard let url = components?.url else {
            throw GameUpdateCheckError.invalidURL
        }

        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw GameUpdateCheckError.badResponse
        }

        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw GameUpdateCheckError.cannotParse
        }

        let code = object["code"] as? Int ?? -1
        if code != 0 {
            let message = object["message"] as? String ?? "更新检查失败"
            throw GameUpdateCheckError.backend(message: message)
        }

        guard let dataObject = object["data"] as? [String: Any] else {
            return GameUpdateInfo(hasUpdate: false, forceUpdate: false, latestVersion: nil, downloadURL: nil, md5: nil)
        }

        let hasUpdate = dataObject["hasUpdate"] as? Bool ?? false
        let forceUpdate = dataObject["forceUpdate"] as? Bool ?? false
        let latestVersion = dataObject["latestVersion"] as? String
        let md5 = dataObject["md5"] as? String
        let downloadRaw = dataObject["downloadUrl"] as? String

        let resolvedURL: URL?
        if let downloadRaw, downloadRaw.isEmpty == false {
            resolvedURL = normalize(downloadRaw: downloadRaw, apiBaseURL: baseURL)
        } else {
            resolvedURL = nil
        }

        return GameUpdateInfo(
            hasUpdate: hasUpdate,
            forceUpdate: forceUpdate,
            latestVersion: latestVersion,
            downloadURL: resolvedURL,
            md5: md5
        )
    }

    private func normalize(downloadRaw: String, apiBaseURL: URL) -> URL? {
        if downloadRaw.hasPrefix("/") {
            return URL(string: downloadRaw, relativeTo: apiBaseURL)?.absoluteURL
        }
        return URL(string: downloadRaw)
    }
}
