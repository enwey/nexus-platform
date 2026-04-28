import Foundation

struct LibraryHomeSnapshot: Sendable {
    let currentPlayingGame: Game?
    let recentGames: [Game]
    let myGames: [Game]
    let newbieMustPlay: [Game]
    let everyonePlaying: [Game]
    let favoriteCount: Int
    let shareCount: Int
}

enum LibraryHomeServiceError: LocalizedError {
    case unauthorized
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return AppText.pleaseLogin()
        case .invalidResponse:
            return AppText.invalidLibraryResponse()
        }
    }
}

protocol LibraryHomeServiceProtocol: Sendable {
    func fetchHome() async throws -> LibraryHomeSnapshot
    func markPlayed(appID: String) async
    func setFavorite(appID: String, favorite: Bool) async throws
    func markShared(appID: String) async throws
}

struct LibraryHomeService: LibraryHomeServiceProtocol {
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

    func fetchHome() async throws -> LibraryHomeSnapshot {
        let data = try await request(path: "library/home", method: "GET", body: nil)
        guard let payload = data as? [String: Any] else {
            throw LibraryHomeServiceError.invalidResponse
        }
        let currentPlaying = decodeGame(payload["currentPlayingGame"])
        let recentGames = decodeGames(payload["recentGames"])
        let myGames = decodeGames(payload["myGames"])
        let newbieGames = decodeGames(payload["newbieMustPlay"])
        let everyonePlaying = decodeGames(payload["everyonePlaying"])
        let resolvedCurrentPlaying = await mergeOptionalGame(currentPlaying)
        let resolvedRecentGames = await metadataResolver.merge(recentGames)
        let resolvedMyGames = await metadataResolver.merge(myGames)
        let resolvedNewbieGames = await metadataResolver.merge(newbieGames)
        let resolvedEveryonePlaying = await metadataResolver.merge(everyonePlaying)

        return LibraryHomeSnapshot(
            currentPlayingGame: resolvedCurrentPlaying,
            recentGames: resolvedRecentGames,
            myGames: resolvedMyGames,
            newbieMustPlay: resolvedNewbieGames,
            everyonePlaying: resolvedEveryonePlaying,
            favoriteCount: payload["favoriteCount"] as? Int ?? 0,
            shareCount: payload["shareCount"] as? Int ?? 0
        )
    }

    func markPlayed(appID: String) async {
        guard appID.isEmpty == false else { return }
        _ = try? await request(path: "library/\(appID)/play", method: "POST", body: [:])
    }

    func setFavorite(appID: String, favorite: Bool) async throws {
        let method = favorite ? "POST" : "DELETE"
        _ = try await request(path: "library/\(appID)/favorite", method: method, body: favorite ? [:] : nil)
    }

    func markShared(appID: String) async throws {
        _ = try await request(path: "library/\(appID)/share", method: "POST", body: [:])
    }

    private func request(path: String, method: String, body: [String: Any]?) async throws -> Any {
        return try await client.request(path: path, method: method, body: body, authMode: .required)
    }

    private func decodeGames(_ object: Any?) -> [Game] {
        guard let rows = object as? [[String: Any]] else {
            return []
        }
        return rows.compactMap(decodeGame)
    }

    private func decodeGame(_ object: Any?) -> Game? {
        guard let row = object as? [String: Any] else {
            return nil
        }
        var normalized = row
        normalized["id"] = row["appId"] ?? row["id"] ?? UUID().uuidString
        normalized["description"] = row["description"] ?? ""
        normalized["iconUrl"] = row["iconUrl"] ?? row["coverUrl"] ?? ""
        if let rawURL = row["downloadUrl"] as? String {
            normalized["downloadUrl"] = normalizeBackendURL(rawURL)
        } else {
            normalized["downloadUrl"] = ""
        }
        normalized["version"] = row["version"] ?? "0.0.0"
        normalized["md5"] = row["md5"] ?? ""
        guard let json = try? JSONSerialization.data(withJSONObject: normalized),
              let game = try? JSONDecoder().decode(Game.self, from: json) else {
            return nil
        }
        return game
    }

    private func mergeOptionalGame(_ game: Game?) async -> Game? {
        guard let game else {
            return nil
        }
        return await metadataResolver.merge(game)
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
