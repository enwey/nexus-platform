import SwiftUI

@main
struct NexusPlatformApp: App {
    init() {
        let navigationAppearance = UINavigationBarAppearance()
        navigationAppearance.configureWithOpaqueBackground()
        navigationAppearance.backgroundColor = UIColor(
            red: 18 / 255,
            green: 18 / 255,
            blue: 18 / 255,
            alpha: 1
        )
        navigationAppearance.shadowColor = UIColor.white.withAlphaComponent(0.04)
        navigationAppearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationAppearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]

        let navigationBar = UINavigationBar.appearance()
        navigationBar.standardAppearance = navigationAppearance
        navigationBar.scrollEdgeAppearance = navigationAppearance
        navigationBar.compactAppearance = navigationAppearance
        navigationBar.compactScrollEdgeAppearance = navigationAppearance
        navigationBar.tintColor = .white
        navigationBar.prefersLargeTitles = false

    }

    var body: some Scene {
        WindowGroup {
            LaunchExperienceView()
                .preferredColorScheme(.dark)
        }
    }
}

struct RootTabView: View {
    enum Tab: Hashable {
        case library
        case discover
        case recommend
        case profile
    }

    @State private var selection: Tab = .library
    @State private var language: AppLanguage = AppLanguageStore.currentSync()
    @StateObject private var biometricController = BiometricAuthController.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack {
                LibraryView()
            }
            .tabItem {
                Label(copy.library, systemImage: "gamecontroller.fill")
            }
            .tag(Tab.library)

            NavigationStack {
                DiscoverView()
            }
            .tabItem {
                Label(copy.discover, systemImage: "sparkles")
            }
            .tag(Tab.discover)

            NavigationStack {
                RecommendView()
            }
            .tabItem {
                Label(copy.recommend, systemImage: "play.square.fill")
            }
            .tag(Tab.recommend)

            NavigationStack {
                ProfileView()
            }
            .tabItem {
                Label(copy.profile, systemImage: "person.crop.circle.fill")
            }
            .tag(Tab.profile)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .tint(Color(hex: 0x6B4EFF))
        .toolbarBackground(Color(hex: 0x121212), for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
        .overlay {
            if biometricController.isEnabled && biometricController.isLocked {
                BiometricLockOverlay(
                    language: language,
                    biometricTitle: biometricController.availability.kind.title(for: language),
                    isAuthenticating: biometricController.isAuthenticating,
                    onUnlock: {
                        biometricController.unlock(language: language)
                    }
                )
            }
        }
        .onAppear {
            biometricController.loadIfNeeded()
            biometricController.handleScenePhase(scenePhase)
        }
        .onChange(of: scenePhase) { _, newPhase in
            biometricController.handleScenePhase(newPhase)
        }
        .onReceive(NotificationCenter.default.publisher(for: AppLanguageStore.didChangeNotification)) { notification in
            if let language = notification.object as? AppLanguage {
                self.language = language
            } else {
                language = AppLanguageStore.currentSync()
            }
        }
    }
}

private struct BiometricLockOverlay: View {
    let language: AppLanguage
    let biometricTitle: String
    let isAuthenticating: Bool
    let onUnlock: () -> Void

    private var copy: BiometricOverlayCopy {
        .forLanguage(language)
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.78)
                .ignoresSafeArea()

            VStack(spacing: 18) {
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(.white)

                Text(copy.title)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.white)

                Text(copy.message.replacingOccurrences(of: "{biometric}", with: biometricTitle))
                    .font(.system(size: 14))
                    .foregroundStyle(Color.white.opacity(0.72))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)

                Button {
                    onUnlock()
                } label: {
                    Text(isAuthenticating ? copy.authenticating : copy.button)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(isAuthenticating)
            }
            .padding(24)
            .frame(maxWidth: 320)
            .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .stroke(Color.white.opacity(0.08), lineWidth: 1)
            )
            .padding(.horizontal, 24)
        }
    }
}

private struct BiometricOverlayCopy {
    let title: String
    let message: String
    let button: String
    let authenticating: String

    static func forLanguage(_ language: AppLanguage) -> BiometricOverlayCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "需要身份验证",
                message: "请使用 {biometric} 或设备密码继续访问账号内容。",
                button: "立即验证",
                authenticating: "验证中..."
            )
        case .traditionalChinese:
            return .init(
                title: "需要身分驗證",
                message: "請使用 {biometric} 或裝置密碼繼續存取帳號內容。",
                button: "立即驗證",
                authenticating: "驗證中..."
            )
        case .english:
            return .init(
                title: "Verification Required",
                message: "Use {biometric} or your device passcode to continue.",
                button: "Verify Now",
                authenticating: "Verifying..."
            )
        }
    }
}

