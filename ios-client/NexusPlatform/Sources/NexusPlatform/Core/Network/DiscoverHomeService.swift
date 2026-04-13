import Foundation

struct DiscoverHero: Sendable {
    let appID: String
    let title: String
    let subtitle: String
    let coverURL: String
    let badgeText: String
}

struct DiscoverHomePayload: Sendable {
    let hero: DiscoverHero?
    let categories: [String]
    let rankedGames: [Game]
    let newbieGames: [Game]
}

protocol DiscoverHomeServiceProtocol: Sendable {
    func fetchHome(limit: Int) async throws -> DiscoverHomePayload
}

struct DiscoverHomeService: DiscoverHomeServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchHome(limit: Int = 20) async throws -> DiscoverHomePayload {
        var components = URLComponents(url: baseURL.appendingPathComponent("discover/home"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "limit", value: String(max(1, min(limit, 100))))]
        guard let url = components?.url else {
            throw URLError(.badURL)
        }

        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }

        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int,
              code == 0,
              let payload = root["data"] as? [String: Any] else {
            throw URLError(.cannotParseResponse)
        }

        let hero: DiscoverHero?
        if let heroDict = payload["hero"] as? [String: Any] {
            hero = DiscoverHero(
                appID: heroDict["appId"] as? String ?? "",
                title: heroDict["title"] as? String ?? "",
                subtitle: heroDict["subtitle"] as? String ?? "",
                coverURL: heroDict["coverUrl"] as? String ?? "",
                badgeText: heroDict["badgeText"] as? String ?? ""
            )
        } else {
            hero = nil
        }

        let categories = (payload["categories"] as? [String] ?? ["all"]).map { $0 == "all" ? "全部" : $0 }
        let ranked = decodeGames(payload["rankedGames"])
        let newbie = decodeGames(payload["newbieMustPlay"])
        return DiscoverHomePayload(hero: hero, categories: categories, rankedGames: ranked, newbieGames: newbie)
    }

    private func decodeGames(_ object: Any?) -> [Game] {
        guard let rawList = object as? [[String: Any]] else {
            return []
        }
        let normalized = rawList.map { raw -> [String: Any] in
            var out = raw
            out["id"] = raw["appId"] ?? raw["id"] ?? UUID().uuidString
            if out["iconUrl"] == nil {
                out["iconUrl"] = raw["coverUrl"] ?? ""
            }
            if out["downloadUrl"] == nil {
                out["downloadUrl"] = ""
            }
            if out["description"] == nil {
                out["description"] = ""
            }
            return out
        }
        guard let json = try? JSONSerialization.data(withJSONObject: normalized),
              let games = try? JSONDecoder().decode([Game].self, from: json) else {
            return []
        }
        return games
    }
}
