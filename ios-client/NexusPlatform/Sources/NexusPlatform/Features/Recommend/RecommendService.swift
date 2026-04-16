import Foundation

protocol RecommendServiceProtocol: Sendable {
    func fetchToday(limit: Int) async throws -> [RecommendTodayItem]
}

struct RecommendService: RecommendServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = BackendPinnedSession.shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchToday(limit: Int = 10) async throws -> [RecommendTodayItem] {
        var components = URLComponents(url: baseURL.appendingPathComponent("discover/community"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "limit", value: String(max(1, min(limit, 20))))]
        guard let url = components?.url else {
            throw URLError(.badURL)
        }

        guard let rows = try await client.request(url: url, authMode: .optional) as? [[String: Any]] else {
            throw URLError(.cannotParseResponse)
        }

        return rows.map { row in
            let appID = row["appId"] as? String ?? ""
            return RecommendTodayItem(
                id: "\(appID)_\(row["cardTitle"] as? String ?? UUID().uuidString)",
                appID: appID,
                gameName: row["gameName"] as? String ?? "推荐游戏",
                gameIconURL: row["gameIconUrl"] as? String ?? "",
                gameCategory: row["gameCategory"] as? String ?? "",
                coverURL: row["coverUrl"] as? String ?? "",
                cardCategory: row["cardCategory"] as? String ?? "",
                cardTitle: row["cardTitle"] as? String ?? "",
                articleTag: row["articleTag"] as? String ?? "",
                articleTitle: row["articleTitle"] as? String ?? "",
                articleBody: row["articleBody"] as? String ?? "",
                actionText: row["actionText"] as? String ?? ""
            )
        }
    }
}
