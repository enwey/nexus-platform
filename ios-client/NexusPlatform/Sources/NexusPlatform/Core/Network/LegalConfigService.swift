import Foundation

struct LegalLinks: Sendable {
    let termsURL: URL
    let privacyURL: URL
}

protocol LegalConfigServiceProtocol: Sendable {
    func fetchLinks() async -> LegalLinks
}

struct LegalConfigService: LegalConfigServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = BackendPinnedSession.shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchLinks() async -> LegalLinks {
        do {
            if let payload = try await client.request(path: "public/legal/config") as? [String: Any],
               let links = parse(payload: payload) {
                return links
            }
        } catch {
            // Fall back to the default public legal endpoints used on Android.
        }
        return fallbackLinks()
    }

    private func parse(payload: [String: Any]) -> LegalLinks? {
        guard
            let terms = payload["termsUrl"] as? String,
            let privacy = payload["privacyUrl"] as? String,
            let termsURL = URL(string: terms),
            let privacyURL = URL(string: privacy)
        else {
            return nil
        }
        return LegalLinks(termsURL: termsURL, privacyURL: privacyURL)
    }

    private func fallbackLinks() -> LegalLinks {
        let termsURL = baseURL.appendingPathComponent("public/legal/terms")
        let privacyURL = baseURL.appendingPathComponent("public/legal/privacy")
        return LegalLinks(termsURL: termsURL, privacyURL: privacyURL)
    }
}
