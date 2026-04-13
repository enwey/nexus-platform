import Foundation
import WebKit
import CoreGraphics
import CoreFoundation

final class JSBridge: NSObject, JSBridgeProtocol {
    private weak var webView: WKWebView?
    private var handlers: [String: any JSBridgeAPIHandling] = [:]
    private let storageBox = BridgeStorageBox()

    private let messageHandlerName = "NexusBridge"

    override init() {
        super.init()
        registerDefaultHandlers(rectProvider: { .zero })
    }

    @MainActor
    func attach(to webView: WKWebView) {
        self.webView = webView
        webView.configuration.userContentController.add(self, name: messageHandlerName)
    }

    @MainActor
    func detach() {
        webView?.configuration.userContentController.removeScriptMessageHandler(forName: messageHandlerName)
        webView = nil
    }

    @MainActor
    func register(handler: any JSBridgeAPIHandling) {
        handlers[handler.apiName] = handler
        handlers["wx.\(handler.apiName)"] = handler
    }

    @MainActor
    func unregister(apiName: String) {
        handlers.removeValue(forKey: apiName)
    }

    @MainActor
    func handle(messageBody: Any) async {
        do {
            let request = try parseRequest(from: messageBody)
            do {
                guard let handler = handlers[request.api] else {
                    await callback(.failure(callbackID: request.callbackID, message: "API not implemented: \(request.api)"))
                    return
                }

                let data = try await handler.handle(params: request.params)
                await callback(.success(callbackID: request.callbackID, data: data))
            } catch let bridgeError as JSBridgeError {
                await callback(.failure(callbackID: request.callbackID, code: bridgeError.code, message: bridgeError.message))
            } catch {
                await callback(.failure(callbackID: request.callbackID, message: error.localizedDescription))
            }
        } catch let bridgeError as JSBridgeError {
            await callback(.failure(callbackID: "", code: bridgeError.code, message: bridgeError.message))
        } catch {
            await callback(.failure(callbackID: "", message: error.localizedDescription))
        }
    }

    @MainActor
    func callback(_ response: JSBridgeResponse) async {
        guard let webView else {
            return
        }

        var payload: [String: Any] = ["callbackId": response.callbackID]
        if let data = response.data {
            payload["data"] = data.toFoundationDictionary()
        }
        if let error = response.error {
            payload["error"] = [
                "code": error.code,
                "errMsg": error.message
            ]
        }

        guard let raw = try? JSONSerialization.data(withJSONObject: payload),
              let jsonString = String(data: raw, encoding: .utf8) else {
            return
        }

        let script = "window.NexusBridgeCallback && window.NexusBridgeCallback(\(jsonString));"
        webView.evaluateJavaScript(script, completionHandler: nil)
    }

    @MainActor
    func registerDefaultHandlers(rectProvider: @escaping @MainActor () -> CGRect) {
        register(handler: LoginBridgeAPI())
        register(handler: RequestBridgeAPI())
        register(handler: StorageSetBridgeAPI(box: storageBox))
        register(handler: StorageGetBridgeAPI(box: storageBox))
        register(handler: StorageRemoveBridgeAPI(box: storageBox))
        register(handler: StorageClearBridgeAPI(box: storageBox))
        register(handler: UpdateCheckBridgeAPI())
        register(handler: UpdateApplyBridgeAPI())
        register(handler: MenuButtonRectBridgeAPI(rectProvider: rectProvider))

        // Legacy wx-mock-sdk aliases.
        handlers["setStorageSync"] = handlers["setStorage"]
        handlers["wx.setStorageSync"] = handlers["setStorage"]
        handlers["getStorageSync"] = handlers["getStorage"]
        handlers["wx.getStorageSync"] = handlers["getStorage"]
        handlers["removeStorageSync"] = handlers["removeStorage"]
        handlers["wx.removeStorageSync"] = handlers["removeStorage"]
        handlers["clearStorageSync"] = handlers["clearStorage"]
        handlers["wx.clearStorageSync"] = handlers["clearStorage"]
    }

    @MainActor
    func updateMenuRectProvider(_ provider: @escaping @MainActor () -> CGRect) {
        register(handler: MenuButtonRectBridgeAPI(rectProvider: provider))
    }

    private func parseRequest(from body: Any) throws -> JSBridgeRequest {
        guard let dict = body as? [String: Any],
              let api = dict["api"] as? String,
              let callbackID = dict["callbackId"] as? String else {
            throw JSBridgeError(code: -1, message: "invalid bridge payload")
        }

        let paramsObject = (dict["params"] as? [String: Any]) ?? [:]
        let params = paramsObject.reduce(into: [String: AnySendable]()) { partialResult, entry in
            partialResult[entry.key] = AnySendable.fromFoundation(entry.value)
        }

        return JSBridgeRequest(api: api, callbackID: callbackID, params: params)
    }
}

extension JSBridge: WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == messageHandlerName else {
            return
        }

        Task { @MainActor in
            await handle(messageBody: message.body)
        }
    }
}

private extension Dictionary where Key == String, Value == AnySendable {
    func toFoundationDictionary() -> [String: Any] {
        reduce(into: [String: Any]()) { partialResult, entry in
            partialResult[entry.key] = entry.value.toFoundationObject()
        }
    }
}
