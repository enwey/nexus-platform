import Foundation

struct ReferralSummary: Sendable {
    let inviteCount: Int
    let totalReward: String
    let referralLink: String
}

struct ReferralRecord: Identifiable, Sendable {
    let id: String
    let title: String
    let subtitle: String
    let createdAt: String
    let reward: String
}

enum ReferralServiceError: LocalizedError {
    case unauthorized
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return "请先登录"
        case .invalidResponse:
            return "邀请数据响应无效"
        }
    }
}

protocol ReferralServiceProtocol: Sendable {
    func fetchSummary() async throws -> ReferralSummary
    func fetchRecords(limit: Int) async throws -> [ReferralRecord]
    func markShared(channel: String) async throws
}

struct ReferralService: ReferralServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchSummary() async throws -> ReferralSummary {
        let data = try await request(path: "referral/summary", method: "GET", body: nil)
        guard let payload = data as? [String: Any] else {
            throw ReferralServiceError.invalidResponse
        }
        return ReferralSummary(
            inviteCount: payload["inviteCount"] as? Int ?? 0,
            totalReward: payload["totalReward"] as? String ?? "0",
            referralLink: payload["referralLink"] as? String ?? ""
        )
    }

    func fetchRecords(limit: Int = 20) async throws -> [ReferralRecord] {
        let data = try await request(path: "referral/records?limit=\(max(1, limit))", method: "GET", body: nil)
        guard let rows = data as? [[String: Any]] else {
            return []
        }
        return rows.enumerated().map { index, row in
            ReferralRecord(
                id: row["id"] as? String ?? "\(index)",
                title: row["title"] as? String ?? "",
                subtitle: row["subtitle"] as? String ?? "",
                createdAt: row["createdAt"] as? String ?? "",
                reward: row["reward"] as? String ?? ""
            )
        }
    }

    func markShared(channel: String) async throws {
        _ = try await request(path: "referral/share", method: "POST", body: ["channel": channel])
    }

    private func request(path: String, method: String, body: [String: Any]?) async throws -> Any {
        guard let auth = await authorizationHeader() else {
            throw ReferralServiceError.unauthorized
        }
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw ReferralServiceError.invalidResponse
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue(auth, forHTTPHeaderField: "Authorization")
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ReferralServiceError.invalidResponse
        }
        if http.statusCode == 401 || http.statusCode == 403 {
            throw ReferralServiceError.unauthorized
        }
        guard (200..<300).contains(http.statusCode) else {
            throw ReferralServiceError.invalidResponse
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int,
              code == 0 else {
            throw ReferralServiceError.invalidResponse
        }
        return root["data"] as Any
    }

    private func authorizationHeader() async -> String? {
        guard let session = await AuthSessionStore.shared.current() else {
            return nil
        }
        return "Bearer \(session.accessToken)"
    }
}
