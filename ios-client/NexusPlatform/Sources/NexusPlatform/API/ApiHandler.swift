import Foundation

protocol ApiHandler {
    func handle(api: String, params: [String: Any]) async throws -> Any
}
