import Foundation

struct DeviceSession: Identifiable, Sendable {
    let id: String
    let deviceName: String
    let model: String
    let ip: String
    let lastActiveAt: String
    let current: Bool
}

enum DeviceSessionServiceError: LocalizedError {
    case unauthorized
    case invalidResponse
    case failed(String)

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return "请先登录"
        case .invalidResponse:
            return "设备数据响应无效"
        case .failed(let message):
            return message.isEmpty ? "设备操作失败" : message
        }
    }
}

protocol DeviceSessionServiceProtocol: Sendable {
    func fetchDevices() async throws -> [DeviceSession]
    func kick(deviceID: String) async throws
    func logoutAll() async throws
}

struct DeviceSessionService: DeviceSessionServiceProtocol {
    private let session: URLSession
    private let baseURL: URL
    private var client: BackendAPIClient { .init(session: session, baseURL: baseURL) }

    init(session: URLSession = BackendPinnedSession.shared, env: BackendEnvironment = .current()) {
        self.session = session
        self.baseURL = env.apiBaseURL
    }

    func fetchDevices() async throws -> [DeviceSession] {
        let data = try await request(path: "user/devices", method: "GET", body: nil)
        guard let list = data as? [[String: Any]] else {
            return []
        }
        return list.compactMap { node in
            let id = node["deviceId"] as? String ?? ""
            guard id.isEmpty == false else { return nil }
            return DeviceSession(
                id: id,
                deviceName: node["deviceName"] as? String ?? "",
                model: node["model"] as? String ?? "",
                ip: node["ip"] as? String ?? "",
                lastActiveAt: node["lastActiveAt"] as? String ?? "",
                current: node["current"] as? Bool ?? false
            )
        }
    }

    func kick(deviceID: String) async throws {
        _ = try await request(path: "user/devices/\(deviceID)/kick", method: "POST", body: [:])
    }

    func logoutAll() async throws {
        _ = try await request(path: "user/logout-all", method: "POST", body: [:])
    }

    private func request(path: String, method: String, body: [String: Any]?) async throws -> Any {
        return try await client.request(path: path, method: method, body: body, authMode: .required)
    }
}