private extension RootTabView {
    var copy: RootTabCopy {
        .forLanguage(language)
    }
}

private struct RootTabCopy {
    let library: String
    let discover: String
    let recommend: String
    let profile: String

    static func forLanguage(_ language: AppLanguage) -> RootTabCopy {
        switch language {
        case .simplifiedChinese:
            return .init(library: "游戏", discover: "发现", recommend: "推荐", profile: "我的")
        case .traditionalChinese:
            return .init(library: "遊戲", discover: "發現", recommend: "推薦", profile: "我的")
        case .english:
            return .init(library: "Games", discover: "Discover", recommend: "Recommend", profile: "Profile")
        }
    }
}

enum AppText {
    static func pleaseLogin(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "请先登录"
        case .traditionalChinese: return "請先登入"
        case .english: return "Please sign in first"
        }
    }

    static func invalidServiceResponse(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "服务响应无效"
        case .traditionalChinese: return "服務回應無效"
        case .english: return "Invalid service response"
        }
    }

    static func invalidResponse(path: String, language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "服务响应无效\n\(path)"
        case .traditionalChinese: return "服務回應無效\n\(path)"
        case .english: return "Invalid service response\n\(path)"
        }
    }

    static func requestFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "请求失败"
        case .traditionalChinese: return "請求失敗"
        case .english: return "Request failed"
        }
    }

    static func loginFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "登录失败"
        case .traditionalChinese: return "登入失敗"
        case .english: return "Sign in failed"
        }
    }

    static func invalidBillingResponse(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "账单数据响应无效"
        case .traditionalChinese: return "賬單資料回應無效"
        case .english: return "Invalid billing response"
        }
    }

    static func fetchBillingFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "获取账单失败"
        case .traditionalChinese: return "取得賬單失敗"
        case .english: return "Failed to load billing"
        }
    }

    static func invalidDeviceResponse(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "设备数据响应无效"
        case .traditionalChinese: return "裝置資料回應無效"
        case .english: return "Invalid device response"
        }
    }

    static func deviceActionFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "设备操作失败"
        case .traditionalChinese: return "裝置操作失敗"
        case .english: return "Device action failed"
        }
    }

    static func invalidReferralResponse(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "邀请数据响应无效"
        case .traditionalChinese: return "邀請資料回應無效"
        case .english: return "Invalid referral response"
        }
    }

    static func invalidUserProfileResponse(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "用户资料响应无效"
        case .traditionalChinese: return "使用者資料回應無效"
        case .english: return "Invalid profile response"
        }
    }

    static func invalidWalletResponse(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "钱包数据响应无效"
        case .traditionalChinese: return "錢包資料回應無效"
        case .english: return "Invalid wallet response"
        }
    }

    static func fetchWalletFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "获取钱包失败"
        case .traditionalChinese: return "取得錢包失敗"
        case .english: return "Failed to load wallet"
        }
    }

    static func invalidLibraryResponse(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏库数据响应无效"
        case .traditionalChinese: return "遊戲庫資料回應無效"
        case .english: return "Invalid game library response"
        }
    }

    static func updateCheckInvalidURL(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "更新检查地址无效"
        case .traditionalChinese: return "更新檢查位址無效"
        case .english: return "Invalid update check URL"
        }
    }

    static func updateCheckUnavailable(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "更新检查服务不可用"
        case .traditionalChinese: return "更新檢查服務不可用"
        case .english: return "Update check service unavailable"
        }
    }

    static func updateCheckParseFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "更新检查返回格式错误"
        case .traditionalChinese: return "更新檢查回傳格式錯誤"
        case .english: return "Invalid update check response"
        }
    }

    static func updateCheckFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "更新检查失败"
        case .traditionalChinese: return "更新檢查失敗"
        case .english: return "Update check failed"
        }
    }

    static func launchUpdateCheckFailed(_ message: String, language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "更新检查失败：\(message)"
        case .traditionalChinese: return "更新檢查失敗：\(message)"
        case .english: return "Update check failed: \(message)"
        }
    }

    static func packageInstallFailed(_ message: String, language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏包安装失败：\(message)"
        case .traditionalChinese: return "遊戲包安裝失敗：\(message)"
        case .english: return "Game package install failed: \(message)"
        }
    }

    static func forceUpdateFailed(_ message: String, language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "强制更新失败：\(message)"
        case .traditionalChinese: return "強制更新失敗：\(message)"
        case .english: return "Forced update failed: \(message)"
        }
    }

    static func fallbackDownloadFailed(_ message: String, language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "首包下载失败：\(message)"
        case .traditionalChinese: return "首包下載失敗：\(message)"
        case .english: return "Initial package download failed: \(message)"
        }
    }

    static func gamePageLoadFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏页面载入失败，请重试"
        case .traditionalChinese: return "遊戲頁面載入失敗，請重試"
        case .english: return "Game page failed to load. Please try again."
        }
    }

    static func checksumMismatch(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏包校验失败"
        case .traditionalChinese: return "遊戲包校驗失敗"
        case .english: return "Game package checksum mismatch"
        }
    }

    static func entryFileMissing(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏入口文件不存在"
        case .traditionalChinese: return "遊戲入口檔案不存在"
        case .english: return "Game entry file not found"
        }
    }

    static func versionMissing(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏版本不存在"
        case .traditionalChinese: return "遊戲版本不存在"
        case .english: return "Game version does not exist"
        }
    }

    static func runtimeKeyUnavailable(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏运行密钥不可用"
        case .traditionalChinese: return "遊戲運行金鑰不可用"
        case .english: return "Game runtime key is unavailable"
        }
    }

    static func invalidHostedManifest(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "小游戏清单无效，请重新上传符合规范的包"
        case .traditionalChinese: return "小遊戲清單無效，請重新上傳符合規範的包"
        case .english: return "Mini app manifest is invalid. Please upload a compliant package."
        }
    }

    static func invalidSecurePackage(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏安全包无效"
        case .traditionalChinese: return "遊戲安全包無效"
        case .english: return "Game secure package is invalid"
        }
    }

    static func securePayloadMissing(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "游戏安全包内容缺失"
        case .traditionalChinese: return "遊戲安全包內容缺失"
        case .english: return "Game secure payload is missing"
        }
    }

    static func unsupportedSecurePackage(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "当前客户端不支持该游戏安全包格式"
        case .traditionalChinese: return "目前客戶端不支援該遊戲安全包格式"
        case .english: return "Game secure package format is unsupported"
        }
    }

    static func loggedOut(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "已退出登录"
        case .traditionalChinese: return "已登出"
        case .english: return "Signed out"
        }
    }

    static func loginSucceeded(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "登录成功"
        case .traditionalChinese: return "登入成功"
        case .english: return "Signed in successfully"
        }
    }

    static func accountTerminated(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "账号已注销"
        case .traditionalChinese: return "帳號已註銷"
        case .english: return "Account terminated"
        }
    }

    static func deviceKicked(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "设备已下线"
        case .traditionalChinese: return "裝置已下線"
        case .english: return "Device signed out"
        }
    }

    static func otherDevicesLoggedOut(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "其他设备已退出"
        case .traditionalChinese: return "其他裝置已登出"
        case .english: return "Other devices signed out"
        }
    }

    static func languageUpdated(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "语言已更新"
        case .traditionalChinese: return "語言已更新"
        case .english: return "Language updated"
        }
    }

    static func cloudSyncEnabled(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "云同步已开启"
        case .traditionalChinese: return "雲端同步已開啟"
        case .english: return "Cloud sync enabled"
        }
    }

    static func cloudSyncDisabled(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "云同步已关闭"
        case .traditionalChinese: return "雲端同步已關閉"
        case .english: return "Cloud sync disabled"
        }
    }

    static func biometricUnavailable(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "当前设备暂不支持生物识别验证"
        case .traditionalChinese: return "目前裝置暫不支援生物辨識驗證"
        case .english: return "Biometric verification is not available on this device"
        }
    }

    static func biometricEnableFailed(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "生物识别未开启，请完成系统验证后重试"
        case .traditionalChinese: return "生物辨識未開啟，請完成系統驗證後再試一次"
        case .english: return "Biometric login was not enabled. Complete system verification and try again."
        }
    }

    static func biometricEnableReason(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "开启生物识别，用于保护账号安全"
        case .traditionalChinese: return "開啟生物辨識，用於保護帳號安全"
        case .english: return "Enable biometric login to protect your account"
        }
    }

    static func biometricUnlockReason(_ language: AppLanguage = AppLanguageStore.currentSync()) -> String {
        switch language {
        case .simplifiedChinese: return "验证身份后继续访问账号内容"
        case .traditionalChinese: return "驗證身分後繼續存取帳號內容"
        case .english: return "Verify your identity to continue"
        }
    }
}

