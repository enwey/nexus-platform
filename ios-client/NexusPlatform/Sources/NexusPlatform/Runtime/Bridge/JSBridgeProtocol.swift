import Foundation
import WebKit
import CoreFoundation

/// Single JS call parsed from `window.webkit.messageHandlers` payload.
struct JSBridgeRequest: Sendable {
    let api: String
    let callbackID: String
    let params: [String: AnySendable]
}

/// Normalized callback payload sent back to H5 runtime.
struct JSBridgeResponse: Sendable {
    let callbackID: String
    let data: [String: AnySendable]?
    let error: JSBridgeError?
}

struct JSBridgeError: Error, Sendable {
    let code: Int
    let message: String
}

/// Type-safe container to make heterogeneous JSON values Sendable.
enum AnySendable: Sendable {
    case string(String)
    case int(Int)
    case double(Double)
    case bool(Bool)
    case object([String: AnySendable])
    case array([AnySendable])
    case null
}

extension AnySendable {
    static func fromFoundation(_ any: Any) -> AnySendable {
        switch any {
        case let value as String:
            return .string(value)
        case let value as Int:
            return .int(value)
        case let value as Double:
            return .double(value)
        case let value as NSNumber:
            if CFGetTypeID(value) == CFBooleanGetTypeID() {
                return .bool(value.boolValue)
            }
            return .double(value.doubleValue)
        case let value as [String: Any]:
            let mapped = value.reduce(into: [String: AnySendable]()) { partialResult, entry in
                partialResult[entry.key] = .fromFoundation(entry.value)
            }
            return .object(mapped)
        case let value as [Any]:
            return .array(value.map { .fromFoundation($0) })
        default:
            return .null
        }
    }

    func toFoundationObject() -> Any {
        switch self {
        case .string(let value):
            return value
        case .int(let value):
            return value
        case .double(let value):
            return value
        case .bool(let value):
            return value
        case .object(let dict):
            return dict.reduce(into: [String: Any]()) { partialResult, entry in
                partialResult[entry.key] = entry.value.toFoundationObject()
            }
        case .array(let array):
            return array.map { $0.toFoundationObject() }
        case .null:
            return NSNull()
        }
    }
}

/// Concrete bridge API handler (e.g. login/request/setStorage).
protocol JSBridgeAPIHandling: Sendable {
    var apiName: String { get }
    func handle(params: [String: AnySendable]) async throws -> [String: AnySendable]
}

/// Core bridge abstraction to keep WKWebView plumbing testable.
@MainActor
protocol JSBridgeProtocol: AnyObject {
    func attach(to webView: WKWebView)
    func detach()

    func register(handler: any JSBridgeAPIHandling)
    func unregister(apiName: String)

    func handle(messageBody: Any) async
    func callback(_ response: JSBridgeResponse) async
}

extension JSBridgeResponse {
    static func success(callbackID: String, data: [String: AnySendable]?) -> JSBridgeResponse {
        JSBridgeResponse(callbackID: callbackID, data: data, error: nil)
    }

    static func failure(callbackID: String, code: Int = -1, message: String) -> JSBridgeResponse {
        JSBridgeResponse(callbackID: callbackID, data: nil, error: JSBridgeError(code: code, message: message))
    }
}
