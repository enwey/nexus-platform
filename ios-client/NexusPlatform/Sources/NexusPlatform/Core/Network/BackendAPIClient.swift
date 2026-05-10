import Foundation
import CryptoKit
import Security

enum BackendAuthMode {
    case none
    case optional
    case required
}

enum BackendAPIClientError: LocalizedError {
    case unauthorized
    case invalidResponse
    case backend(String)
    case network(url: String, message: String)

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return AppText.pleaseLogin()
        case .invalidResponse:
            return AppText.invalidServiceResponse()
        case .backend(let message):
            return message.isEmpty ? AppText.requestFailed() : message
        case let .network(url, message):
            return "\(message)\n\(url)"
        }
    }
}

actor BackendTokenRefresher {
    static let shared = BackendTokenRefresher()

    func refreshIfNeeded(staleAccessToken: String, session: URLSession, baseURL: URL) async -> String? {
        let latest = await AuthSessionStore.shared.current()
        if let latest, latest.accessToken.isEmpty == false, latest.accessToken != staleAccessToken {
            return latest.accessToken
        }

        guard let current = latest else {
            return nil
        }

        var request = URLRequest(url: baseURL.appendingPathComponent("user/refresh"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["refreshToken": current.refreshToken])

        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                if let http = response as? HTTPURLResponse, http.statusCode == 401 || http.statusCode == 403 {
                    await AuthSessionStore.shared.clear()
                }
                return nil
            }
            guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let code = root["code"] as? Int,
                  code == 0,
                  let payload = root["data"] as? [String: Any],
                  let accessToken = payload["token"] as? String,
                  let refreshToken = payload["refreshToken"] as? String else {
                return nil
            }
            let sessionValue = AuthSession(
                accessToken: accessToken,
                refreshToken: refreshToken,
                email: current.email
            )
            await AuthSessionStore.shared.save(sessionValue)
            return accessToken
        } catch {
            return nil
        }
    }
}

struct BackendAPIClient {
    let session: URLSession
    let baseURL: URL

    func request(
        path: String,
        method: String = "GET",
        body: [String: Any]? = nil,
        authMode: BackendAuthMode = .none,
        extraHeaders: [String: String] = [:]
    ) async throws -> Any {
        let url = baseURL.appendingPathComponent(path)
        return try await request(url: url, method: method, body: body, authMode: authMode, extraHeaders: extraHeaders)
    }

    func request(
        url: URL,
        method: String = "GET",
        body: [String: Any]? = nil,
        authMode: BackendAuthMode = .none,
        extraHeaders: [String: String] = [:]
    ) async throws -> Any {
        let sessionState = await AuthSessionStore.shared.current()
        let initialAccessToken = sessionState?.accessToken

        if authMode == .required, initialAccessToken.isNilOrEmpty {
            throw BackendAPIClientError.unauthorized
        }

        let first = try await sendRaw(
            url: url,
            method: method,
            body: body,
            accessToken: authMode == .none ? nil : initialAccessToken,
            extraHeaders: extraHeaders
        )

        if first.response.statusCode == 401 || first.response.statusCode == 403 {
            switch authMode {
            case .none:
                throw BackendAPIClientError.unauthorized
            case .required:
                guard
                    let staleToken = initialAccessToken,
                    let refreshed = await BackendTokenRefresher.shared.refreshIfNeeded(
                        staleAccessToken: staleToken,
                        session: session,
                        baseURL: baseURL
                    )
                else {
                    throw BackendAPIClientError.unauthorized
                }
                let retry = try await sendRaw(
                    url: url,
                    method: method,
                    body: body,
                    accessToken: refreshed,
                    extraHeaders: extraHeaders
                )
                return try parse(retry.data, response: retry.response)
            case .optional:
                if
                    let staleToken = initialAccessToken,
                    let refreshed = await BackendTokenRefresher.shared.refreshIfNeeded(
                        staleAccessToken: staleToken,
                        session: session,
                        baseURL: baseURL
                    )
                {
                    let retry = try await sendRaw(
                        url: url,
                        method: method,
                        body: body,
                        accessToken: refreshed,
                        extraHeaders: extraHeaders
                    )
                    return try parse(retry.data, response: retry.response)
                }

                let anonymousRetry = try await sendRaw(
                    url: url,
                    method: method,
                    body: body,
                    accessToken: nil,
                    extraHeaders: extraHeaders
                )
                return try parse(anonymousRetry.data, response: anonymousRetry.response)
            }
        }

        return try parse(first.data, response: first.response)
    }

