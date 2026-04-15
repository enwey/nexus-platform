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
    func sendCode(email: String, purpose: String) async throws
    func register(email: String, password: String, code: String) async throws -> AuthSession
    func resetPassword(email: String, code: String, newPassword: String) async throws
    func logout(accessToken: String?) async throws
    func terminateAccount(accessToken: String?, confirmText: String) async throws
}

struct AuthService: AuthServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func login(email: String, password: String) async throws -> AuthSession {
        let payload = try await post(path: "user/login", body: [
            "email": email,
            "password": password
        ])
        return try parseAuthSession(from: payload, email: email)
    }

    func sendCode(email: String, purpose: String) async throws {
        _ = try await post(path: "user/send-code", body: [
            "email": email,
            "purpose": purpose
        ])
    }

    func register(email: String, password: String, code: String) async throws -> AuthSession {
        let payload = try await post(path: "user/register", body: [
            "email": email,
            "password": password,
            "code": code
        ])
        return try parseAuthSession(from: payload, email: email)
    }

    func resetPassword(email: String, code: String, newPassword: String) async throws {
        _ = try await post(path: "user/password/reset", body: [
            "email": email,
            "code": code,
            "newPassword": newPassword
        ])
    }

    func logout(accessToken: String?) async throws {
        let url = baseURL.appendingPathComponent("user/logout")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        if let accessToken, accessToken.isEmpty == false {
            let hasBearer = accessToken.lowercased().hasPrefix("bearer ")
            let auth = hasBearer ? accessToken : "Bearer \(accessToken)"
            request.setValue(auth, forHTTPHeaderField: "Authorization")
        }

        let (_, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AuthError.invalidResponse
        }
    }

    func terminateAccount(accessToken: String?, confirmText: String) async throws {
        let url = baseURL.appendingPathComponent("user/terminate")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["confirmText": confirmText])
        if let accessToken, accessToken.isEmpty == false {
            let hasBearer = accessToken.lowercased().hasPrefix("bearer ")
            let auth = hasBearer ? accessToken : "Bearer \(accessToken)"
            request.setValue(auth, forHTTPHeaderField: "Authorization")
        }
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AuthError.invalidResponse
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int else {
            throw AuthError.invalidResponse
        }
        if code != 0 {
            throw AuthError.loginFailed(root["message"] as? String ?? "注销失败")
        }
    }

    private func post(path: String, body: [String: Any]) async throws -> [String: Any] {
        let url = baseURL.appendingPathComponent(path)
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AuthError.invalidResponse
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int else {
            throw AuthError.invalidResponse
        }
        if code != 0 {
            throw AuthError.loginFailed(root["message"] as? String ?? "请求失败")
        }
        return root["data"] as? [String: Any] ?? [:]
    }

    private func parseAuthSession(from payload: [String: Any], email: String) throws -> AuthSession {
        guard let token = payload["token"] as? String,
              let refresh = payload["refreshToken"] as? String else {
            throw AuthError.invalidResponse
        }
        return AuthSession(accessToken: token, refreshToken: refresh, email: email)
    }
}
