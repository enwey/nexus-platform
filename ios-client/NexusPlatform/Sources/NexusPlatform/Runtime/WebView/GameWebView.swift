import SwiftUI
import WebKit

struct GameWebView: UIViewRepresentable {
    let initialURL: URL
    let bridge: JSBridge
    let schemeHandler: WKURLSchemeHandler
    let sdkScript: String
    let layoutMetrics: GameLayoutMetrics
    let language: AppLanguage
    @Binding var isLoading: Bool
    @Binding var isContentVisible: Bool
    @Binding var errorMessage: String?
    var onWebViewReady: ((WKWebView) -> Void)?

    func makeUIView(context: Context) -> WKWebView {
        let builder = WebViewConfigBuilder(
            schemeHandler: schemeHandler,
            injectedSDKScript: sdkScript,
            layoutMetrics: layoutMetrics,
            language: language
        )
        let configuration = builder.build()

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.bounces = false
        webView.scrollView.alwaysBounceVertical = false
        webView.scrollView.alwaysBounceHorizontal = false
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.scrollView.contentInset = .zero
        webView.scrollView.scrollIndicatorInsets = .zero
        webView.scrollView.automaticallyAdjustsScrollIndicatorInsets = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        webView.navigationDelegate = context.coordinator

        Task { @MainActor in
            bridge.attach(to: webView)
        }

        webView.load(URLRequest(url: initialURL))
        onWebViewReady?(webView)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        let builder = WebViewConfigBuilder(
            schemeHandler: schemeHandler,
            injectedSDKScript: sdkScript,
            layoutMetrics: layoutMetrics,
            language: language
        )

        if context.coordinator.lastLayoutMetrics != layoutMetrics {
            context.coordinator.lastLayoutMetrics = layoutMetrics
            uiView.evaluateJavaScript(builder.layoutUpdateScript(), completionHandler: nil)
        }

        if context.coordinator.lastLanguage != language {
            context.coordinator.lastLanguage = language
            uiView.evaluateJavaScript(builder.languageUpdateScript(), completionHandler: nil)
        }
    }

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
        fileprivate var lastLayoutMetrics: GameLayoutMetrics
        fileprivate var lastLanguage: AppLanguage

        init(_ parent: GameWebView) {
            self.parent = parent
            self.lastLayoutMetrics = parent.layoutMetrics
            self.lastLanguage = parent.language
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            parent.isLoading = true
            parent.isContentVisible = false
            parent.errorMessage = nil
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
            parent.isContentVisible = true
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
            parent.isContentVisible = false
            parent.errorMessage = parent.userVisibleMessage(for: error)
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
            parent.isContentVisible = false
            parent.errorMessage = parent.userVisibleMessage(for: error)
        }

        func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
            parent.isContentVisible = false
        }
    }
}

private extension GameWebView {
    func userVisibleMessage(for error: Error) -> String {
        let message = error.localizedDescription.lowercased()
        if message.contains("offline") || message.contains("internet connection") {
            return AppText.gamePageLoadFailed()
        }
        return AppText.gamePageLoadFailed()
    }
}
