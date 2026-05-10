import Foundation
import CoreGraphics

struct LoginBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "login"

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        let timestamp = Int(Date().timeIntervalSince1970)
        return [
            "code": .string("ios_login_\(timestamp)"),
            "errMsg": .string("login:ok")
        ]
    }
}

struct RequestBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "request"
    private let client = BridgeRequestClient()

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        let result = try await client.send(params: params)
        return result
    }
}

actor BridgeStorageBox {
    private let defaults = UserDefaults.standard
    private let keyPrefix = "nexus.bridge.storage."

    func set(_ value: AnySendable, for key: String) {
        defaults.set(value.toFoundationObject(), forKey: keyPrefix + key)
    }

    func get(for key: String) -> AnySendable? {
        guard let raw = defaults.object(forKey: keyPrefix + key) else {
            return nil
        }
        return AnySendable.fromFoundation(raw)
    }

    func remove(for key: String) {
        defaults.removeObject(forKey: keyPrefix + key)
    }

    func clearAll() {
        let dict = defaults.dictionaryRepresentation()
        for key in dict.keys where key.hasPrefix(keyPrefix) {
            defaults.removeObject(forKey: key)
        }
    }
}

private struct BridgeRequestAccessPolicy {
    let backendBaseURL: URL

    var allowedHosts: Set<String> {
        guard let host = backendBaseURL.host?.lowercased(), host.isEmpty == false else {
            return []
        }
        return [host]
    }

    func resolveURL(from rawURL: String) throws -> URL {
        guard rawURL.isEmpty == false else {
            throw JSBridgeError(code: -1, message: "request:fail url required")
        }

        if rawURL.hasPrefix("/") {
            guard let merged = URL(string: rawURL, relativeTo: backendBaseURL)?.absoluteURL else {
                throw JSBridgeError(code: -1, message: "request:fail invalid url")
            }
            return merged
        }

        guard let absolute = URL(string: rawURL),
              let scheme = absolute.scheme?.lowercased(),
              ["http", "https"].contains(scheme) else {
            throw JSBridgeError(code: -1, message: "request:fail invalid url")
        }

        guard let host = absolute.host?.lowercased(), allowedHosts.contains(host) else {
            throw JSBridgeError(code: -1, message: "request:fail host not allowed")
        }

        return absolute
    }

    func shouldAttachAuthHeaders(to url: URL) -> Bool {
        guard let host = url.host?.lowercased() else { return false }
        return allowedHosts.contains(host)
    }
}

struct StorageSetBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "setStorage"
    let box: BridgeStorageBox

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        guard case .string(let key)? = params["key"], let value = params["data"] else {
            throw JSBridgeError(code: -1, message: "setStorage: invalid params")
        }
        await box.set(value, for: key)
        return ["errMsg": .string("setStorage:ok")]
    }
}

struct StorageGetBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "getStorage"
    let box: BridgeStorageBox

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        guard case .string(let key)? = params["key"] else {
            throw JSBridgeError(code: -1, message: "getStorage: invalid params")
        }
        let value = await box.get(for: key) ?? .null
        return [
            "data": value,
            "errMsg": .string("getStorage:ok")
        ]
    }
}

struct StorageRemoveBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "removeStorage"
    let box: BridgeStorageBox

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        guard case .string(let key)? = params["key"] else {
            throw JSBridgeError(code: -1, message: "removeStorage: invalid params")
        }
        await box.remove(for: key)
        return ["errMsg": .string("removeStorage:ok")]
    }
}

struct StorageClearBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "clearStorage"
    let box: BridgeStorageBox

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        await box.clearAll()
        return ["errMsg": .string("clearStorage:ok")]
    }
}

struct MenuButtonRectBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "getMenuButtonBoundingClientRect"
    let rectProvider: @MainActor @Sendable () -> CGRect

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        let rect = await MainActor.run { rectProvider() }
        return [
            "left": .double(rect.origin.x),
            "top": .double(rect.origin.y),
            "right": .double(rect.maxX),
            "bottom": .double(rect.maxY),
            "width": .double(rect.width),
            "height": .double(rect.height)
        ]
    }
}

struct UpdateCheckBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "update.check"

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        let gameID: String?
        if case .string(let value)? = params["appId"] {
            gameID = value
        } else {
            gameID = nil
        }

        guard let snapshot = await GameManager.shared.updateSnapshot(gameID: gameID) else {
            return [
                "hasUpdate": .bool(false),
                "ready": .bool(false),
                "forceUpdate": .bool(false),
                "latestVersion": .string(""),
                "errMsg": .string("update.check:ok")
            ]
        }

        return [
            "hasUpdate": .bool(snapshot.hasUpdate),
            "ready": .bool(snapshot.ready),
            "forceUpdate": .bool(snapshot.force),
            "latestVersion": .string(snapshot.latestVersion ?? ""),
            "errMsg": .string(snapshot.errMsg)
        ]
    }
}

