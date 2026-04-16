import SwiftUI

struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()
    @State private var showAuthFlow = false
    @State private var showLanguageDialog = false
    @State private var cacheSizeText = "0 B"
    @State private var language: AppLanguage = AppLanguageStore.currentSync()
    @State private var toastMessage: String?
    @State private var showBilling = false
    @State private var showReferral = false
    @State private var showSecurity = false

    var body: some View {
        VStack(spacing: 24) {
            userCard
            walletCard
            referralCard
            menuGroup
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding(.leading, 24)
        .padding(.top, 24)
        .padding(.trailing, 24)
        .padding(.bottom, 96)
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear {
            language = viewModel.selectedLanguage
            viewModel.loadSession()
            refreshCacheSize()
        }
        .onChange(of: viewModel.selectedLanguage) { _, newValue in
            language = newValue
        }
        .onChange(of: viewModel.message) { _, newValue in
            if let newValue, newValue.isEmpty == false {
                toastMessage = newValue
            }
        }
        .sheet(isPresented: $showAuthFlow) {
            NavigationStack {
                AuthFlowView { session in
                    viewModel.applyAuthenticatedSession(session)
                }
            }
        }
        .confirmationDialog(copy.languageDialogTitle, isPresented: $showLanguageDialog, titleVisibility: .visible) {
            ForEach(AppLanguage.allCases) { item in
                Button(item.title) {
                    viewModel.setLanguage(item)
                }
            }
            Button(copy.cancel, role: .cancel) {}
        }
        .overlay(alignment: .bottom) {
            if let toastMessage {
                Text(toastMessage)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.82), in: Capsule())
                    .padding(.bottom, 26)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            withAnimation(.easeOut(duration: 0.2)) {
                                if self.toastMessage == toastMessage {
                                    self.toastMessage = nil
                                }
                            }
                        }
                }
            }
        }
        .navigationDestination(isPresented: $showBilling) {
            BillingListView()
        }
        .navigationDestination(isPresented: $showReferral) {
            ReferralView()
        }
        .navigationDestination(isPresented: $showSecurity) {
            AccountSecurityView(
                email: viewModel.currentEmail ?? "",
                onSavedLanguage: nil,
                cloudSyncEnabled: viewModel.cloudSyncEnabled,
                onCloudSyncToggle: { viewModel.setCloudSyncEnabled($0) },
                selectedLanguage: viewModel.selectedLanguage,
                onLanguageSelect: { viewModel.setLanguage($0) },
                onRequestLogin: { showAuthFlow = true },
                onLogoutCurrent: { viewModel.logout() }
            )
        }
    }

    private var copy: ProfileCopy {
        .forLanguage(language)
    }

    private var userCard: some View {
        HStack(alignment: .center, spacing: 20) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 80, height: 80)

                Circle()
                    .fill(Color(hex: 0x1C1C1F))
                    .frame(width: 76, height: 76)

                Image(systemName: "person.crop.circle.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(Color(hex: 0xD4D4DC))
            }

            VStack(alignment: .leading, spacing: 6) {
                Text(displayName)
                    .font(.system(size: 28, weight: .black))
                    .foregroundStyle(.white)

                Text(accountBadgeText)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(hex: 0x232326))
                    )
            }

            Spacer()

            if viewModel.isLoggedIn == false {
                Button(copy.loginButton) {
                    showAuthFlow = true
                }
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color(hex: 0x6B4EFF), in: Capsule())
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var walletCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 8) {
                Text(copy.walletTitle)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.white.opacity(0.8))
                    .lineLimit(1)

                Text(walletBalanceText)
                    .font(.system(size: 34, weight: .heavy, design: .monospaced))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }

            Spacer()

            HStack(spacing: 12) {
                Button {
                    requireLogin {
                        showBilling = true
                    }
                } label: {
                    walletAction(title: copy.billButton, filled: true)
                }
                .buttonStyle(.plain)

                Button {
                    showReferral = true
                } label: {
                    walletAction(title: copy.howToEarnButton, filled: false)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .frame(height: 164)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ),
            in: RoundedRectangle(cornerRadius: 20, style: .continuous)
        )
    }

    private func walletAction(title: String, filled: Bool) -> some View {
        Text(title)
            .font(.system(size: 14, weight: .bold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 38)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.white.opacity(filled ? 0.2 : 0.1))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(filled ? .clear : Color.white.opacity(0.3), lineWidth: 1)
            )
    }

    private var referralCard: some View {
        HStack(alignment: .center, spacing: 16) {
            Text("🎁")
                .font(.system(size: 34))

            VStack(alignment: .leading, spacing: 0) {
                Text(copy.referralTitle)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)

                HStack(alignment: .center, spacing: 12) {
                    Text(copy.referralSubtitle)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .lineLimit(1)

                    Spacer()

                    Button(copy.referralButton) {
                        requireLogin {
                            showReferral = true
                        }
                    }
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(Color(hex: 0x121212))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 6)
                    .background(Color(hex: 0x6B4EFF), in: Capsule())
                }
                .padding(.top, 8)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x6B4EFF, alpha: 0.15), Color(hex: 0xA04CFF, alpha: 0.15)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0x6B4EFF), lineWidth: 1)
        )
    }

    private var menuGroup: some View {
        VStack(spacing: 0) {
            menuRow(title: copy.security, trailing: nil, showsChevron: true) {
                requireLogin {
                    showSecurity = true
                }
            }

            divider

            menuRow(
                title: copy.cloudSync,
                trailing: viewModel.isLoggedIn && viewModel.cloudSyncEnabled ? copy.syncEnabled : copy.syncDisabled,
                showsChevron: false
            ) {
                requireLogin {
                    viewModel.setCloudSyncEnabled(!viewModel.cloudSyncEnabled)
                }
            }

            divider

            menuRow(title: copy.clearCache, trailing: cacheSizeText, showsChevron: false) {
                Task {
                    let storage = VersionedGameStorageManager()
                    try? await storage.clearAllLocalCaches()
                    await MainActor.run {
                        refreshCacheSize()
                        toastMessage = "\(copy.cacheCleared) (\(cacheSizeText))"
                    }
                }
            }

            divider

            menuRow(title: copy.language, trailing: viewModel.selectedLanguage.title, showsChevron: false) {
                showLanguageDialog = true
            }
        }
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private func menuRow(title: String, trailing: String?, showsChevron: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(title)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.white)

                Spacer()

                if let trailing {
                    Text(trailing)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                } else if showsChevron {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 18)
        }
        .buttonStyle(.plain)
    }

    private var divider: some View {
        Rectangle()
            .fill(Color(hex: 0x2D2D31))
            .frame(height: 1)
    }

    private var displayName: String {
        if viewModel.isLoggedIn {
            return viewModel.displayName ?? copy.headerTitle
        }
        return copy.guestMode
    }

    private var accountBadgeText: String {
        if viewModel.isLoggedIn, let userID = viewModel.userID {
            return String(format: copy.accountIDFormat, String(userID))
        }
        if viewModel.isLoggedIn {
            return copy.accountPlaceholder
        }
        return copy.notLoggedIn
    }

    private var walletBalanceText: String {
        viewModel.balanceText
    }

    private func requireLogin(action: () -> Void) {
        if viewModel.isLoggedIn {
            action()
        } else {
            toastMessage = copy.loginRequired
            showAuthFlow = true
        }
    }

    private func refreshCacheSize() {
        cacheSizeText = ByteCountFormatter.string(fromByteCount: cacheBytes(), countStyle: .file)
    }

    private func cacheBytes() -> Int64 {
        let fileManager = FileManager.default
        let directories = [
            fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first,
            fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        ].compactMap { $0 }

        var total: Int64 = 0
        for directory in directories {
            guard let enumerator = fileManager.enumerator(
                at: directory,
                includingPropertiesForKeys: [.isRegularFileKey, .fileSizeKey],
                options: [.skipsHiddenFiles]
            ) else { continue }

            for case let fileURL as URL in enumerator {
                guard
                    let values = try? fileURL.resourceValues(forKeys: [.isRegularFileKey, .fileSizeKey]),
                    values.isRegularFile == true
                else { continue }
                total += Int64(values.fileSize ?? 0)
            }
        }
        return total
    }
}

