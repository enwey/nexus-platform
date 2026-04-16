import Foundation

protocol GameCatalogServiceProtocol: Sendable {
    func fetchGames() async throws -> [Game]
    func fetchGame(appID: String) async throws -> Game?
}

struct GameCatalogService: GameCatalogServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private let metadataResolver: LocalGameMetadataResolver
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(
        session: URLSession = BackendPinnedSession.shared,
        env: BackendEnvironment = .current(),
        metadataResolver: LocalGameMetadataResolver = .shared
    ) {
        self.session = session
        self.baseURL = env.apiBaseURL
        self.metadataResolver = metadataResolver
    }

    func fetchGames() async throws -> [Game] {
        let primary = baseURL.appendingPathComponent("game/public/list")
        let fallback = baseURL.appendingPathComponent("game/list")

        if let games = try await fetch(from: primary, authMode: .optional) {
            return await metadataResolver.merge(games)
        }
        if let games = try await fetch(from: fallback, authMode: .optional) {
            return await metadataResolver.merge(games)
        }
        throw BackendAPIClientError.invalidResponse
    }

    func fetchGame(appID: String) async throws -> Game? {
        guard appID.isEmpty == false else {
            return nil
        }
        let target = baseURL.appendingPathComponent("game").appendingPathComponent(appID)
        guard let payload = try await client.request(url: target, authMode: .optional) as? [String: Any] else {
            return nil
        }

        let normalized = normalizeGameDict(payload)
        let json = try JSONSerialization.data(withJSONObject: normalized)
        let game = try JSONDecoder().decode(Game.self, from: json)
        return await metadataResolver.merge(game)
    }

    private func fetch(from url: URL, authMode: BackendAuthMode) async throws -> [Game]? {
        guard let payload = try await client.request(url: url, authMode: authMode) as? [[String: Any]] else {
            return nil
        }

        let normalized: [[String: Any]] = payload.map(normalizeGameDict)

        let json = try JSONSerialization.data(withJSONObject: normalized)
        return try JSONDecoder().decode([Game].self, from: json)
    }

    private func normalizeGameDict(_ raw: [String: Any]) -> [String: Any] {
        var mutable = raw
        if mutable["id"] == nil {
            mutable["id"] = mutable["appId"]
        }
        if mutable["description"] == nil {
            mutable["description"] = ""
        }
        if mutable["iconUrl"] == nil {
            mutable["iconUrl"] = ""
        }
        if mutable["downloadUrl"] == nil {
            mutable["downloadUrl"] = ""
        } else if let rawURL = mutable["downloadUrl"] as? String {
            mutable["downloadUrl"] = normalizeBackendURL(rawURL)
        }
        if mutable["version"] == nil {
            mutable["version"] = "0.0.0"
        }
        if mutable["md5"] == nil {
            mutable["md5"] = ""
        }
        return mutable
    }

    private func normalizeBackendURL(_ raw: String) -> String {
        guard raw.isEmpty == false else { return raw }
        guard var components = URLComponents(string: raw) else { return raw }
        let host = components.host?.lowercased() ?? ""
        if host == "localhost" || host == "127.0.0.1" || host == "::1" {
            components.scheme = baseURL.scheme
            components.host = baseURL.host
            components.port = baseURL.port
            return components.string ?? raw
        }
        return raw
    }
}
