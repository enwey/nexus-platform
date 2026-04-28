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
            return AppText.pleaseLogin()
        case .invalidResponse:
            return AppText.invalidReferralResponse()
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
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = BackendPinnedSession.shared, env: BackendEnvironment = .current()) {
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
        let rows: [[String: Any]]
        if let list = data as? [[String: Any]] {
            rows = list
        } else if let payload = data as? [String: Any], let nested = payload["records"] as? [[String: Any]] {
            rows = nested
        } else {
            return []
        }
        return rows.enumerated().map { index, row in
            ReferralRecord(
                id: (row["id"] as? String) ?? String(row["id"] as? Int ?? index),
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
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw ReferralServiceError.invalidResponse
        }
        return try await client.request(url: url, method: method, body: body, authMode: .required)
    }
}
