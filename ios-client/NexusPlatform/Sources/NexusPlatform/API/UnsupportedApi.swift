import Foundation

final class UnsupportedApi: ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any {
        return [
            "errMsg": "\(api):fail not supported on ios",
            "code": "NOT_SUPPORTED"
        ]
    }
}
