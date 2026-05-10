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
            return AppText.updateCheckInvalidURL()
        case .badResponse:
            return AppText.updateCheckUnavailable()
        case .cannotParse:
            return AppText.updateCheckParseFailed()
        case .backend(let message):
            return message.isEmpty ? AppText.updateCheckFailed() : message
        }
    }
}

enum BackendEnvironmentError: LocalizedError, Sendable {
    case missingBaseURL
    case invalidBaseURL

    var errorDescription: String? {
        switch self {
        case .missingBaseURL:
            return "Missing backend API base URL configuration."
        case .invalidBaseURL:
            return "Invalid backend API base URL configuration."
        }
    }
}

struct BackendEnvironment {
    let apiBaseURL: URL

    static func current() throws -> BackendEnvironment {
        let candidates = [
            ProcessInfo.processInfo.environment["PLATFORM_API_BASE_URL"],
            ProcessInfo.processInfo.environment["BACKEND_BASE_URL"],
            Bundle.main.object(forInfoDictionaryKey: "PLATFORM_API_BASE_URL") as? String,
            Bundle.main.object(forInfoDictionaryKey: "BACKEND_BASE_URL") as? String
        ]

        for raw in candidates {
            guard let raw else { continue }
            let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard trimmed.isEmpty == false else { continue }
            guard let url = normalizedURL(from: trimmed) else {
                throw BackendEnvironmentError.invalidBaseURL
            }
            return BackendEnvironment(apiBaseURL: url)
        }

        throw BackendEnvironmentError.missingBaseURL
    }

    private static func normalizedURL(from raw: String) -> URL? {
        guard let url = URL(string: raw),
              let scheme = url.scheme?.lowercased(),
              ["http", "https"].contains(scheme),
              url.host?.isEmpty == false else {
            return nil
        }
        return url
    }
}

struct BackendGameUpdateService: GameUpdateCheckServiceProtocol {
    private let configuredBaseURL: URL?
    private let session: URLSession

    init(environment: BackendEnvironment? = nil, session: URLSession = BackendPinnedSession.shared) {
        self.configuredBaseURL = environment?.apiBaseURL
        self.session = session
    }

    func checkUpdate(appID: String, localVersion: String) async throws -> GameUpdateInfo {
        let baseURL: URL
        if let configuredBaseURL {
            baseURL = configuredBaseURL
        } else {
            baseURL = try BackendEnvironment.current().apiBaseURL
        }
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
            let message = object["message"] as? String ?? AppText.updateCheckFailed()
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
