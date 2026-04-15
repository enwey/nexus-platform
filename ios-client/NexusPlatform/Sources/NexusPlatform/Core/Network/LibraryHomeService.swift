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
            return "请先登录"
        case .invalidResponse:
            return "游戏库数据响应无效"
        }
    }
}

protocol LibraryHomeServiceProtocol: Sendable {
    func fetchHome() async throws -> LibraryHomeSnapshot
    func setFavorite(appID: String, favorite: Bool) async throws
    func markShared(appID: String) async throws
}

struct LibraryHomeService: LibraryHomeServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchHome() async throws -> LibraryHomeSnapshot {
        let data = try await request(path: "library/home", method: "GET", body: nil)
        guard let payload = data as? [String: Any] else {
            throw LibraryHomeServiceError.invalidResponse
        }
        return LibraryHomeSnapshot(
            currentPlayingGame: decodeGame(payload["currentPlayingGame"]),
            recentGames: decodeGames(payload["recentGames"]),
            myGames: decodeGames(payload["myGames"]),
            newbieMustPlay: decodeGames(payload["newbieMustPlay"]),
            everyonePlaying: decodeGames(payload["everyonePlaying"]),
            favoriteCount: payload["favoriteCount"] as? Int ?? 0,
            shareCount: payload["shareCount"] as? Int ?? 0
        )
    }

    func setFavorite(appID: String, favorite: Bool) async throws {
        let method = favorite ? "POST" : "DELETE"
        _ = try await request(path: "library/\(appID)/favorite", method: method, body: favorite ? [:] : nil)
    }

    func markShared(appID: String) async throws {
        _ = try await request(path: "library/\(appID)/share", method: "POST", body: [:])
    }

    private func request(path: String, method: String, body: [String: Any]?) async throws -> Any {
        guard let auth = await authorizationHeader() else {
            throw LibraryHomeServiceError.unauthorized
        }

        var request = URLRequest(url: baseURL.appendingPathComponent(path))
        request.httpMethod = method
        request.setValue(auth, forHTTPHeaderField: "Authorization")
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw LibraryHomeServiceError.invalidResponse
        }
        if http.statusCode == 401 || http.statusCode == 403 {
            throw LibraryHomeServiceError.unauthorized
        }
        guard (200..<300).contains(http.statusCode) else {
            throw LibraryHomeServiceError.invalidResponse
        }

        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int,
              code == 0 else {
            throw LibraryHomeServiceError.invalidResponse
        }
        return root["data"] as Any
    }

    private func authorizationHeader() async -> String? {
        guard let session = await AuthSessionStore.shared.current() else {
            return nil
        }
        return "Bearer \(session.accessToken)"
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
        normalized["downloadUrl"] = row["downloadUrl"] ?? ""
        normalized["version"] = row["version"] ?? "0.0.0"
        normalized["md5"] = row["md5"] ?? ""
        guard let json = try? JSONSerialization.data(withJSONObject: normalized),
              let game = try? JSONDecoder().decode(Game.self, from: json) else {
            return nil
        }
        return game
    }
}
