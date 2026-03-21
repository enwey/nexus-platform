import Foundation
import WebKit

class NexusBridge: NSObject, WKScriptMessageHandler {
    static let shared = NexusBridge()
    private var webView: WKWebView?
    private var apiHandlers: [String: ApiHandler] = [:]
    
    private override init() {
        super.init()
        registerApiHandlers()
    }
    
    func setup(webView: WKWebView) {
        self.webView = webView
        let contentController = webView.configuration.userContentController
        contentController.add(self, name: "NexusBridge")
    }
    
    private func registerApiHandlers() {
        apiHandlers["wx.login"] = LoginApi()
        apiHandlers["wx.request"] = RequestApi()
        apiHandlers["wx.getSystemInfoSync"] = SystemInfoApi()
        apiHandlers["wx.setStorage"] = StorageApi()
        apiHandlers["wx.getStorage"] = StorageApi()
        apiHandlers["wx.removeStorage"] = StorageApi()
        apiHandlers["wx.clearStorage"] = StorageApi()
        apiHandlers["wx.getUserInfo"] = UserInfoApi()
        apiHandlers["wx.shareAppMessage"] = ShareApi()
        apiHandlers["wx.showToast"] = ToastApi()
        apiHandlers["wx.showModal"] = ModalApi()
        apiHandlers["wx.downloadFile"] = FileApi()
        apiHandlers["wx.uploadFile"] = FileApi()
        apiHandlers["wx.getNetworkType"] = NetworkApi()
        apiHandlers["wx.chooseImage"] = ImageApi()
        apiHandlers["wx.setClipboardData"] = ClipboardApi()
        apiHandlers["wx.getClipboardData"] = ClipboardApi()
        apiHandlers["wx.vibrateShort"] = VibrateApi()
        apiHandlers["wx.vibrateLong"] = VibrateApi()
    }
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any],
              let api = body["api"] as? String,
              let callbackId = body["callbackId"] as? String,
              let params = body["params"] as? [String: Any] else {
            return
        }
        
        Task {
            do {
                if let handler = apiHandlers[api] {
                    let result = try await handler.handle(api: api, params: params)
                    sendCallback(callbackId: callbackId, data: result, error: nil)
                } else {
                    sendCallback(callbackId: callbackId, data: nil, error: "API not implemented: \(api)")
                }
            } catch {
                sendCallback(callbackId: callbackId, data: nil, error: error.localizedDescription)
            }
        }
    }
    
    private func sendCallback(callbackId: String, data: Any?, error: String?) {
        var response: [String: Any] = ["callbackId": callbackId]
        
        if let data = data {
            response["data"] = data
        }
        
        if let error = error {
            response["error"] = [
                "errMsg": error,
                "code": -1
            ]
        }
        
        guard let jsonData = try? JSONSerialization.data(withJSONObject: response),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            return
        }
        
        let script = "window.NexusBridgeCallback(\(jsonString));"
        webView?.evaluateJavaScript(script, completionHandler: nil)
    }
}
