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

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchProfile() async throws -> UserProfileDetail {
        guard let auth = await authorizationHeader() else {
            throw UserProfileServiceError.unauthorized
        }

        var request = URLRequest(url: baseURL.appendingPathComponent("user/profile"))
        request.httpMethod = "GET"
        request.setValue(auth, forHTTPHeaderField: "Authorization")

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw UserProfileServiceError.invalidResponse
        }
        if http.statusCode == 401 || http.statusCode == 403 {
            throw UserProfileServiceError.unauthorized
        }
        guard (200..<300).contains(http.statusCode) else {
            throw UserProfileServiceError.invalidResponse
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int,
              code == 0,
              let payload = root["data"] as? [String: Any] else {
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

    private func authorizationHeader() async -> String? {
        guard let session = await AuthSessionStore.shared.current() else {
            return nil
        }
        return "Bearer \(session.accessToken)"
    }
}
