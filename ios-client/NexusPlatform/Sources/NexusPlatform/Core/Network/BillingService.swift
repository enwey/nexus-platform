import Foundation

struct BillingRecord: Identifiable, Sendable {
    let id: Int64
    let type: String
    let title: String
    let subtitle: String
    let amount: Decimal
    let createdAtText: String
}

struct BillingDetail: Sendable {
    let id: Int64
    let type: String
    let title: String
    let subtitle: String
    let amount: Decimal
    let createdAtText: String
    let receiptURL: String
}

enum BillingServiceError: LocalizedError {
    case unauthorized
    case invalidResponse
    case failed(String)

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return "请先登录"
        case .invalidResponse:
            return "账单数据响应无效"
        case .failed(let message):
            return message.isEmpty ? "获取账单失败" : message
        }
    }
}

protocol BillingServiceProtocol: Sendable {
    func fetchBillingList(limit: Int) async throws -> [BillingRecord]
    func fetchBillingDetail(id: Int64) async throws -> BillingDetail
}

struct BillingService: BillingServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchBillingList(limit: Int = 20) async throws -> [BillingRecord] {
        var components = URLComponents(url: baseURL.appendingPathComponent("wallet/billing/list"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "limit", value: "\(max(1, min(limit, 100)))")]
        guard let url = components?.url else {
            throw BillingServiceError.invalidResponse
        }

        guard let payload = try await request(pathURL: url) as? [[String: Any]] else {
            return []
        }

        return payload.compactMap(parseRecord(raw:))
    }

    func fetchBillingDetail(id: Int64) async throws -> BillingDetail {
        let url = baseURL.appendingPathComponent("wallet/billing/\(id)")
        guard let payload = try await request(pathURL: url) as? [String: Any] else {
            throw BillingServiceError.invalidResponse
        }
        return parseDetail(raw: payload, fallbackID: id)
    }

    private func authorizationHeader() async -> String? {
        guard let session = await AuthSessionStore.shared.current() else {
            return nil
        }
        return "Bearer \(session.accessToken)"
    }

    private func request(pathURL url: URL) async throws -> Any {
        return try await client.request(url: url, authMode: .required)
    }

    private func parseRecord(raw: [String: Any]) -> BillingRecord? {
        let idValue: Int64
        if let id = raw["id"] as? Int64 {
            idValue = id
        } else if let id = raw["id"] as? Int {
            idValue = Int64(id)
        } else if let idString = raw["id"] as? String, let id = Int64(idString) {
            idValue = id
        } else {
            return nil
        }

        return BillingRecord(
            id: idValue,
            type: raw["type"] as? String ?? "",
            title: raw["title"] as? String ?? "账单记录",
            subtitle: raw["subtitle"] as? String ?? "",
            amount: decimal(raw["amount"]),
            createdAtText: raw["createdAt"] as? String ?? ""
        )
    }

    private func parseDetail(raw: [String: Any], fallbackID: Int64) -> BillingDetail {
        let idValue: Int64
        if let id = raw["id"] as? Int64 {
            idValue = id
        } else if let id = raw["id"] as? Int {
            idValue = Int64(id)
        } else if let idString = raw["id"] as? String, let id = Int64(idString) {
            idValue = id
        } else {
            idValue = fallbackID
        }

        return BillingDetail(
            id: idValue,
            type: raw["type"] as? String ?? "",
            title: raw["title"] as? String ?? "账单详情",
            subtitle: raw["subtitle"] as? String ?? "",
            amount: decimal(raw["amount"]),
            createdAtText: raw["createdAt"] as? String ?? "",
            receiptURL: raw["receiptUrl"] as? String ?? ""
        )
    }

    private func decimal(_ any: Any?) -> Decimal {
        if let value = any as? NSNumber {
            return value.decimalValue
        }
        if let value = any as? String {
            return Decimal(string: value) ?? 0
        }
        return 0
    }
}