    private func sendRaw(
        url: URL,
        method: String,
        body: [String: Any]?,
        accessToken: String?,
        extraHeaders: [String: String]
    ) async throws -> (data: Data, response: HTTPURLResponse) {
        var request = URLRequest(url: url)
        request.httpMethod = method

        if let accessToken, accessToken.isEmpty == false {
            request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        }
        extraHeaders.forEach { key, value in
            request.setValue(value, forHTTPHeaderField: key)
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError {
            throw BackendAPIClientError.network(url: url.absoluteString, message: error.localizedDescription)
        } catch {
            throw BackendAPIClientError.network(url: url.absoluteString, message: error.localizedDescription)
        }
        guard let http = response as? HTTPURLResponse else {
            throw BackendAPIClientError.invalidResponse
        }
        return (data, http)
    }

    private func parse(_ data: Data, response: HTTPURLResponse) throws -> Any {
        guard (200..<300).contains(response.statusCode) else {
            throw BackendAPIClientError.invalidResponse
        }

        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let code = root["code"] as? Int else {
            throw BackendAPIClientError.invalidResponse
        }

        if code != 0 {
            throw BackendAPIClientError.backend(root["message"] as? String ?? "")
        }

        return root["data"] as Any
    }
}

enum BackendPinnedSession {
    static let shared: URLSession = {
        let config = URLSessionConfiguration.default
        let delegate = BackendPinningDelegate()
        return URLSession(configuration: config, delegate: delegate, delegateQueue: nil)
    }()
}

private final class BackendPinningDelegate: NSObject, URLSessionDelegate {
    private let expectedCertSHA256Pins: Set<String>

    override init() {
        if let raw = ProcessInfo.processInfo.environment["BACKEND_CERT_SHA256"], raw.isEmpty == false {
            self.expectedCertSHA256Pins = Self.parsePins(raw)
        } else if let raw = Bundle.main.object(forInfoDictionaryKey: "BACKEND_CERT_SHA256") as? String, raw.isEmpty == false {
            self.expectedCertSHA256Pins = Self.parsePins(raw)
        } else {
            self.expectedCertSHA256Pins = []
        }
        super.init()
    }

    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        guard let environment = try? BackendEnvironment.current() else {
            completionHandler(.performDefaultHandling, nil)
            return
        }

        let pinnedHost = environment.apiBaseURL.host
        let isHTTPS = environment.apiBaseURL.scheme?.lowercased() == "https"
        guard challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
              isHTTPS,
              expectedCertSHA256Pins.isEmpty == false,
              let pinnedHost,
              challenge.protectionSpace.host == pinnedHost,
              let trust = challenge.protectionSpace.serverTrust,
              let certificateChain = SecTrustCopyCertificateChain(trust) as? [SecCertificate],
              let certificate = certificateChain.first else {
            completionHandler(.performDefaultHandling, nil)
            return
        }

        let certificateData = SecCertificateCopyData(certificate) as Data
        let actual = SHA256.hash(data: certificateData).map { String(format: "%02x", $0) }.joined()
        guard expectedCertSHA256Pins.contains(actual) else {
            completionHandler(.cancelAuthenticationChallenge, nil)
            return
        }

        completionHandler(.useCredential, URLCredential(trust: trust))
    }

    private static func parsePins(_ raw: String) -> Set<String> {
        Set(
            raw
                .components(separatedBy: CharacterSet(charactersIn: ",;\n\r"))
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
                .filter { $0.isEmpty == false }
        )
    }
}

private extension Optional where Wrapped == String {
    var isNilOrEmpty: Bool {
        self?.isEmpty ?? true
    }
}
