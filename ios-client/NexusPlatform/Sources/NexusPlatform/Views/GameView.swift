import SwiftUI
import WebKit
import UIKit

struct GameView: View {
    let game: Game

    @Environment(\.presentationMode) private var presentationMode
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
    private var copy: GameRuntimeCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

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
                            Text(copy.loginRequired)
                                .font(.headline)
                            Button(copy.goLogin) {
                                showAuthFlow = true
                            }
                            .nexusPrimaryCTA()
                        }
                        .padding(20)
                        .nexusGlassCard()
                    } else {
                        ProgressView(copy.loading)
                            .scaleEffect(1.2)
                            .padding(20)
                            .nexusGlassCard()
                    }
                }

                if forceUpdating {
                    VStack(spacing: 14) {
                        Text(copy.updating)
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
                    onExit: { presentationMode.wrappedValue.dismiss() },
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
            .alert(copy.launchFailed, isPresented: Binding(get: { errorMessage != nil }, set: { _ in errorMessage = nil })) {
                Button(copy.confirm, role: .cancel) {}
            } message: {
                Text(errorMessage ?? copy.unknownError)
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
            .onChange(of: proxy.size) {
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
            Task {
                await GameEngagementStore.shared.markPlayed(gameID: game.id)
                await libraryService.markPlayed(appID: game.id)
            }
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
        showTip(copy.linkCopied)
        Task { try? await libraryService.markShared(appID: game.id) }
    }

    private func shareToChannel(_ channel: String) {
        UIPasteboard.general.string = "nexus://\(game.id)/index.html"
        let channelName = copy.channelName(channel)
        showTip(String(format: copy.shareCopiedFormat, channelName))
        Task { try? await libraryService.markShared(appID: game.id) }
    }

    private func toggleFavorite() {
        Task {
            let updated = await GameEngagementStore.shared.toggleFavorite(gameID: game.id)
            await MainActor.run {
                isFavorite = updated
                showTip(updated ? copy.addedFavorite : copy.removedFavorite)
            }
            try? await libraryService.setFavorite(appID: game.id, favorite: updated)
        }
    }

    private func sendFeedback() {
        UIPasteboard.general.string = "Game feedback: \(game.name) - \(game.id)"
        showTip(copy.feedbackCopied)
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

private struct GameRuntimeCopy {
    let loginRequired: String
    let goLogin: String
    let loading: String
    let updating: String
    let launchFailed: String
    let confirm: String
    let unknownError: String
    let linkCopied: String
    let shareCopiedFormat: String
    let addedFavorite: String
    let removedFavorite: String
    let feedbackCopied: String
    let whatsapp: String
    let facebook: String
    let more: String

    func channelName(_ raw: String) -> String {
        switch raw.lowercased() {
        case "whatsapp":
            return whatsapp
        case "facebook":
            return facebook
        default:
            return more
        }
    }

    static func forLanguage(_ language: AppLanguage) -> GameRuntimeCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                loginRequired: "需要登录才能启动游戏",
                goLogin: "去登录",
                loading: "加载中...",
                updating: "正在更新游戏资源",
                launchFailed: "启动失败",
                confirm: "确定",
                unknownError: "未知错误",
                linkCopied: "分享链接已复制",
                shareCopiedFormat: "%@ 分享文案已复制",
                addedFavorite: "已加入收藏",
                removedFavorite: "已移出收藏",
                feedbackCopied: "反馈信息已复制",
                whatsapp: "WhatsApp",
                facebook: "Facebook",
                more: "更多"
            )
        case .traditionalChinese:
            return .init(
                loginRequired: "需要登入才能啟動遊戲",
                goLogin: "去登入",
                loading: "載入中...",
                updating: "正在更新遊戲資源",
                launchFailed: "啟動失敗",
                confirm: "確定",
                unknownError: "未知錯誤",
                linkCopied: "分享連結已複製",
                shareCopiedFormat: "%@ 分享文案已複製",
                addedFavorite: "已加入收藏",
                removedFavorite: "已移出收藏",
                feedbackCopied: "回饋資訊已複製",
                whatsapp: "WhatsApp",
                facebook: "Facebook",
                more: "更多"
            )
        case .english:
            return .init(
                loginRequired: "Sign in to launch this game",
                goLogin: "Sign In",
                loading: "Loading...",
                updating: "Updating game resources",
                launchFailed: "Launch Failed",
                confirm: "OK",
                unknownError: "Unknown error",
                linkCopied: "Share link copied",
                shareCopiedFormat: "%@ share text copied",
                addedFavorite: "Added to favorites",
                removedFavorite: "Removed from favorites",
                feedbackCopied: "Feedback info copied",
                whatsapp: "WhatsApp",
                facebook: "Facebook",
                more: "More"
            )
        }
    }
}
