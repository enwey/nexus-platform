import Foundation

struct WalletSummary: Sendable {
    let balance: Decimal
    let frozenBalance: Decimal
    let availableBalance: Decimal
    let todayIncome: Decimal
    let totalIncome: Decimal
}

enum WalletServiceError: LocalizedError {
    case unauthorized
    case invalidResponse
    case failed(String)

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return "请先登录"
        case .invalidResponse:
            return "钱包数据响应无效"
        case .failed(let message):
            return message.isEmpty ? "获取钱包失败" : message
        }
    }
}

protocol WalletServiceProtocol: Sendable {
    func fetchSummary() async throws -> WalletSummary
}

struct WalletService: WalletServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchSummary() async throws -> WalletSummary {
        guard let auth = await authorizationHeader() else {
            throw WalletServiceError.unauthorized
        }

        let url = baseURL.appendingPathComponent("wallet/summary")
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(auth, forHTTPHeaderField: "Authorization")

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw WalletServiceError.invalidResponse
        }
        if http.statusCode == 401 || http.statusCode == 403 {
            throw WalletServiceError.unauthorized
        }
        guard (200..<300).contains(http.statusCode) else {
            throw WalletServiceError.invalidResponse
        }

        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int else {
            throw WalletServiceError.invalidResponse
        }
        if code != 0 {
            throw WalletServiceError.failed(root["message"] as? String ?? "")
        }
        guard let payload = root["data"] as? [String: Any] else {
            throw WalletServiceError.invalidResponse
        }

        return WalletSummary(
            balance: decimal(payload["balance"]),
            frozenBalance: decimal(payload["frozenBalance"]),
            availableBalance: decimal(payload["availableBalance"]),
            todayIncome: decimal(payload["todayIncome"]),
            totalIncome: decimal(payload["totalIncome"])
        )
    }

    private func authorizationHeader() async -> String? {
        guard let session = await AuthSessionStore.shared.current() else {
            return nil
        }
        return "Bearer \(session.accessToken)"
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
