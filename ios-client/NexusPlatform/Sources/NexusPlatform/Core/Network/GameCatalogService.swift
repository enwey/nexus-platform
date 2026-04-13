import Foundation

protocol GameCatalogServiceProtocol: Sendable {
    func fetchGames() async throws -> [Game]
    func fetchGame(appID: String) async throws -> Game?
}

struct GameCatalogService: GameCatalogServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchGames() async throws -> [Game] {
        let primary = baseURL.appendingPathComponent("game/public/list")
        let fallback = baseURL.appendingPathComponent("game/list")

        if let games = try await fetch(from: primary) {
            return games
        }
        if let games = try await fetch(from: fallback) {
            return games
        }
        return mockedGames()
    }

    func fetchGame(appID: String) async throws -> Game? {
        guard appID.isEmpty == false else {
            return nil
        }
        let target = baseURL.appendingPathComponent("game").appendingPathComponent(appID)
        let (data, response) = try await session.data(from: target)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            return nil
        }

        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = object["code"] as? Int,
              code == 0,
              let payload = object["data"] as? [String: Any] else {
            return nil
        }

        let normalized = normalizeGameDict(payload)
        let json = try JSONSerialization.data(withJSONObject: normalized)
        return try JSONDecoder().decode(Game.self, from: json)
    }

    private func fetch(from url: URL) async throws -> [Game]? {
        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            return nil
        }

        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = object["code"] as? Int,
              code == 0,
              let payload = object["data"] as? [[String: Any]] else {
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
        }
        if mutable["version"] == nil {
            mutable["version"] = "0.0.0"
        }
        if mutable["md5"] == nil {
            mutable["md5"] = ""
        }
        return mutable
    }

    private func mockedGames() -> [Game] {
        [
            Game(
                id: "demo-racing",
                name: "霓虹竞速",
                description: "高帧率街机竞速，60 秒一局",
                iconUrl: "",
                downloadUrl: "",
                version: "1.0.0",
                md5: "",
                category: "动作射击"
            ),
            Game(
                id: "demo-puzzle",
                name: "迷宫方块",
                description: "轻度益智，随开随停",
                iconUrl: "",
                downloadUrl: "",
                version: "1.0.0",
                md5: "",
                category: "休闲益智"
            ),
            Game(
                id: "demo-rpg",
                name: "异界旅人",
                description: "横版冒险，收集与成长",
                iconUrl: "",
                downloadUrl: "",
                version: "1.0.0",
                md5: "",
                category: "角色扮演"
            )
        ]
    }
}
