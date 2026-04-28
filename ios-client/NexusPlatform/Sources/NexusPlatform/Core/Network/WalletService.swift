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
            return AppText.pleaseLogin()
        case .invalidResponse:
            return AppText.invalidWalletResponse()
        case .failed(let message):
            return message.isEmpty ? AppText.fetchWalletFailed() : message
        }
    }
}

protocol WalletServiceProtocol: Sendable {
    func fetchSummary() async throws -> WalletSummary
}

struct WalletService: WalletServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = BackendPinnedSession.shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchSummary() async throws -> WalletSummary {
        guard let payload = try await client.request(path: "wallet/summary", authMode: .required) as? [String: Any] else {
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
