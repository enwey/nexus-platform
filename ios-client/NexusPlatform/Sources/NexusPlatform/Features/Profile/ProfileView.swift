import SwiftUI

struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()
    @State private var hasLoaded = false
    @State private var showAuthFlow = false
    @State private var showLanguageDialog = false
    @State private var cacheSizeText = "0 B"
    @State private var language: AppLanguage = AppLanguageStore.currentSync()
    @State private var toastMessage: String?
    @State private var showBilling = false
    @State private var showReferral = false
    @State private var showHowToEarn = false
    @State private var showSecurity = false
    private let storage = VersionedGameStorageManager()

    var body: some View {
        ScrollView(showsIndicators: false) {
            ZStack(alignment: .top) {
                if viewModel.isLoading {
                    profileSkeleton
                        .transition(NativeMotion.stateSwapTransition)
                } else {
                    VStack(spacing: 24) {
                        userCard
                        walletCard
                        referralCard
                        menuGroup
                    }
                    .transition(NativeMotion.contentRevealTransition)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
            .padding(.leading, 24)
            .padding(.top, 24)
            .padding(.trailing, 24)
            .padding(.bottom, 24)
        }
        .scrollBounceBehavior(.always)
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear {
            guard hasLoaded == false else { return }
            hasLoaded = true
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
        .overlay(alignment: .bottom) {
            NativeToastOverlay(message: $toastMessage)
        }
        .overlay {
            NativeCenteredModal(isPresented: $showLanguageDialog) {
                languageDialogContent
            }
        }
        .navigationDestination(isPresented: $showBilling) {
            BillingListView()
        }
        .navigationDestination(isPresented: $showReferral) {
            ReferralView()
        }
        .navigationDestination(isPresented: $showHowToEarn) {
            HowToEarnView(
                language: viewModel.selectedLanguage,
                isLoggedIn: viewModel.isLoggedIn,
                onRequestLogin: { showAuthFlow = true }
            )
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
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading)
    }

    private var profileSkeleton: some View {
        VStack(spacing: 24) {
            HStack(alignment: .center, spacing: 20) {
                NativeSkeletonBlock(width: 84, height: 84, cornerRadius: 42)
                VStack(alignment: .leading, spacing: 10) {
                    NativeSkeletonBlock(width: 164, height: 32, cornerRadius: 10)
                    NativeSkeletonBlock(width: 92, height: 19, cornerRadius: 8)
                }
                Spacer()
                NativeSkeletonBlock(width: 78, height: 32, cornerRadius: 16)
            }

            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x24253A), Color(hex: 0x1C1C1F)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(height: 164)
                .overlay {
                    VStack(alignment: .leading, spacing: 0) {
                        NativeSkeletonBlock(width: 72, height: 12, cornerRadius: 6)
                        Spacer().frame(height: 8)
                        NativeSkeletonBlock(width: 142, height: 34, cornerRadius: 10)
                        Spacer()
                        HStack(spacing: 12) {
                            NativeSkeletonBlock(height: 38, cornerRadius: 12)
                            NativeSkeletonBlock(height: 38, cornerRadius: 12)
                        }
                    }
                    .padding(20)
                }

            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color(hex: 0x1C1C1F))
                .frame(height: 96)
                .overlay {
                    HStack(spacing: 16) {
                        NativeSkeletonBlock(width: 34, height: 34, cornerRadius: 17)
                        VStack(alignment: .leading, spacing: 10) {
                            NativeSkeletonBlock(width: 116, height: 16, cornerRadius: 7)
                            NativeSkeletonBlock(width: 174, height: 13, cornerRadius: 6)
                        }
                        Spacer()
                        NativeSkeletonBlock(width: 70, height: 30, cornerRadius: 15)
                    }
                    .padding(20)
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(Color(hex: 0x6B4EFF), lineWidth: 1)
                )

            VStack(spacing: 0) {
                ForEach(0..<4, id: \.self) { index in
                    HStack {
                        NativeSkeletonBlock(width: index == 0 ? 72 : 96, height: 16, cornerRadius: 7)
                        Spacer()
                        NativeSkeletonBlock(width: index == 0 ? 14 : 58, height: 13, cornerRadius: 6)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 18)

                    if index != 3 {
                        divider
                    }
                }
            }
            .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
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
                    .fill(Color.white.opacity(0.08))
                    .frame(width: 84, height: 84)

                BrandLogoImage(size: 76, cornerRadius: 38)
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .stroke(Color.white.opacity(0.12), lineWidth: 1)
                    )
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
                    showHowToEarn = true
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
                showSecurity = true
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
                    let clearedBytes = await storage.cacheSizeInBytes()
                    try? await storage.clearAllLocalCaches()
                    await MainActor.run {
                        cacheSizeText = ByteCountFormatter.string(fromByteCount: 0, countStyle: .file)
                        let clearedText = ByteCountFormatter.string(fromByteCount: clearedBytes, countStyle: .file)
                        toastMessage = "\(copy.cacheCleared) (\(clearedText))"
                    }
                    refreshCacheSize()
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
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.vertical, 18)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var languageDialogContent: some View {
        VStack(spacing: 18) {
            Text(copy.languageDialogTitle)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .center)

            VStack(spacing: 10) {
                ForEach(AppLanguage.allCases) { item in
                    Button {
                        viewModel.setLanguage(item)
                        showLanguageDialog = false
                    } label: {
                        HStack {
                            Text(item.title)
                                .font(.system(size: 15, weight: .medium))
                                .foregroundStyle(.white)

                            Spacer()

                            if item == viewModel.selectedLanguage {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundStyle(Color(hex: 0x6B4EFF))
                            }
                        }
                        .frame(maxWidth: .infinity, minHeight: 52, alignment: .leading)
                        .padding(.horizontal, 16)
                        .background(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(Color(hex: 0x232326))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(item == viewModel.selectedLanguage ? Color(hex: 0x6B4EFF) : Color.clear, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }

            Button(copy.cancel) {
                showLanguageDialog = false
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(Color(hex: 0xA0A0A0))
        }
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
        Task {
            let bytes = await storage.cacheSizeInBytes()
            await MainActor.run {
                cacheSizeText = ByteCountFormatter.string(fromByteCount: bytes, countStyle: .file)
            }
        }
    }
}

private struct HowToEarnView: View {
    let language: AppLanguage
    let isLoggedIn: Bool
    let onRequestLogin: () -> Void

    @State private var showReferral = false

    private var copy: HowToEarnCopy {
        .forLanguage(language)
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                VStack(spacing: 16) {
                    Circle()
                        .fill(Color.white.opacity(0.08))
                        .frame(width: 64, height: 64)
                        .overlay(
                            Text(copy.inviterBadge)
                                .font(.system(size: 28))
                        )

                    Text(copy.landingInviteLine)
                        .font(.system(size: 16))
                        .foregroundStyle(.white)

                    Text(copy.heroTitle)
                        .font(.system(size: 28, weight: .black))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 24)
                .padding(.top, 36)
                .padding(.bottom, 24)

                landingGiftCard

                VStack(alignment: .leading, spacing: 18) {
                    Text(copy.previewTitle)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color(hex: 0xD2D4DB))
                        .frame(maxWidth: .infinity, alignment: .center)

                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3), spacing: 12) {
                        ForEach(0..<6, id: \.self) { _ in
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [Color(hex: 0x2A2F4F), Color(hex: 0x181A24)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .aspectRatio(1, contentMode: .fit)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                        .stroke(Color.white.opacity(0.06), lineWidth: 1)
                                )
                        }
                    }

                    Button {
                        if isLoggedIn {
                            showReferral = true
                        } else {
                            onRequestLogin()
                        }
                    } label: {
                        Text(isLoggedIn ? copy.ctaLoggedIn : copy.ctaLoggedOut)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 24)
                .padding(.top, 28)
                .padding(.bottom, 96)
            }
        }
        .background(
            LinearGradient(
                colors: [Color(hex: 0x2B1C46), Color.black],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .navigationDestination(isPresented: $showReferral) {
            ReferralView()
        }
    }

    private var landingGiftCard: some View {
        VStack(spacing: 0) {
            VStack(spacing: 8) {
                Text(copy.giftBadge)
                    .font(.system(size: 44))

                Text(copy.giftAmount)
                    .font(.system(size: 46, weight: .black))
                    .foregroundStyle(.white)

                Text(copy.giftSubtitle)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .multilineTextAlignment(.center)
            }
            .padding(.top, 30)

            Spacer().frame(height: 24)

            VStack(alignment: .leading, spacing: 10) {
                Text(copy.stepsTitle)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)

                howToEarnStep(number: "1", title: copy.stepOneTitle, detail: copy.stepOneDetail)
                howToEarnStep(number: "2", title: copy.stepTwoTitle, detail: copy.stepTwoDetail)
                howToEarnStep(number: "3", title: copy.stepThreeTitle, detail: copy.stepThreeDetail)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x222433), Color(hex: 0x15161D)],
                startPoint: .top,
                endPoint: .bottom
            ),
            in: RoundedRectangle(cornerRadius: 24, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: 0x6B4EFF, alpha: 0.24), lineWidth: 1)
        )
        .padding(.horizontal, 24)
    }

    private func howToEarnStep(number: String, title: String, detail: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(number)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(Color(hex: 0x6B4EFF))
                .frame(width: 22, height: 22)
                .background(Color.white.opacity(0.05), in: Circle())
                .overlay(
                    Circle()
                        .stroke(Color(hex: 0x6B4EFF), lineWidth: 1)
                )

            Text("\(title)：\(detail)")
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: 0xA0A0A0))
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct BillingListView: View {
    @StateObject private var viewModel = BillingListViewModel()

    var body: some View {
        ScrollView(showsIndicators: false) {
            ZStack(alignment: .topLeading) {
                if viewModel.isLoading && viewModel.records.isEmpty {
                    billingListSkeleton
                        .transition(NativeMotion.stateSwapTransition)
                } else if viewModel.records.isEmpty {
                    NativeStateCard {
                        Text(copy.empty)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: 0xA0A0A0))
                    }
                    .padding(.vertical, 20)
                    .transition(NativeMotion.stateSwapTransition)
                } else {
                    VStack(alignment: .leading, spacing: 24) {
                        HStack(spacing: 12) {
                            summaryMetricCard(
                                title: copy.incomeTitle,
                                value: amountText(monthlyIncome),
                                valueColor: Color(hex: 0x36C282)
                            )
                            summaryMetricCard(
                                title: copy.expenseTitle,
                                value: amountText(monthlyExpense),
                                valueColor: .white
                            )
                        }

                        Text(copy.recentTitle)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: 0xA0A0A0))

                        VStack(spacing: 0) {
                            ForEach(viewModel.records) { record in
                                NavigationLink(destination: BillingDetailView(record: record)) {
                                    billingRow(record)
                                }
                                .padding(.vertical, 16)
                                .buttonStyle(.plain)

                                if record.id != viewModel.records.last?.id {
                                    Rectangle()
                                        .fill(Color(hex: 0x2D2D31))
                                        .frame(height: 1)
                                }
                            }
                        }

                        Text(copy.recentFooter)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: 0x4B4B50))
                            .frame(maxWidth: .infinity, alignment: .center)
                    }
                    .transition(NativeMotion.contentRevealTransition)
                }

                if viewModel.isLoading && viewModel.records.isEmpty == false {
                    NativeSectionRefreshOverlay(lineWidths: [90, 60], cornerRadius: 18)
                        .padding(.top, 10)
                }
            }
            .padding(24)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .onAppear { viewModel.load() }
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading)
        .animation(NativeMotion.overlayTransition, value: viewModel.records.isEmpty)
    }

    private func amountText(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value).doubleValue
        let text = String(format: "%.0f", abs(number))
        return number >= 0 ? "+\(text)" : "-\(text)"
    }

    private var copy: BillingListCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    private var billingListSkeleton: some View {
        VStack(alignment: .leading, spacing: 24) {
            HStack(spacing: 12) {
                NativeSkeletonBlock(height: 84, cornerRadius: 16)
                NativeSkeletonBlock(height: 84, cornerRadius: 16)
            }

            NativeSkeletonBlock(width: 72, height: 13, cornerRadius: 6)

            ForEach(0..<5, id: \.self) { index in
                HStack(alignment: .top) {
                    NativeSkeletonBlock(width: 44, height: 44, cornerRadius: 12)
                    VStack(alignment: .leading, spacing: 6) {
                        NativeSkeletonBlock(width: 142, height: 15, cornerRadius: 7)
                        NativeSkeletonBlock(width: 182, height: 12, cornerRadius: 6)
                    }
                    Spacer()
                    NativeSkeletonBlock(width: 58, height: 15, cornerRadius: 7)
                }
                .padding(.vertical, 16)

                if index != 4 {
                    Rectangle()
                        .fill(Color(hex: 0x2D2D31))
                        .frame(height: 1)
                }
            }
        }
    }

    private var monthlyIncome: Decimal {
        viewModel.records.reduce(Decimal.zero) { partial, record in
            record.amount > 0 ? partial + record.amount : partial
        }
    }

    private var monthlyExpense: Decimal {
        viewModel.records.reduce(Decimal.zero) { partial, record in
            record.amount < 0 ? partial + abs(record.amount) : partial
        }
    }

    private func summaryMetricCard(title: String, value: String, valueColor: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0xA0A0A0))
            Text(value)
                .font(.system(size: 18, weight: .bold, design: .monospaced))
                .foregroundStyle(valueColor)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private func billingRow(_ record: BillingRecord) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(billingAccentColor(record).opacity(0.14))
                    .frame(width: 44, height: 44)
                Text(billingIcon(record))
                    .font(.system(size: 20))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(record.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                Text(record.subtitle.isEmpty ? record.type : record.subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .lineLimit(1)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text(amountText(record.amount))
                    .font(.system(size: 16, weight: .bold, design: .monospaced))
                    .foregroundStyle(record.amount < 0 ? .white : Color(hex: 0x36C282))
                Text(record.createdAtText)
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
            }
        }
    }

    private func billingIcon(_ record: BillingRecord) -> String {
        let value = "\(record.title) \(record.subtitle) \(record.type)".lowercased()
        if value.contains("邀请") || value.contains("invite") {
            return "🤝"
        }
        if value.contains("购买") || value.contains("buy") {
            return "🎮"
        }
        if value.contains("试玩") || value.contains("reward") {
            return "🕹️"
        }
        if value.contains("兑换") || value.contains("prop") {
            return "💎"
        }
        return record.amount >= 0 ? "🪙" : "🧾"
    }

    private func billingAccentColor(_ record: BillingRecord) -> Color {
        record.amount >= 0 ? Color(hex: 0x36C282) : Color(hex: 0x6B4EFF)
    }
}

