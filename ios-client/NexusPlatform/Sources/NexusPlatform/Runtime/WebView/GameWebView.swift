import SwiftUI
import WebKit

struct GameWebView: UIViewRepresentable {
    let initialURL: URL
    let bridge: JSBridge
    let schemeHandler: WKURLSchemeHandler
    let sdkScript: String
    @Binding var isLoading: Bool
    var onWebViewReady: ((WKWebView) -> Void)?

    func makeUIView(context: Context) -> WKWebView {
        let builder = WebViewConfigBuilder(
            schemeHandler: schemeHandler,
            injectedSDKScript: sdkScript
        )
        let configuration = builder.build()

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.bounces = false
        webView.scrollView.alwaysBounceVertical = false
        webView.scrollView.alwaysBounceHorizontal = false
        webView.navigationDelegate = context.coordinator

        Task { @MainActor in
            bridge.attach(to: webView)
        }

        webView.load(URLRequest(url: initialURL))
        onWebViewReady?(webView)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        Task { @MainActor in
            coordinator.parent.bridge.detach()
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        fileprivate let parent: GameWebView

        init(_ parent: GameWebView) {
            self.parent = parent
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            parent.isLoading = true
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
        }
    }
}
