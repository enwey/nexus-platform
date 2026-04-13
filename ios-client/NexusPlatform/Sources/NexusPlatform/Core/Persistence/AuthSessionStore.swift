import Foundation

struct AuthSession: Codable, Sendable {
    let accessToken: String
    let refreshToken: String
    let email: String
}

actor AuthSessionStore {
    static let shared = AuthSessionStore()

    private let defaults = UserDefaults.standard
    private let sessionKey = "nexus.auth.session"

    func save(_ session: AuthSession) {
        if let data = try? JSONEncoder().encode(session) {
            defaults.set(data, forKey: sessionKey)
        }
        defaults.set(session.accessToken, forKey: "access_token")
        defaults.set(session.accessToken, forKey: "token")
        defaults.set(session.accessToken, forKey: "auth_token")
        defaults.set("Bearer \(session.accessToken)", forKey: "authorization")
    }

    func current() -> AuthSession? {
        guard let data = defaults.data(forKey: sessionKey),
              let session = try? JSONDecoder().decode(AuthSession.self, from: data) else {
            return nil
        }
        return session
    }

    func clear() {
        defaults.removeObject(forKey: sessionKey)
        defaults.removeObject(forKey: "access_token")
        defaults.removeObject(forKey: "token")
        defaults.removeObject(forKey: "auth_token")
        defaults.removeObject(forKey: "authorization")
    }
}
