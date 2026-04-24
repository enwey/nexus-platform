import Foundation

enum AuthError: LocalizedError {
    case invalidResponse(path: String)
    case loginFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse(let path):
            return "服务响应无效\n\(path)"
        case .loginFailed(let message):
            return message.isEmpty ? "登录失败" : message
        }
    }
}

protocol AuthServiceProtocol: Sendable {
    func login(email: String, password: String) async throws -> AuthSession
    func sendCode(email: String, purpose: String, source: String, scene: String?) async throws
    func register(email: String, password: String, code: String, accountType: String) async throws -> AuthSession
    func resetPassword(email: String, code: String, newPassword: String) async throws
    func logout(accessToken: String?) async throws
    func terminateAccount(accessToken: String?, confirmText: String) async throws
}

struct AuthService: AuthServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = BackendPinnedSession.shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func login(email: String, password: String) async throws -> AuthSession {
        let path = "user/login"
        let payload = try await postObject(path: path, body: [
            "email": email,
            "password": password
        ])
        return try parseAuthSession(from: payload, email: email, path: path)
    }

    func sendCode(email: String, purpose: String, source: String = "ios-client", scene: String? = nil) async throws {
        var payload: [String: Any] = [
            "email": email,
            "purpose": purpose,
            "source": source
        ]
        if let scene, scene.isEmpty == false {
            payload["scene"] = scene
        }
        try await postVoid(path: "user/send-code", body: payload, extraHeaders: [
            "X-Client-Source": "ios-client",
            "X-Client-Scene": scene ?? ""
        ])
    }

    func register(email: String, password: String, code: String, accountType: String = "PLAYER") async throws -> AuthSession {
        let path = "user/register"
        let payload = try await postObject(path: path, body: [
            "email": email,
            "password": password,
            "code": code,
            "accountType": accountType
        ])
        return try parseAuthSession(from: payload, email: email, path: path)
    }

    func resetPassword(email: String, code: String, newPassword: String) async throws {
        try await postVoid(path: "user/password/reset", body: [
            "email": email,
            "code": code,
            "newPassword": newPassword
        ])
    }

    func logout(accessToken: String?) async throws {
        let storedToken = await AuthSessionStore.shared.current()?.accessToken
        let token = normalizedToken(accessToken) ?? storedToken
        _ = try await client.request(
            path: "user/logout",
            method: "POST",
            body: [:],
            authMode: token == nil ? .none : .required
        )
    }

    func terminateAccount(accessToken: String?, confirmText: String) async throws {
        _ = try await client.request(
            path: "user/terminate",
            method: "POST",
            body: ["confirmText": confirmText],
            authMode: .required
        )
    }

    private func postObject(path: String, body: [String: Any], extraHeaders: [String: String] = [:]) async throws -> [String: Any] {
        guard let payload = try await client.request(
            path: path,
            method: "POST",
            body: body,
            authMode: .none,
            extraHeaders: extraHeaders
        ) as? [String: Any] else {
            throw AuthError.invalidResponse(path: path)
        }
        return payload
    }

    private func postVoid(path: String, body: [String: Any], extraHeaders: [String: String] = [:]) async throws {
        _ = try await client.request(
            path: path,
            method: "POST",
            body: body,
            authMode: .none,
            extraHeaders: extraHeaders
        )
    }

    private func parseAuthSession(from payload: [String: Any], email: String, path: String) throws -> AuthSession {
        guard let token = payload["token"] as? String,
              let refresh = payload["refreshToken"] as? String else {
            throw AuthError.invalidResponse(path: path)
        }
        return AuthSession(accessToken: token, refreshToken: refresh, email: email)
    }

    private func normalizedToken(_ value: String?) -> String? {
        guard let value, value.isEmpty == false else { return nil }
        if value.lowercased().hasPrefix("bearer ") {
            return String(value.dropFirst(7))
        }
        return value
    }
}