private struct BillingListView: View {
    @StateObject private var viewModel = BillingListViewModel()

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                if viewModel.isLoading {
                    ProgressView(copy.loading)
                        .padding(.vertical, 20)
                } else if viewModel.records.isEmpty {
                    Text(copy.empty)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .padding(.vertical, 20)
                } else {
                    ForEach(viewModel.records) { record in
                        NavigationLink(destination: BillingDetailView(record: record)) {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(record.title)
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(.white)
                                    Text(record.subtitle.isEmpty ? record.type : record.subtitle)
                                        .font(.system(size: 12))
                                        .foregroundStyle(Color(hex: 0xA0A0A0))
                                    if record.createdAtText.isEmpty == false {
                                        Text(record.createdAtText)
                                            .font(.system(size: 11))
                                            .foregroundStyle(Color(hex: 0xA0A0A0))
                                    }
                                }
                                Spacer()
                                Text(amountText(record.amount))
                                    .font(.system(size: 14, weight: .medium, design: .monospaced))
                                    .foregroundStyle(amountText(record.amount).hasPrefix("-") ? Color(hex: 0xEF5A5A) : Color(hex: 0x36C282))
                            }
                            .padding(.vertical, 16)
                        }
                        .buttonStyle(.plain)

                        if record.id != viewModel.records.last?.id {
                            Rectangle()
                                .fill(Color(hex: 0x2D2D31))
                                .frame(height: 1)
                        }
                    }
                }
            }
            .padding(24)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { viewModel.load() }
    }

    private func amountText(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value).doubleValue
        let text = String(format: "%.2f", number)
        return number >= 0 ? "+\(text)" : text
    }

    private var copy: BillingListCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

