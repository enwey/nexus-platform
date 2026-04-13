import Foundation

enum AuthError: LocalizedError {
    case invalidResponse
    case loginFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "登录响应无效"
        case .loginFailed(let message):
            return message.isEmpty ? "登录失败" : message
        }
    }
}

protocol AuthServiceProtocol: Sendable {
    func login(email: String, password: String) async throws -> AuthSession
}

struct AuthService: AuthServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func login(email: String, password: String) async throws -> AuthSession {
        let url = baseURL.appendingPathComponent("user/login")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "email": email,
            "password": password
        ])

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AuthError.invalidResponse
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int else {
            throw AuthError.invalidResponse
        }
        if code != 0 {
            throw AuthError.loginFailed(root["message"] as? String ?? "登录失败")
        }
        guard let payload = root["data"] as? [String: Any],
              let token = payload["token"] as? String,
              let refresh = payload["refreshToken"] as? String else {
            throw AuthError.invalidResponse
        }
        return AuthSession(accessToken: token, refreshToken: refresh, email: email)
    }
}
