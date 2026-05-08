import Foundation

struct RuntimeProfile: Sendable {
    let appID: String
    let gameName: String
    let studioName: String
    let playerCountText: String
    let packageSizeBytes: Int64?
    let categoryPlayerCount: Int64?
    let categoryRank: Int?
    let categoryName: String
    let runtimeBannerURL: String
    let runtimeLogoURL: String
    let shareTitle: String
    let shareSubtitle: String
    let shareImageURL: String
}

protocol GameRuntimeProfileServiceProtocol: Sendable {
    func fetchRuntimeProfile(appID: String) async throws -> RuntimeProfile?
}

struct GameRuntimeProfileService: GameRuntimeProfileServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = BackendPinnedSession.shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchRuntimeProfile(appID: String) async throws -> RuntimeProfile? {
        guard appID.isEmpty == false else {
            return nil
        }
        let url = baseURL.appendingPathComponent("game").appendingPathComponent(appID).appendingPathComponent("runtime-profile")
        guard let payload = try await client.request(url: url, authMode: .optional) as? [String: Any] else {
            return nil
        }

        return RuntimeProfile(
            appID: payload["appId"] as? String ?? appID,
            gameName: payload["gameName"] as? String ?? "",
            studioName: payload["studioName"] as? String ?? "",
            playerCountText: payload["playerCountText"] as? String ?? "",
            packageSizeBytes: parseInt64(payload["packageSizeBytes"]),
            categoryPlayerCount: parseInt64(payload["categoryPlayerCount"]),
            categoryRank: payload["categoryRank"] as? Int,
            categoryName: payload["categoryName"] as? String ?? "",
            runtimeBannerURL: payload["runtimeBannerUrl"] as? String ?? "",
            runtimeLogoURL: payload["runtimeLogoUrl"] as? String ?? "",
            shareTitle: payload["shareTitle"] as? String ?? "",
            shareSubtitle: payload["shareSubtitle"] as? String ?? "",
            shareImageURL: payload["shareImageUrl"] as? String ?? ""
        )
    }

    private func parseInt64(_ value: Any?) -> Int64? {
        switch value {
        case let number as Int64:
            return number
        case let number as Int:
            return Int64(number)
        case let number as NSNumber:
            return number.int64Value
        case let text as String:
            return Int64(text)
        default:
            return nil
        }
    }
}
