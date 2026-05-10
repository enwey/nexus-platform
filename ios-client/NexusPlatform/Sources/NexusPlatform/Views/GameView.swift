import SwiftUI
import WebKit
import UIKit

struct GameView: View {
    let game: Game

    @Environment(\.dismiss) private var dismiss
    @StateObject private var runtimeSession = GameRuntimeSession()
    @State private var isLoading = true
    @State private var isContentVisible = false
    @State private var showLoadingOverlay = true
    @State private var loadingOverlayOpacity = 1.0
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
    @State private var showRuntimeMenu = false
    @State private var runtimeMenuPresented = false
    @State private var runtimeMenuDragOffset: CGFloat = 0
    @State private var runtimeProfile: RuntimeProfile?
    @State private var language: AppLanguage = AppLanguageStore.currentSync()

    private let runtimeMenuOpenAnimation = Animation.interpolatingSpring(duration: 0.46, bounce: 0.08)
    private let runtimeMenuCloseAnimation = Animation.easeInOut(duration: 0.34)
    private let runtimeMenuHiddenOffset: CGFloat = 520

    private let libraryService: LibraryHomeServiceProtocol = LibraryHomeService()
    private let runtimeProfileService: GameRuntimeProfileServiceProtocol = GameRuntimeProfileService()
    private var copy: GameRuntimeCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color.clear
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                if gameReady {
                    GameWebView(
                        initialURL: gameEntryURL(),
                        bridge: runtimeSession.bridge,
                        schemeHandler: runtimeSession.schemeHandler,
                        sdkScript: GameManager.shared.getSDKContent(),
                        layoutMetrics: layoutMetrics(in: proxy),
                        language: language,
                        isLoading: $isLoading,
                        isContentVisible: $isContentVisible,
                        errorMessage: $errorMessage,
                        onWebViewReady: { webView in
                            currentWebView = webView
                        }
                    )
                    .id(reloadToken)
                    .opacity(isContentVisible ? 1 : 0)
                    .animation(.easeOut(duration: 0.18), value: isContentVisible)
                    .ignoresSafeArea()
                }

            }
            .nexusPageBackground()
            .overlay {
                runtimeStatusOverlay
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
            .overlay(alignment: .topTrailing) {
                CapsuleMenuOverlay(
                    onOpenMenu: {
                        presentRuntimeMenu()
                    },
                    onExit: { dismiss() },
                )
                .padding(.top, menuButtonRect(in: proxy).minY)
                .padding(.trailing, 20)
            }
            .overlay(alignment: .bottom) {
                NativeToastOverlay(message: $actionTip, bottomPadding: 24)
            }
            .overlay {
                if showRuntimeMenu {
                    runtimeMenuSheet
                }
            }
            .task {
                isFavorite = await GameEngagementStore.shared.isFavorite(gameID: game.id)
                await ensureAuthenticatedAndBoot()
                runtimeProfile = try? await runtimeProfileService.fetchRuntimeProfile(appID: game.id)
                await MainActor.run {
                    runtimeSession.bridge.updateMenuRectProvider {
                        menuButtonRect(in: proxy)
                    }
                }
            }
            .onChange(of: proxy.size) {
                Task { @MainActor in
                    runtimeSession.bridge.updateMenuRectProvider {
                        menuButtonRect(in: proxy)
                    }
                }
            }
            .onChange(of: isLoading) {
                updateLoadingOverlayState()
            }
            .onChange(of: isContentVisible) {
                updateLoadingOverlayState()
            }
            .onChange(of: errorMessage) {
                updateLoadingOverlayState()
            }
            .onReceive(NotificationCenter.default.publisher(for: AppLanguageStore.didChangeNotification)) { notification in
                if let language = notification.object as? AppLanguage {
                    self.language = language
                } else {
                    language = AppLanguageStore.currentSync()
                }
            }
        }
        .navigationBarHidden(true)
        .toolbar(.hidden, for: .tabBar)
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

    private var runtimeMenuSheet: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(backdropOpacity)
                .ignoresSafeArea()
                .onTapGesture {
                    dismissRuntimeMenu()
                }

            VStack(spacing: 0) {
                Capsule()
                    .fill(Color.white.opacity(0.18))
                    .frame(width: 40, height: 5)
                    .padding(.top, 12)
                    .contentShape(Rectangle())
                    .gesture(runtimeMenuDragGesture)

                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 16) {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color.white.opacity(0.08))
                            .frame(width: 64, height: 64)
                            .overlay(
                                Text(String(game.localizedName(for: language).prefix(1)))
                                    .font(.system(size: 28, weight: .bold))
                                    .foregroundStyle(.white)
                            )

                        VStack(alignment: .leading, spacing: 4) {
                            Text(game.localizedName(for: language))
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                            Text(runtimeMetaText)
                                .font(.system(size: 12))
                                .foregroundStyle(Color.white.opacity(0.72))
                                .lineLimit(1)
                                .truncationMode(.tail)
                        }

                        Spacer()

                        Button {
                            toggleFavorite()
                        } label: {
                            Image(systemName: isFavorite ? "star.fill" : "star")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(isFavorite ? Color(hex: 0xF5C451) : .white)
                                .frame(width: 40, height: 40)
                                .background(Color.white.opacity(0.08), in: Circle())
                        }
                        .buttonStyle(.plain)
                    }

                    Text(copy.menuShareTitle)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(Color.white.opacity(0.72))
                        .padding(.top, 30)

                    HStack(spacing: 12) {
                        shareShortcut(title: "WhatsApp", fill: Color(hex: 0x25D366), icon: "💬") {
                            shareToChannel("whatsapp")
                        }
                        shareShortcut(title: "Facebook", fill: Color(hex: 0x1877F2), icon: "f") {
                            shareToChannel("facebook")
                        }
                        shareShortcut(title: copy.menuShareLink, fill: Color.white.opacity(0.08), icon: "🔗") {
                            copyShareLink()
                        }
                    }
                    .padding(.top, 16)

                    VStack(spacing: 0) {
                        actionRow(
                            icon: isFavorite ? "star.fill" : "star",
                            title: isFavorite ? copy.menuRemoveFavorite : copy.menuAddFavorite
                        ) {
                            toggleFavorite()
                        }

                        dividerLine

                        actionRow(icon: "arrow.clockwise", title: copy.menuRestart) {
                            dismissRuntimeMenuThen {
                                retryLaunch()
                            }
                        }

                        dividerLine

                        actionRow(icon: "bubble.left.and.exclamationmark.bubble.right", title: copy.menuFeedback) {
                            sendFeedback()
                        }
                    }
                    .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .padding(.top, 24)

                    Button(copy.cancel) {
                        dismissRuntimeMenu()
                    }
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .padding(.top, 20)
                    .padding(.bottom, 16)
                }
                .padding(.horizontal, 24)
                .padding(.top, 10)
            }
            .frame(maxWidth: .infinity)
            .background(Color(hex: 0x1A1B23), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(Color.white.opacity(0.08), lineWidth: 1)
            )
            .padding(.horizontal, 12)
            .padding(.bottom, 0)
            .offset(y: runtimeMenuPresented ? runtimeMenuDragOffset : runtimeMenuHiddenOffset)
        }
        .ignoresSafeArea()
    }

    @ViewBuilder
    private var runtimeStatusOverlay: some View {
        if showLoadingOverlay || (hasCheckedAuth && isAuthenticated == false) {
            ZStack {
                Color.black.opacity(0.8)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    Rectangle()
                        .fill(Color(hex: 0x222433))
                        .frame(width: 96, height: 96)

                    Text(runtimeStatusText)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.top, 24)

                    RuntimeLoadingBar(progress: forceUpdating ? forceUpdateProgress : nil)
                        .frame(width: 220)
                        .frame(height: 4)
                        .padding(.top, 24)

                    if let errorMessage {
                        Button(copy.retry) {
                            retryLaunch()
                        }
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(Color(hex: 0x5C67FF).opacity(0.2), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .padding(.top, 20)

                        Text(errorMessage)
                            .font(.system(size: 13))
                            .foregroundStyle(Color.white.opacity(0.72))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                            .padding(.top, 16)
                    }
                }
                .padding(.horizontal, 24)
            }
            .opacity(loadingOverlayOpacity)
        } else if hasCheckedAuth && isAuthenticated == false {
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
        }
    }

    private var runtimeStatusText: String {
        if let errorMessage {
            return errorMessage
        }
        if forceUpdating {
            return copy.loadingResource
        }
        if gameReady {
            return copy.starting
        }
        return copy.preparing
    }

    @MainActor
    private func updateLoadingOverlayState() {
        let shouldKeepVisible = forceUpdating || isLoading || errorMessage != nil || (gameReady == false && hasCheckedAuth && isAuthenticated)

        if shouldKeepVisible {
            showLoadingOverlay = true
            withAnimation(.easeOut(duration: 0.12)) {
                loadingOverlayOpacity = 1
            }
            return
        }

        guard showLoadingOverlay else { return }

        withAnimation(.easeOut(duration: 0.28)) {
            loadingOverlayOpacity = 0
        }

        Task {
            try? await Task.sleep(nanoseconds: 300_000_000)
            await MainActor.run {
                if !(forceUpdating || isLoading || errorMessage != nil || (gameReady == false && hasCheckedAuth && isAuthenticated)) {
                    showLoadingOverlay = false
                }
            }
        }
    }

    private func bootGameIfNeeded() async {
        do {
            await MainActor.run {
                errorMessage = nil
                isLoading = true
                isContentVisible = false
                showLoadingOverlay = true
                loadingOverlayOpacity = 1
            }
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
                    errorMessage = userVisibleError(from: launchError.localizedDescription)
                } else {
                    errorMessage = userVisibleError(from: error.localizedDescription)
                }
                isLoading = false
                isContentVisible = false
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
        UIPasteboard.general.string = shareURLString
        showTip(copy.linkCopied)
        Task { try? await libraryService.markShared(appID: game.id) }
    }

    private func shareToChannel(_ channel: String) {
        presentActivitySheet(items: shareItems)
        showTip(String(format: copy.shareOpenedFormat, copy.channelName(channel)))
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
        let subject = "\(copy.feedbackEmailSubject) \(game.localizedName(for: language))"
        let body = feedbackBody

        if let mailURL = mailComposeURL(subject: subject, body: body),
           UIApplication.shared.canOpenURL(mailURL) {
            UIApplication.shared.open(mailURL)
            showTip(copy.feedbackOpened)
            return
        }

        presentActivitySheet(items: [body])
        showTip(copy.feedbackFallbackShared)
    }

    private func retryLaunch() {
        errorMessage = nil
        forceUpdating = false
        forceUpdateProgress = 0
        isContentVisible = false
        showLoadingOverlay = true
        loadingOverlayOpacity = 1
        if gameReady {
            reloadToken = UUID()
            isLoading = true
            return
        }
        hasBooted = false
        Task {
            await ensureAuthenticatedAndBoot()
        }
    }

    @ViewBuilder
    private func shareShortcut(title: String, fill: Color, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .fill(fill)
                    .frame(width: 50, height: 50)
                    .overlay(
                        Text(icon)
                            .font(.system(size: icon == "f" ? 24 : 20, weight: .bold))
                            .foregroundStyle(title == copy.menuShareLink ? .white : .white)
                    )
                Text(title)
                    .font(.system(size: 11))
                    .foregroundStyle(Color.white.opacity(0.72))
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func actionRow(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .semibold))
                    .frame(width: 18, height: 18)
                    .foregroundStyle(icon.contains("star") && isFavorite ? Color(hex: 0xF5C451) : .white)
                Text(title)
                    .font(.system(size: 15, weight: .bold))
                Spacer()
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .padding(.horizontal, 20)
        }
        .buttonStyle(.plain)
    }

    private var dividerLine: some View {
        Rectangle()
            .fill(Color.white.opacity(0.08))
            .frame(height: 1)
            .padding(.horizontal, 20)
    }

    private var runtimeMenuDragGesture: some Gesture {
        DragGesture(minimumDistance: 2, coordinateSpace: .global)
            .onChanged { value in
                guard runtimeMenuPresented else { return }
                runtimeMenuDragOffset = max(0, value.translation.height)
            }
            .onEnded { value in
                guard runtimeMenuPresented else { return }
                let shouldDismiss = value.translation.height > 120 || value.predictedEndTranslation.height > 180
                if shouldDismiss {
                    dismissRuntimeMenu()
                } else {
                    withAnimation(runtimeMenuOpenAnimation) {
                        runtimeMenuDragOffset = 0
                    }
                }
            }
    }

    @MainActor
    private func showTip(_ text: String) {
        actionTip = text
    }

    private func menuButtonRect(in proxy: GeometryProxy) -> CGRect {
        let width: CGFloat = 88
        let height: CGFloat = 34
        let x = proxy.size.width - 20 - width
        let y: CGFloat = 12
        return CGRect(x: x, y: y, width: width, height: height)
    }

    private func userVisibleError(from message: String) -> String {
        let raw = message.lowercased()
        if raw.contains("requires online") {
            return copy.requiresOnline
        }
        if raw.contains("checksum") {
            return copy.errorChecksum
        }
        if raw.contains("download") {
            return copy.errorDownload
        }
        if raw.contains("entry") {
            return copy.errorEntryMissing
        }
        if raw.contains("webview") || raw.contains("could not connect") || raw.contains("network") {
            return copy.errorWebView
        }
        return copy.errorStartup
    }

    private var runtimeMetaText: String {
        let playerText = runtimeProfile?.playerCountText.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let studioText = runtimeProfile?.studioName.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        if playerText.isEmpty == false && studioText.isEmpty == false {
            return "\(playerText) | \(studioText)"
        }
        if playerText.isEmpty == false {
            return playerText
        }
        if studioText.isEmpty == false {
            return studioText
        }
        return copy.menuMetaFallback
    }

    private var shareURLString: String {
        if game.downloadUrl.isEmpty == false {
            return game.downloadUrl
        }
        return "nexus://\(game.id)/index.html"
    }

    private var shareItems: [Any] {
        let subtitle = runtimeProfile?.shareSubtitle.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let items = [game.localizedName(for: language), subtitle, shareURLString].filter { $0.isEmpty == false }
        return [items.joined(separator: "\n")]
    }

    private var feedbackBody: String {
        return """
        \(copy.feedbackTemplateTitle)
        \(copy.feedbackGameLabel): \(game.localizedName(for: language))
        \(copy.feedbackAppIDLabel): \(game.id)
        \(copy.feedbackVersionLabel): \(game.version)
        \(copy.feedbackDescribeHint)
        """
    }

    private func presentActivitySheet(items: [Any]) {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController?
            .present(controller, animated: true)
    }

    private func mailComposeURL(subject: String, body: String) -> URL? {
        var components = URLComponents()
        components.scheme = "mailto"
        components.queryItems = [
            URLQueryItem(name: "subject", value: subject),
            URLQueryItem(name: "body", value: body)
        ]
        return components.url
    }

    @MainActor
    private func dismissRuntimeMenu() {
        guard showRuntimeMenu else { return }
        withAnimation(runtimeMenuCloseAnimation) {
            runtimeMenuDragOffset = 0
            runtimeMenuPresented = false
        }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 360_000_000)
            if runtimeMenuPresented == false {
                showRuntimeMenu = false
            }
        }
    }

    private func dismissRuntimeMenuThen(_ action: @escaping @MainActor () -> Void) {
        dismissRuntimeMenu()
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 340_000_000)
            await action()
        }
    }

    @MainActor
    private func presentRuntimeMenu() {
        guard showRuntimeMenu == false else {
            withAnimation(runtimeMenuOpenAnimation) {
                runtimeMenuDragOffset = 0
                runtimeMenuPresented = true
            }
            return
        }
        runtimeMenuDragOffset = 0
        showRuntimeMenu = true
        runtimeMenuPresented = false
        Task { @MainActor in
            await Task.yield()
            withAnimation(runtimeMenuOpenAnimation) {
                runtimeMenuPresented = true
            }
        }
    }

    private var backdropOpacity: Double {
        let baseOpacity = runtimeMenuPresented ? 0.42 : 0
        let dragFade = min(runtimeMenuDragOffset / 600, 0.2)
        return max(0, baseOpacity - dragFade)
    }

    private func layoutMetrics(in proxy: GeometryProxy) -> GameLayoutMetrics {
        GameLayoutMetrics(
            windowWidth: proxy.size.width,
            windowHeight: proxy.size.height,
            safeAreaTop: proxy.safeAreaInsets.top,
            safeAreaBottom: proxy.safeAreaInsets.bottom,
            safeAreaLeft: proxy.safeAreaInsets.leading,
            safeAreaRight: proxy.safeAreaInsets.trailing,
            menuButtonRect: menuButtonRect(in: proxy)
        )
    }
}

