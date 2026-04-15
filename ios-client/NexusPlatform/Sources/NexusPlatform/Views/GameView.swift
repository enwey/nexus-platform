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
    @State private var isAuthenticated = false
    @State private var hasCheckedAuth = false
    @State private var hasBooted = false
    @State private var showAuthFlow = false
    @State private var isFavorite = false
    @State private var actionTip: String?

    private let bridge = JSBridge()
    private let schemeHandler = NexusSchemeHandler()
    private let libraryService: LibraryHomeServiceProtocol = LibraryHomeService()

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
                    if hasCheckedAuth && isAuthenticated == false {
                        VStack(spacing: 10) {
                            Text("需要登录才能启动游戏")
                                .font(.headline)
                            Button("去登录") {
                                showAuthFlow = true
                            }
                            .nexusPrimaryCTA()
                        }
                        .padding(20)
                        .nexusGlassCard()
                    } else {
                        ProgressView("加载中...")
                            .scaleEffect(1.2)
                            .padding(20)
                            .nexusGlassCard()
                    }
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
                    isFavorite: isFavorite,
                    onToggleFavorite: { toggleFavorite() },
                    onExit: { dismiss() },
                    onRestart: { reloadToken = UUID() },
                    onCopyLink: { copyShareLink() },
                    onShareWhatsApp: { shareToChannel("whatsapp") },
                    onShareFacebook: { shareToChannel("facebook") },
                    onFeedback: { sendFeedback() }
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
            .overlay(alignment: .bottom) {
                if let actionTip {
                    Text(actionTip)
                        .font(.footnote)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial, in: Capsule())
                        .padding(.bottom, 24)
                }
            }
            .task {
                isFavorite = await GameEngagementStore.shared.isFavorite(gameID: game.id)
                await ensureAuthenticatedAndBoot()
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
        .sheet(isPresented: $showAuthFlow) {
            NavigationStack {
                AuthFlowView { session in
                    Task {
                        await AuthSessionStore.shared.save(session)
                        await MainActor.run {
                            isAuthenticated = true
                            showAuthFlow = false
                        }
                        await ensureAuthenticatedAndBoot()
                    }
                }
            }
        }
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

    private func ensureAuthenticatedAndBoot() async {
        if hasCheckedAuth == false {
            let session = await AuthSessionStore.shared.current()
            await MainActor.run {
                hasCheckedAuth = true
                isAuthenticated = session != nil
                if isAuthenticated == false {
                    showAuthFlow = true
                }
            }
        }

        guard isAuthenticated else { return }
        guard hasBooted == false else { return }
        hasBooted = true
        await bootGameIfNeeded()
    }

    private func copyShareLink() {
        UIPasteboard.general.string = "nexus://\(game.id)/index.html"
        showTip("分享链接已复制")
        Task { try? await libraryService.markShared(appID: game.id) }
    }

    private func shareToChannel(_ channel: String) {
        UIPasteboard.general.string = "nexus://\(game.id)/index.html"
        showTip("\(channel.capitalized) 分享文案已复制")
        Task { try? await libraryService.markShared(appID: game.id) }
    }

    private func toggleFavorite() {
        Task {
            let updated = await GameEngagementStore.shared.toggleFavorite(gameID: game.id)
            await MainActor.run {
                isFavorite = updated
                showTip(updated ? "已加入收藏" : "已移出收藏")
            }
            try? await libraryService.setFavorite(appID: game.id, favorite: updated)
        }
    }

    private func sendFeedback() {
        UIPasteboard.general.string = "Game feedback: \(game.name) - \(game.id)"
        showTip("反馈信息已复制")
    }

    @MainActor
    private func showTip(_ text: String) {
        actionTip = text
        Task {
            try? await Task.sleep(nanoseconds: 1_600_000_000)
            await MainActor.run {
                if actionTip == text {
                    actionTip = nil
                }
            }
        }
    }

    private func menuButtonRect(in proxy: GeometryProxy) -> CGRect {
        let width: CGFloat = 92
        let height: CGFloat = 36
        let x = proxy.size.width - 12 - width
        let y = proxy.safeAreaInsets.top + 8
        return CGRect(x: x, y: y, width: width, height: height)
    }
}
