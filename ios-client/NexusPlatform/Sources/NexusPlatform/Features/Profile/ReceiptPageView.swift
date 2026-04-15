import SwiftUI
import WebKit

struct ReceiptPageView: View {
    let url: URL

    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var reloadToken = UUID()

    var body: some View {
        ZStack {
            ReceiptWebView(
                request: authorizedRequest(url: url),
                isLoading: $isLoading,
                errorMessage: $errorMessage
            )
            .id(reloadToken)

            if isLoading {
                ProgressView("加载回执中...")
                    .padding(16)
                    .nexusGlassCard()
            }

            if let errorMessage {
                VStack(spacing: 10) {
                    Text(errorMessage)
                        .font(.footnote)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    Button("重试") {
                        self.errorMessage = nil
                        reloadToken = UUID()
                    }
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(AppTheme.ColorToken.auroraBlue)
                }
                .padding(16)
                .nexusGlassCard()
            }
        }
        .nexusPageBackground()
        .navigationTitle("回执")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func authorizedRequest(url: URL) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        let defaults = UserDefaults.standard
        let keys = ["authorization", "access_token", "token", "auth_token"]
        for key in keys {
            guard let raw = defaults.string(forKey: key), raw.isEmpty == false else { continue }
            let hasBearer = raw.lowercased().hasPrefix("bearer ")
            request.setValue(hasBearer ? raw : "Bearer \(raw)", forHTTPHeaderField: "Authorization")
            break
        }
        return request
    }
}

private struct ReceiptWebView: UIViewRepresentable {
    let request: URLRequest
    @Binding var isLoading: Bool
    @Binding var errorMessage: String?

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.scrollView.bounces = false
        webView.load(request)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        private let parent: ReceiptWebView

        init(_ parent: ReceiptWebView) {
            self.parent = parent
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            parent.isLoading = true
            parent.errorMessage = nil
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
            parent.errorMessage = error.localizedDescription
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
            parent.errorMessage = error.localizedDescription
        }
    }
}