@MainActor
private final class BillingListViewModel: ObservableObject {
    private static var cachedRecords: [BillingRecord] = []

    @Published var records: [BillingRecord] = []
    @Published var isLoading = false

    private let service: BillingServiceProtocol = BillingService()

    func load() {
        if records.isEmpty, Self.cachedRecords.isEmpty == false {
            records = Self.cachedRecords
        }

        Task {
            isLoading = true
            defer { isLoading = false }
            if let loaded = try? await service.fetchBillingList(limit: 20) {
                records = loaded
                Self.cachedRecords = loaded
            } else if records.isEmpty {
                records = []
            }
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
                headerTitle: "BringBox 玩家",
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
                headerTitle: "BringBox 玩家",
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
                headerTitle: "BringBox Player",
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
    let refreshing: String
    let incomeTitle: String
    let expenseTitle: String
    let recentTitle: String
    let recentFooter: String

    static func forLanguage(_ language: AppLanguage) -> BillingListCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "账单明细", loading: "加载中...", empty: "暂无账单记录", refreshing: "正在刷新", incomeTitle: "本月累计收入", expenseTitle: "本月累计支出", recentTitle: "最近记录", recentFooter: "仅显示最近 30 天记录")
        case .traditionalChinese:
            return .init(title: "帳單明細", loading: "載入中...", empty: "暫無帳單記錄", refreshing: "正在刷新", incomeTitle: "本月累計收入", expenseTitle: "本月累計支出", recentTitle: "最近記錄", recentFooter: "僅顯示最近 30 天記錄")
        case .english:
            return .init(title: "Billing", loading: "Loading...", empty: "No billing records", refreshing: "Refreshing", incomeTitle: "Monthly Income", expenseTitle: "Monthly Expense", recentTitle: "Recent Records", recentFooter: "Only the latest 30 days are shown")
        }
    }
}