struct UpdateApplyBridgeAPI: JSBridgeAPIHandling {
    let apiName: String = "update.apply"

    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        let gameID: String?
        if case .string(let value)? = params["appId"] {
            gameID = value
        } else {
            gameID = nil
        }

        do {
            try await GameManager.shared.applyPendingUpdate(gameID: gameID)
            return ["errMsg": .string("update.apply:ok")]
        } catch {
            return ["errMsg": .string("update.apply:fail \(error.localizedDescription)")]
        }
    }
}

struct BridgeHTTPResponse: Sendable {
    let statusCode: Int
    let bodyText: String
    let headers: [String: String]
}

actor BridgeRequestClient {
    private let session: URLSession

    init(session: URLSession = BackendPinnedSession.shared) {
        self.session = session
    }

    func send(params: [String: AnySendable]) async throws -> [String: AnySendable] {
        do {
            let environment = try BackendEnvironment.current()
            let accessPolicy = BridgeRequestAccessPolicy(backendBaseURL: environment.apiBaseURL)
            let url = try accessPolicy.resolveURL(from: resolveRawURL(params: params))
            var request = URLRequest(url: url)
            request.httpMethod = resolveMethod(params: params)
            request.timeoutInterval = resolveTimeout(params: params)

            var headers = resolveHeaders(params: params)
            if accessPolicy.shouldAttachAuthHeaders(to: url) {
                headers.merge(await resolveAuthHeaders(url: url), uniquingKeysWith: { existing, _ in existing })
            }
            for (k, v) in headers {
                request.setValue(v, forHTTPHeaderField: k)
            }

            if let body = params["data"] {
                if request.value(forHTTPHeaderField: "Content-Type") == nil {
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                }
                request.httpBody = try JSONSerialization.data(withJSONObject: body.toFoundationObject())
            }

            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                throw JSBridgeError(code: -1, message: "request:fail invalid response")
            }
            let text = String(data: data, encoding: .utf8) ?? ""
            var responseHeaders: [String: String] = [:]
            http.allHeaderFields.forEach { key, value in
                responseHeaders[String(describing: key)] = String(describing: value)
            }

            return [
                "statusCode": .int(http.statusCode),
                "data": .string(text),
                "header": .object(responseHeaders.reduce(into: [String: AnySendable](), { $0[$1.key] = .string($1.value) })),
                "errMsg": .string("request:ok")
            ]
        } catch {
            return [
                "statusCode": .int(-1),
                "data": .string(""),
                "errMsg": .string("request:fail \(error.localizedDescription)")
            ]
        }
    }

    private func resolveRawURL(params: [String: AnySendable]) throws -> String {
        guard case .string(let rawURL)? = params["url"], rawURL.isEmpty == false else {
            throw JSBridgeError(code: -1, message: "request:fail url required")
        }
        return rawURL
    }

    private func resolveMethod(params: [String: AnySendable]) -> String {
        if case .string(let method)? = params["method"], method.isEmpty == false {
            return method.uppercased()
        }
        return "GET"
    }

    private func resolveTimeout(params: [String: AnySendable]) -> TimeInterval {
        if case .double(let timeout)? = params["timeout"], timeout > 0 {
            return timeout / 1000
        }
        if case .int(let timeout)? = params["timeout"], timeout > 0 {
            return Double(timeout) / 1000
        }
        return 30
    }

    private func resolveHeaders(params: [String: AnySendable]) -> [String: String] {
        guard case .object(let headerObject)? = params["header"] else {
            return [:]
        }
        return headerObject.reduce(into: [String: String]()) { partialResult, entry in
            if case .string(let value) = entry.value {
                partialResult[entry.key] = value
            }
        }
    }

    private func resolveAuthHeaders(url: URL) async -> [String: String] {
        var output: [String: String] = [:]

        if let session = await AuthSessionStore.shared.current(),
           session.accessToken.isEmpty == false {
            output["Authorization"] = "Bearer \(session.accessToken)"
        }

        if let cookies = HTTPCookieStorage.shared.cookies(for: url), cookies.isEmpty == false {
            let cookieValue = cookies.map { "\($0.name)=\($0.value)" }.joined(separator: "; ")
            if cookieValue.isEmpty == false {
                output["Cookie"] = cookieValue
            }
        }

        return output
    }
}