@MainActor
private final class BillingListViewModel: ObservableObject {
    @Published var records: [BillingRecord] = []
    @Published var isLoading = false

    private let service: BillingServiceProtocol = BillingService()

    func load() {
        Task {
            isLoading = true
            defer { isLoading = false }
            records = (try? await service.fetchBillingList(limit: 20)) ?? []
        }
    }
}

private struct ProfileCopy {
    let title: String
    let headerTitle: String
    let guestMode: String
    let accountIDFormat: String
    let accountPlaceholder: String
    let notLoggedIn: String
    let loginButton: String
    let loginRequired: String
    let walletTitle: String
    let billButton: String
    let howToEarnButton: String
    let referralTitle: String
    let referralSubtitle: String
    let referralButton: String
    let security: String
    let cloudSync: String
    let clearCache: String
    let language: String
    let syncEnabled: String
    let syncDisabled: String
    let cacheCleared: String
    let languageDialogTitle: String
    let cancel: String

    static func forLanguage(_ language: AppLanguage) -> ProfileCopy {
        switch language {
        case .simplifiedChinese:
            return ProfileCopy(
                title: "我的",
                headerTitle: "Nexus 玩家",
                guestMode: "游客模式",
                accountIDFormat: "账号ID：%@",
                accountPlaceholder: "账号ID",
                notLoggedIn: "未登录",
                loginButton: "登录账号",
                loginRequired: "请先登录",
                walletTitle: "钱包余额",
                billButton: "账单",
                howToEarnButton: "如何赚取",
                referralTitle: "邀请奖励",
                referralSubtitle: "查看邀请人数、奖励和分享链接",
                referralButton: "去查看",
                security: "账号安全",
                cloudSync: "云同步",
                clearCache: "清理缓存",
                language: "语言",
                syncEnabled: "已开启",
                syncDisabled: "已关闭",
                cacheCleared: "缓存已清理",
                languageDialogTitle: "选择语言",
                cancel: "取消"
            )
        case .traditionalChinese:
            return ProfileCopy(
                title: "我的",
                headerTitle: "Nexus 玩家",
                guestMode: "訪客模式",
                accountIDFormat: "帳號ID：%@",
                accountPlaceholder: "帳號ID",
                notLoggedIn: "未登入",
                loginButton: "登入帳號",
                loginRequired: "請先登入",
                walletTitle: "錢包餘額",
                billButton: "帳單",
                howToEarnButton: "如何賺取",
                referralTitle: "邀請獎勵",
                referralSubtitle: "查看邀請人數、獎勵和分享連結",
                referralButton: "前往查看",
                security: "帳號安全",
                cloudSync: "雲同步",
                clearCache: "清理快取",
                language: "語言",
                syncEnabled: "已開啟",
                syncDisabled: "已關閉",
                cacheCleared: "快取已清理",
                languageDialogTitle: "選擇語言",
                cancel: "取消"
            )
        case .english:
            return ProfileCopy(
                title: "Profile",
                headerTitle: "Nexus Player",
                guestMode: "Guest Mode",
                accountIDFormat: "Account ID: %@",
                accountPlaceholder: "Account ID",
                notLoggedIn: "Not signed in",
                loginButton: "Sign In",
                loginRequired: "Please sign in first",
                walletTitle: "Wallet Balance",
                billButton: "Billing",
                howToEarnButton: "How to Earn",
                referralTitle: "Referral Rewards",
                referralSubtitle: "View invite counts, rewards, and your share link",
                referralButton: "Open",
                security: "Account Security",
                cloudSync: "Cloud Sync",
                clearCache: "Clear Cache",
                language: "Language",
                syncEnabled: "On",
                syncDisabled: "Off",
                cacheCleared: "Cache cleared",
                languageDialogTitle: "Choose Language",
                cancel: "Cancel"
            )
        }
    }
}

private struct BillingListCopy {
    let title: String
    let loading: String
    let empty: String

    static func forLanguage(_ language: AppLanguage) -> BillingListCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "账单", loading: "加载中...", empty: "暂无账单记录")
        case .traditionalChinese:
            return .init(title: "帳單", loading: "載入中...", empty: "暫無帳單記錄")
        case .english:
            return .init(title: "Billing", loading: "Loading...", empty: "No billing records")
        }
    }
}