enum NativeMotion {
    static let skeletonCycle: TimeInterval = 1.45
    static let skeletonAngle: Double = 18
    static let skeletonBandWidthRatio: CGFloat = 0.34
    static let skeletonBandMinWidth: CGFloat = 24
    static let skeletonBandHeightMultiplier: CGFloat = 2.4
    static let skeletonBandVerticalOffsetMultiplier: CGFloat = 0.7
    static let skeletonBlurRadius: CGFloat = 8
    static let toastPresent = Animation.interpolatingSpring(duration: 0.34, bounce: 0.05)
    static let toastDismiss = Animation.easeInOut(duration: 0.24)
    static let press = Animation.easeOut(duration: 0.12)
    static let overlayTransition = Animation.easeInOut(duration: 0.24)
    static let stateSwapTransition: AnyTransition = .opacity.combined(with: .scale(scale: 0.985, anchor: .top))
    static let contentRevealTransition: AnyTransition = .opacity.combined(with: .scale(scale: 0.992, anchor: .center))
    static let skeletonBaseOpacity = 0.06
    static let skeletonGlowOpacity = 0.12
    static let pillBackgroundOpacity = 0.72
    static let sectionOverlayOpacity = 0.16
}

struct NativeToastOverlay: View {
    @Binding var message: String?
    var bottomPadding: CGFloat = 26

