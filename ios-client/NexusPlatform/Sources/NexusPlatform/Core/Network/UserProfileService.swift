import Foundation

struct UserProfileDetail: Sendable {
    let id: Int64
    let email: String
    let displayName: String
    let avatarURL: String
    let languageTag: String
}

enum UserProfileServiceError: LocalizedError {
    case unauthorized
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return "请先登录"
        case .invalidResponse:
            return "用户资料响应无效"
        }
    }
}

protocol UserProfileServiceProtocol: Sendable {
    func fetchProfile() async throws -> UserProfileDetail
}

struct UserProfileService: UserProfileServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchProfile() async throws -> UserProfileDetail {
        guard let payload = try await client.request(path: "user/profile", authMode: .required) as? [String: Any] else {
            throw UserProfileServiceError.invalidResponse
        }

        return UserProfileDetail(
            id: payload["id"] as? Int64 ?? Int64(payload["id"] as? Int ?? 0),
            email: payload["email"] as? String ?? "",
            displayName: payload["displayName"] as? String ?? "",
            avatarURL: payload["avatarUrl"] as? String ?? "",
            languageTag: payload["languageTag"] as? String ?? ""
        )
    }
}
