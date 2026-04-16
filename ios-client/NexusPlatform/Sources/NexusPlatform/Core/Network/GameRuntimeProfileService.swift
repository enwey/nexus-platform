import Foundation

struct RuntimeProfile: Sendable {
    let appID: String
    let gameName: String
    let studioName: String
    let playerCountText: String
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

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
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
            runtimeBannerURL: payload["runtimeBannerUrl"] as? String ?? "",
            runtimeLogoURL: payload["runtimeLogoUrl"] as? String ?? "",
            shareTitle: payload["shareTitle"] as? String ?? "",
            shareSubtitle: payload["shareSubtitle"] as? String ?? "",
            shareImageURL: payload["shareImageUrl"] as? String ?? ""
        )
    }
}