    @State private var renderedMessage: String?
    @State private var isPresented = false
    @State private var dismissTask: Task<Void, Never>?

    var body: some View {
        ZStack {
            if let renderedMessage {
                Text(renderedMessage)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.82), in: Capsule())
                    .opacity(isPresented ? 1 : 0)
                    .offset(y: isPresented ? 0 : 12)
                    .scaleEffect(isPresented ? 1 : 0.98)
                    .animation(NativeMotion.toastPresent, value: isPresented)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .allowsHitTesting(false)
        .onAppear {
            showIfNeeded(message)
        }
        .onChange(of: message) {
            showIfNeeded(message)
        }
        .onDisappear {
            dismissTask?.cancel()
            dismissTask = nil
        }
    }

    @MainActor
    private func showIfNeeded(_ nextMessage: String?) {
        dismissTask?.cancel()
        dismissTask = nil

        guard let nextMessage, nextMessage.isEmpty == false else {
            hideRenderedMessage()
            return
        }

        renderedMessage = nextMessage
        withAnimation(NativeMotion.toastPresent) {
            isPresented = true
        }

        dismissTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard message == nextMessage else { return }
            hideRenderedMessage()
        }
    }

    @MainActor
    private func hideRenderedMessage() {
        withAnimation(NativeMotion.toastDismiss) {
            isPresented = false
        }

        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 280_000_000)
            if isPresented == false {
                renderedMessage = nil
                message = nil
            }
        }
    }
}

struct NativeCenteredModal<Content: View>: View {
    @Binding var isPresented: Bool
    let dismissOnScrimTap: Bool
    let content: Content

    init(
        isPresented: Binding<Bool>,
        dismissOnScrimTap: Bool = true,
        @ViewBuilder content: () -> Content
    ) {
        _isPresented = isPresented
        self.dismissOnScrimTap = dismissOnScrimTap
        self.content = content()
    }

    var body: some View {
        ZStack {
            if isPresented {
                Color.black.opacity(0.48)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                    .onTapGesture {
                        guard dismissOnScrimTap else { return }
                        withAnimation(NativeMotion.overlayTransition) {
                            isPresented = false
                        }
                    }

                content
                    .padding(24)
                    .frame(maxWidth: 360)
                    .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                    )
                    .padding(.horizontal, 24)
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .allowsHitTesting(isPresented)
        .animation(NativeMotion.overlayTransition, value: isPresented)
    }
}

struct BrandLogoImage: View {
    var size: CGFloat
    var cornerRadius: CGFloat? = nil

