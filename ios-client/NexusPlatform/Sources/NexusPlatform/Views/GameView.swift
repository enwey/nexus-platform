import SwiftUI
import WebKit
import UIKit

struct GameView: View {
    let game: Game

    @Environment(\.dismiss) private var dismiss
    @State private var isLoading = true
    @State private var gameReady = false
    @State private var errorMessage: String?
    @State private var reloadToken = UUID()
    @State private var currentWebView: WKWebView?
    @State private var forceUpdating = false
    @State private var forceUpdateProgress: Double = 0

    private let bridge = JSBridge()
    private let schemeHandler = NexusSchemeHandler()

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .topTrailing) {
                if gameReady {
                    GameWebView(
                        initialURL: gameEntryURL(),
                        bridge: bridge,
                        schemeHandler: schemeHandler,
                        sdkScript: GameManager.shared.getSDKContent(),
                        isLoading: $isLoading,
                        onWebViewReady: { webView in
                            currentWebView = webView
                        }
                    )
                    .id(reloadToken)
                    .ignoresSafeArea()
                }

                if isLoading || (gameReady == false) {
                    ProgressView("加载中...")
                        .scaleEffect(1.2)
                        .padding(20)
                        .nexusGlassCard()
                }

                if forceUpdating {
                    VStack(spacing: 14) {
                        Text("正在更新游戏资源")
                            .font(.headline)
                        ProgressView(value: forceUpdateProgress, total: 1.0)
                            .progressViewStyle(.linear)
                            .frame(width: 220)
                        Text("\(Int(forceUpdateProgress * 100))%")
                            .font(.caption)
                            .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    }
                    .padding(22)
                    .nexusGlassCard()
                }

                CapsuleMenuOverlay(
                    onExit: { dismiss() },
                    onRestart: { reloadToken = UUID() },
                    onShare: { shareGameLink() }
                )
                .padding(.top, proxy.safeAreaInsets.top + 8)
                .padding(.trailing, 12)
            }
            .nexusPageBackground()
            .alert("启动失败", isPresented: Binding(get: { errorMessage != nil }, set: { _ in errorMessage = nil })) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(errorMessage ?? "未知错误")
            }
            .task {
                await bootGameIfNeeded()
                await MainActor.run {
                    bridge.updateMenuRectProvider {
                        menuButtonRect(in: proxy)
                    }
                }
            }
            .onChange(of: proxy.size) { _ in
                Task { @MainActor in
                    bridge.updateMenuRectProvider {
                        menuButtonRect(in: proxy)
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .statusBar(hidden: true)
    }

    private func gameEntryURL() -> URL {
        URL(string: "nexus://\(game.id)/index.html") ?? URL(string: "about:blank") ?? URL(fileURLWithPath: "/")
    }

    private func bootGameIfNeeded() async {
        do {
            let summary = try await GameManager.shared.prepareLaunch(game: game) { progress in
                Task { @MainActor in
                    forceUpdating = true
                    forceUpdateProgress = min(max(progress.fractionCompleted, 0), 1)
                }
            }
            await MainActor.run {
                gameReady = true
                if summary.forceUpdated {
                    forceUpdateProgress = 1
                }
                forceUpdating = false
            }
            Task { await GameEngagementStore.shared.markPlayed(gameID: game.id) }
        } catch {
            await MainActor.run {
                if let launchError = error as? GameLaunchError {
                    errorMessage = launchError.localizedDescription
                } else {
                    errorMessage = error.localizedDescription
                }
                forceUpdating = false
            }
        }
    }

    private func shareGameLink() {
        UIPasteboard.general.string = "nexus://\(game.id)/index.html"
    }

    private func menuButtonRect(in proxy: GeometryProxy) -> CGRect {
        let width: CGFloat = 92
        let height: CGFloat = 36
        let x = proxy.size.width - 12 - width
        let y = proxy.safeAreaInsets.top + 8
        return CGRect(x: x, y: y, width: width, height: height)
    }
}