private struct HowToEarnCopy {
    let title: String
    let heroTitle: String
    let heroSubtitle: String
    let landingInviteLine: String
    let inviterBadge: String
    let giftBadge: String
    let giftAmount: String
    let giftSubtitle: String
    let previewTitle: String
    let stepsTitle: String
    let stepOneTitle: String
    let stepOneDetail: String
    let stepTwoTitle: String
    let stepTwoDetail: String
    let stepThreeTitle: String
    let stepThreeDetail: String
    let ctaLoggedIn: String
    let ctaLoggedOut: String

    static func forLanguage(_ language: AppLanguage) -> HowToEarnCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "如何赚取",
                heroTitle: "送你一个新人专属礼包",
                heroSubtitle: "把邀请链接分享给好友，好友完成注册或达到活动条件后，奖励会自动发到你的钱包。",
                landingInviteLine: "你的好友送你一个礼包",
                inviterBadge: "🎁",
                giftBadge: "🪙",
                giftAmount: "500",
                giftSubtitle: "平台币已就绪，完成登录后即可查看邀请奖励",
                previewTitle: "在 Nexus 玩这些热门游戏",
                stepsTitle: "赚取方式",
                stepOneTitle: "获取邀请链接",
                stepOneDetail: "进入邀请奖励页面，复制或分享你的专属邀请链接。",
                stepTwoTitle: "好友完成邀请",
                stepTwoDetail: "好友通过你的链接进入并完成平台要求的注册或活动。",
                stepThreeTitle: "奖励自动到账",
                stepThreeDetail: "奖励会自动累计到钱包余额里，你可以随时在邀请奖励页面查看明细。",
                ctaLoggedIn: "查看邀请奖励",
                ctaLoggedOut: "登录后查看邀请奖励"
            )
        case .traditionalChinese:
            return .init(
                title: "如何賺取",
                heroTitle: "送你一個新人專屬禮包",
                heroSubtitle: "將邀請連結分享給好友，好友完成註冊或達成活動條件後，獎勵會自動發到你的錢包。",
                landingInviteLine: "你的好友送你一個禮包",
                inviterBadge: "🎁",
                giftBadge: "🪙",
                giftAmount: "500",
                giftSubtitle: "平台幣已就緒，完成登入後即可查看邀請獎勵",
                previewTitle: "在 Nexus 玩這些熱門遊戲",
                stepsTitle: "賺取方式",
                stepOneTitle: "取得邀請連結",
                stepOneDetail: "進入邀請獎勵頁面，複製或分享你的專屬邀請連結。",
                stepTwoTitle: "好友完成邀請",
                stepTwoDetail: "好友透過你的連結進入並完成平台要求的註冊或活動。",
                stepThreeTitle: "獎勵自動到帳",
                stepThreeDetail: "獎勵會自動累計到錢包餘額中，你可以隨時在邀請獎勵頁面查看明細。",
                ctaLoggedIn: "查看邀請獎勵",
                ctaLoggedOut: "登入後查看邀請獎勵"
            )
        case .english:
            return .init(
                title: "How to Earn",
                heroTitle: "A welcome gift is waiting for you",
                heroSubtitle: "Share your referral link with friends. Once they complete the required signup or campaign action, the reward is added to your wallet automatically.",
                landingInviteLine: "A friend sent you a gift",
                inviterBadge: "🎁",
                giftBadge: "🪙",
                giftAmount: "500",
                giftSubtitle: "Coins are ready. Sign in to view referral rewards.",
                previewTitle: "Popular games on Nexus",
                stepsTitle: "How it works",
                stepOneTitle: "Get your referral link",
                stepOneDetail: "Open the referral rewards page and copy or share your personal link.",
                stepTwoTitle: "Friends complete the referral",
                stepTwoDetail: "Friends join through your link and complete the required signup or activity.",
                stepThreeTitle: "Rewards arrive automatically",
                stepThreeDetail: "Rewards accumulate in your wallet balance, and you can review them anytime on the referral rewards page.",
                ctaLoggedIn: "View Referral Rewards",
                ctaLoggedOut: "Sign In to View Referral Rewards"
            )
        }
    }
}