    var body: some View {
        Group {
            if let image = brandUIImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                ZStack {
                    LinearGradient(
                        colors: [Color(hex: 0x2D69FF), Color(hex: 0x7D37FF)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    Text("B")
                        .font(.system(size: size * 0.42, weight: .black))
                        .foregroundStyle(.white)
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(
            RoundedRectangle(
                cornerRadius: cornerRadius ?? size * 0.24,
                style: .continuous
            )
        )
        .overlay(
            RoundedRectangle(
                cornerRadius: cornerRadius ?? size * 0.24,
                style: .continuous
            )
            .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.22), radius: 18, y: 8)
    }

    private var brandUIImage: UIImage? {
        if let path = Bundle.main.path(forResource: "BringBox", ofType: "png"),
           let image = UIImage(contentsOfFile: path) {
            return image
        }

        return UIImage(named: "BringBox", in: .main, compatibleWith: nil)
    }
}

struct NativeNavigationButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.72 : 1)
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(NativeMotion.press, value: configuration.isPressed)
    }
}

struct NativeStateCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 10) {
            content
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(Color.white.opacity(0.04), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }
}

struct NativeSkeletonBlock: View {
    var width: CGFloat? = nil
    var height: CGFloat
    var cornerRadius: CGFloat

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)

        shape
            .fill(Color.white.opacity(NativeMotion.skeletonBaseOpacity))
            .frame(width: width, height: height)
            .overlay {
                TimelineView(.animation) { context in
                    GeometryReader { proxy in
                        let size = proxy.size
                        let seconds = context.date.timeIntervalSinceReferenceDate
                        let progress = seconds.truncatingRemainder(dividingBy: NativeMotion.skeletonCycle) / NativeMotion.skeletonCycle
                        let referenceWidth = max(size.width, size.height)
                        let bandWidth = max(referenceWidth * NativeMotion.skeletonBandWidthRatio, NativeMotion.skeletonBandMinWidth)
                        let travel = size.width + bandWidth * 2
                        let offset = (-bandWidth) + (travel * progress)

                        Rectangle()
                            .fill(
                                LinearGradient(
                                    colors: [
                                        .clear,
                                        Color.white.opacity(NativeMotion.skeletonGlowOpacity * 0.45),
                                        Color.white.opacity(NativeMotion.skeletonGlowOpacity),
                                        Color.white.opacity(NativeMotion.skeletonGlowOpacity * 0.45),
                                        .clear
                                    ],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: bandWidth, height: max(size.width, size.height) * NativeMotion.skeletonBandHeightMultiplier)
                            .rotationEffect(.degrees(NativeMotion.skeletonAngle))
                            .blur(radius: NativeMotion.skeletonBlurRadius)
                            .offset(
                                x: offset - bandWidth,
                                y: -max(size.width, size.height) * NativeMotion.skeletonBandVerticalOffsetMultiplier
                            )
                    }
                }
                .mask(shape)
            }
    }

    init(width: CGFloat? = nil, height: CGFloat, cornerRadius: CGFloat = 16) {
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }
}

struct NativeSkeletonText: View {
    let widths: [CGFloat]
    var lineHeight: CGFloat = 12
    var spacing: CGFloat = 8
    var cornerRadius: CGFloat = 6

    var body: some View {
        VStack(alignment: .leading, spacing: spacing) {
            ForEach(Array(widths.enumerated()), id: \.offset) { _, width in
                NativeSkeletonBlock(width: width, height: lineHeight, cornerRadius: cornerRadius)
            }
        }
    }
}

struct NativeSkeletonIcon: View {
    var size: CGFloat
    var cornerRadius: CGFloat

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(Color(hex: 0x232326))
            .overlay {
                NativeSkeletonBlock(height: size, cornerRadius: cornerRadius)
            }
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
            )
    }
}

struct NativeRefreshPill: View {
    let text: String

    var body: some View {
        HStack(spacing: 8) {
            ProgressView()
                .tint(Color.white)
                .scaleEffect(0.82)
            Text(text)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.black.opacity(NativeMotion.pillBackgroundOpacity), in: Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}

struct NativeSectionRefreshOverlay: View {
    var lineWidths: [CGFloat] = [92, 64]
    var cornerRadius: CGFloat = 20

    var body: some View {
        ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .fill(Color.black.opacity(NativeMotion.sectionOverlayOpacity))

            VStack(alignment: .trailing, spacing: 8) {
                NativeRefreshPill(text: AppLanguageStore.currentSync() == .english ? "Refreshing" : "正在刷新")
                VStack(alignment: .trailing, spacing: 8) {
                    ForEach(Array(lineWidths.enumerated()), id: \.offset) { _, width in
                        NativeSkeletonBlock(width: width, height: 10, cornerRadius: 5)
                    }
                }
            }
            .padding(14)
        }
        .allowsHitTesting(false)
        .transition(.opacity.combined(with: .scale(scale: 0.985, anchor: .topTrailing)))
    }
}
