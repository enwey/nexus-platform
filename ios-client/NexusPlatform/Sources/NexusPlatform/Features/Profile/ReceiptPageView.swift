import SwiftUI
import WebKit

struct ReceiptPageView: View {
    let url: URL

    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var reloadToken = UUID()
    @State private var request: URLRequest?

    var body: some View {
        ZStack {
            if let request {
                ReceiptWebView(
                    request: request,
                    isLoading: $isLoading,
                    errorMessage: $errorMessage
                )
                .id(reloadToken)
                .opacity(isLoading || errorMessage != nil ? 0.001 : 1)
                .animation(NativeMotion.overlayTransition, value: isLoading)
                .animation(NativeMotion.overlayTransition, value: errorMessage != nil)
            }

            if isLoading {
                receiptSkeleton
                    .padding(.horizontal, 24)
                    .transition(NativeMotion.stateSwapTransition)
            }

            if let errorMessage {
                NativeStateCard {
                    Text(errorMessage)
                        .font(.footnote)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    Button(copy.retry) {
                        self.errorMessage = nil
                        reloadToken = UUID()
                    }
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(AppTheme.ColorToken.auroraBlue)
                }
                .padding(.horizontal, 24)
                .transition(NativeMotion.stateSwapTransition)
            }
        }
        .nexusPageBackground()
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .animation(NativeMotion.overlayTransition, value: isLoading)
        .animation(NativeMotion.overlayTransition, value: errorMessage != nil)
        .task {
            await loadAuthorizedRequest()
        }
        .onChange(of: reloadToken) {
            Task {
                await loadAuthorizedRequest()
            }
        }
    }

    private var receiptSkeleton: some View {
        VStack(spacing: 16) {
            NativeSkeletonBlock(height: 18, cornerRadius: 9)
            NativeSkeletonBlock(height: 18, cornerRadius: 9)
            NativeSkeletonBlock(width: 228, height: 18, cornerRadius: 9)
            NativeSkeletonBlock(width: 168, height: 16, cornerRadius: 8)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 18)
        .frame(maxWidth: .infinity)
        .background(Color.white.opacity(0.04), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }

    private var copy: ReceiptCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    @MainActor
    private func loadAuthorizedRequest() async {
        request = await authorizedRequest(url: url)
    }

    private func authorizedRequest(url: URL) async -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        if let session = await AuthSessionStore.shared.current(),
           session.accessToken.isEmpty == false {
            request.setValue("Bearer \(session.accessToken)", forHTTPHeaderField: "Authorization")
        }

        return request
    }
}

private struct ReceiptCopy {
    let title: String
    let loading: String
    let retry: String

    static func forLanguage(_ language: AppLanguage) -> ReceiptCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "回执", loading: "加载回执中...", retry: "重试")
        case .traditionalChinese:
            return .init(title: "回執", loading: "正在載入回執...", retry: "重試")
        case .english:
            return .init(title: "Receipt", loading: "Loading receipt...", retry: "Retry")
        }
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