@MainActor
private final class GameRuntimeSession: ObservableObject {
    let bridge = JSBridge()
    let schemeHandler = NexusSchemeHandler()
}

private struct RuntimeLoadingBar: View {
    let progress: Double?
    @State private var animated = false

    var body: some View {
        GeometryReader { proxy in
            let track = Capsule()
            let indicatorWidth = max(8, proxy.size.width * 0.34)

            ZStack(alignment: .leading) {
                track
                    .fill(Color.white.opacity(0.16))

                Group {
                    if let progress {
                        Capsule()
                            .fill(Color(hex: 0x5C67FF))
                            .frame(width: max(8, proxy.size.width * progress.clamped(to: 0...1)))
                    } else {
                        Capsule()
                            .fill(Color(hex: 0x5C67FF))
                            .frame(width: indicatorWidth)
                            .offset(x: animated ? proxy.size.width - indicatorWidth : 0)
                            .animation(
                                .linear(duration: 0.9).repeatForever(autoreverses: true),
                                value: animated
                            )
                    }
                }
                .clipShape(track)
            }
        }
        .onAppear {
            animated = true
        }
        .onChange(of: progress) {
            if progress == nil {
                animated = true
            }
        }
    }
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

private struct GameRuntimeCopy {
    let loginRequired: String
    let goLogin: String
    let loading: String
    let preparing: String
    let loadingResource: String
    let starting: String
    let updating: String
    let launchFailed: String
    let confirm: String
    let unknownError: String
    let retry: String
    let errorChecksum: String
    let errorDownload: String
    let errorEntryMissing: String
    let errorWebView: String
    let errorStartup: String
    let requiresOnline: String
    let linkCopied: String
    let shareOpenedFormat: String
    let addedFavorite: String
    let removedFavorite: String
    let feedbackOpened: String
    let feedbackFallbackShared: String
    let whatsapp: String
    let facebook: String
    let more: String
    let cancel: String
    let menuMetaFallback: String
    let menuShareTitle: String
    let menuShareLink: String
    let menuAddFavorite: String
    let menuRemoveFavorite: String
    let menuRestart: String
    let menuFeedback: String
    let feedbackEmailSubject: String
    let feedbackTemplateTitle: String
    let feedbackGameLabel: String
    let feedbackAppIDLabel: String
    let feedbackVersionLabel: String
    let feedbackDescribeHint: String

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
                preparing: "正在初始化执行环境...",
                loadingResource: "正在准备游戏资源...",
                starting: "正在启动游戏...",
                updating: "正在更新游戏资源",
                launchFailed: "启动失败",
                confirm: "确定",
                unknownError: "未知错误",
                retry: "重试",
                errorChecksum: "游戏资源校验失败，请重试",
                errorDownload: "游戏下载失败，请检查网络后重试",
                errorEntryMissing: "游戏入口文件缺失，请重试",
                errorWebView: "游戏页面载入失败，请重试",
                errorStartup: "游戏启动失败，请重试",
                requiresOnline: "该游戏需要连接在线服务，离线时暂不可用",
                linkCopied: "分享链接已复制",
                shareOpenedFormat: "已打开 %@ 分享",
                addedFavorite: "已加入收藏",
                removedFavorite: "已移出收藏",
                feedbackOpened: "已打开反馈邮件",
                feedbackFallbackShared: "已打开系统分享，请提交反馈内容",
                whatsapp: "WhatsApp",
                facebook: "Facebook",
                more: "更多",
                cancel: "取消",
                menuMetaFallback: "暂无开发者信息",
                menuShareTitle: "分享至",
                menuShareLink: "复制链接",
                menuAddFavorite: "加入我的游戏",
                menuRemoveFavorite: "从我的游戏移除",
                menuRestart: "重新进入游戏",
                menuFeedback: "反馈与投诉",
                feedbackEmailSubject: "游戏反馈与投诉",
                feedbackTemplateTitle: "请填写你的问题描述：",
                feedbackGameLabel: "游戏名称",
                feedbackAppIDLabel: "游戏 ID",
                feedbackVersionLabel: "游戏版本",
                feedbackDescribeHint: "问题描述："
            )
        case .traditionalChinese:
            return .init(
                loginRequired: "需要登入才能啟動遊戲",
                goLogin: "去登入",
                loading: "載入中...",
                preparing: "正在初始化運行環境...",
                loadingResource: "正在準備遊戲資源...",
                starting: "正在啟動遊戲...",
                updating: "正在更新遊戲資源",
                launchFailed: "啟動失敗",
                confirm: "確定",
                unknownError: "未知錯誤",
                retry: "重試",
                errorChecksum: "遊戲資源校驗失敗，請重試",
                errorDownload: "遊戲下載失敗，請檢查網路後重試",
                errorEntryMissing: "遊戲入口檔案缺失，請重試",
                errorWebView: "遊戲頁面載入失敗，請重試",
                errorStartup: "遊戲啟動失敗，請重試",
                requiresOnline: "該遊戲需要連接在線服務，離線時暫不可用",
                linkCopied: "分享連結已複製",
                shareOpenedFormat: "已打開 %@ 分享",
                addedFavorite: "已加入收藏",
                removedFavorite: "已移出收藏",
                feedbackOpened: "已打開回饋郵件",
                feedbackFallbackShared: "已打開系統分享，請提交回饋內容",
                whatsapp: "WhatsApp",
                facebook: "Facebook",
                more: "更多",
                cancel: "取消",
                menuMetaFallback: "暫無開發者資訊",
                menuShareTitle: "分享至",
                menuShareLink: "複製鏈接",
                menuAddFavorite: "加入我的遊戲",
                menuRemoveFavorite: "從我的遊戲移除",
                menuRestart: "重新進入遊戲",
                menuFeedback: "反饋與投訴",
                feedbackEmailSubject: "遊戲反饋與投訴",
                feedbackTemplateTitle: "請填寫你的問題描述：",
                feedbackGameLabel: "遊戲名稱",
                feedbackAppIDLabel: "遊戲 ID",
                feedbackVersionLabel: "遊戲版本",
                feedbackDescribeHint: "問題描述："
            )
        case .english:
            return .init(
                loginRequired: "Sign in to launch this game",
                goLogin: "Sign In",
                loading: "Loading...",
                preparing: "Initializing runtime...",
                loadingResource: "Preparing game resources...",
                starting: "Starting game...",
                updating: "Updating game resources",
                launchFailed: "Launch Failed",
                confirm: "OK",
                unknownError: "Unknown error",
                retry: "Retry",
                errorChecksum: "Game resource validation failed. Please try again.",
                errorDownload: "Game download failed. Check your network and try again.",
                errorEntryMissing: "Game entry file is missing. Please try again.",
                errorWebView: "Game page failed to load. Please try again.",
                errorStartup: "Game failed to start. Please try again.",
                requiresOnline: "This game requires online services and is temporarily unavailable offline.",
                linkCopied: "Share link copied",
                shareOpenedFormat: "Opened %@ share",
                addedFavorite: "Added to favorites",
                removedFavorite: "Removed from favorites",
                feedbackOpened: "Feedback email opened",
                feedbackFallbackShared: "System share opened for feedback",
                whatsapp: "WhatsApp",
                facebook: "Facebook",
                more: "More",
                cancel: "Cancel",
                menuMetaFallback: "Developer info unavailable",
                menuShareTitle: "Share to",
                menuShareLink: "Copy link",
                menuAddFavorite: "Add to My Games",
                menuRemoveFavorite: "Remove from My Games",
                menuRestart: "Restart Game",
                menuFeedback: "Feedback & Report",
                feedbackEmailSubject: "Game Feedback & Report",
                feedbackTemplateTitle: "Please describe the issue:",
                feedbackGameLabel: "Game",
                feedbackAppIDLabel: "Game ID",
                feedbackVersionLabel: "Version",
                feedbackDescribeHint: "Issue details:"
            )
        }
    }
}
