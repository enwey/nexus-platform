import Foundation

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published private(set) var userID: Int64?
    @Published private(set) var currentEmail: String?
    @Published private(set) var displayName: String?
    @Published private(set) var isLoading = false
    @Published private(set) var message: String?
    @Published private(set) var walletSummary: WalletSummary?
    @Published private(set) var isWalletLoading = false
    @Published private(set) var billingRecords: [BillingRecord] = []
    @Published private(set) var isBillingLoading = false
    @Published private(set) var isTerminating = false
    @Published private(set) var devices: [DeviceSession] = []
    @Published private(set) var isDevicesLoading = false
    @Published private(set) var cloudSyncEnabled = false
    @Published private(set) var selectedLanguage: AppLanguage = .simplifiedChinese

    private let authService: AuthServiceProtocol
    private let authStore: AuthSessionStore
    private let walletService: WalletServiceProtocol
    private let billingService: BillingServiceProtocol
    private let deviceService: DeviceSessionServiceProtocol
    private let profileService: UserProfileServiceProtocol
    private let homeService: LibraryHomeServiceProtocol
    private let cloudSyncStore: CloudSyncStore
    private let languageStore: AppLanguageStore

    init(
        authService: AuthServiceProtocol = AuthService(),
        authStore: AuthSessionStore = .shared,
        walletService: WalletServiceProtocol = WalletService(),
        billingService: BillingServiceProtocol = BillingService(),
        deviceService: DeviceSessionServiceProtocol = DeviceSessionService(),
        profileService: UserProfileServiceProtocol = UserProfileService(),
        homeService: LibraryHomeServiceProtocol = LibraryHomeService(),
        cloudSyncStore: CloudSyncStore = .shared,
        languageStore: AppLanguageStore = .shared
    ) {
        self.authService = authService
        self.authStore = authStore
        self.walletService = walletService
        self.billingService = billingService
        self.deviceService = deviceService
        self.profileService = profileService
        self.homeService = homeService
        self.cloudSyncStore = cloudSyncStore
        self.languageStore = languageStore
    }

    func loadSession() {
        Task {
            isLoading = true
            defer { isLoading = false }
            selectedLanguage = await languageStore.current()
            cloudSyncEnabled = await cloudSyncStore.isEnabled()
            let session = await authStore.current()
            currentEmail = session?.email
            if session != nil {
                if let profile = try? await profileService.fetchProfile() {
                    userID = profile.id
                    displayName = profile.displayName.isEmpty ? nil : profile.displayName
                }
                await loadWallet()
                await loadBilling(limit: 10)
                await loadDevices()
            } else {
                userID = nil
                displayName = nil
                walletSummary = nil
                billingRecords = []
                devices = []
            }
        }
    }

    var isLoggedIn: Bool {
        currentEmail?.isEmpty == false
    }

    func logout() {
        Task {
            if let session = await authStore.current() {
                try? await authService.logout(accessToken: session.accessToken)
            }
            await authStore.clear()
            currentEmail = nil
            userID = nil
            displayName = nil
            walletSummary = nil
            billingRecords = []
            devices = []
            message = AppText.loggedOut(selectedLanguage)
        }
    }

    func applyAuthenticatedSession(_ session: AuthSession) {
        Task {
            await authStore.save(session)
            currentEmail = session.email
            message = AppText.loginSucceeded(selectedLanguage)
            if let profile = try? await profileService.fetchProfile() {
                userID = profile.id
                displayName = profile.displayName.isEmpty ? nil : profile.displayName
            }
            await loadWallet()
            await loadBilling(limit: 10)
            await loadDevices()
        }
    }

    func loadWalletManually() {
        Task { await loadWallet() }
    }

    func loadBillingManually() {
        Task { await loadBilling(limit: 20) }
    }

    func terminateAccount() {
        Task {
            guard let session = await authStore.current() else {
                message = AppText.pleaseLogin(selectedLanguage)
                return
            }
            isTerminating = true
            defer { isTerminating = false }

            do {
                try await authService.terminateAccount(accessToken: session.accessToken, confirmText: "确认注销")
                await authStore.clear()
                currentEmail = nil
                userID = nil
                displayName = nil
                walletSummary = nil
                billingRecords = []
                devices = []
                message = AppText.accountTerminated(selectedLanguage)
            } catch {
                message = error.localizedDescription
            }
        }
    }

    func loadDevicesManually() {
        Task { await loadDevices() }
    }

    func kickDevice(_ deviceID: String) {
        Task {
            do {
                try await deviceService.kick(deviceID: deviceID)
                message = AppText.deviceKicked(selectedLanguage)
                await loadDevices()
            } catch {
                message = error.localizedDescription
            }
        }
    }

    func logoutAllDevices() {
        Task {
            do {
                try await deviceService.logoutAll()
                message = AppText.otherDevicesLoggedOut(selectedLanguage)
                await loadDevices()
            } catch {
                message = error.localizedDescription
            }
        }
    }

    func setLanguage(_ language: AppLanguage) {
        Task {
            await languageStore.set(language)
            await MainActor.run {
                selectedLanguage = language
                message = AppText.languageUpdated(language)
            }
        }
    }

    func setCloudSyncEnabled(_ enabled: Bool) {
        Task {
            await cloudSyncStore.setEnabled(enabled)
            if enabled, let home = try? await homeService.fetchHome() {
                await GameEngagementStore.shared.applyCloudState(
                    currentPlayingGameID: home.currentPlayingGame?.id,
                    recentGameIDs: home.recentGames.map(\.id),
                    favoriteGameIDs: home.myGames.map(\.id)
                )
            }
            await MainActor.run {
                cloudSyncEnabled = enabled
                message = enabled ? AppText.cloudSyncEnabled(selectedLanguage) : AppText.cloudSyncDisabled(selectedLanguage)
            }
        }
    }

    var balanceText: String {
        formatAmount(walletSummary?.balance ?? 0)
    }

    var availableBalanceText: String {
        formatAmount(walletSummary?.availableBalance ?? 0)
    }

    private func loadWallet() async {
        isWalletLoading = true
        defer { isWalletLoading = false }
        do {
            walletSummary = try await walletService.fetchSummary()
        } catch {
            message = error.localizedDescription
            if case WalletServiceError.unauthorized = error {
                walletSummary = nil
            }
        }
    }

    private func loadBilling(limit: Int) async {
        isBillingLoading = true
        defer { isBillingLoading = false }
        do {
            billingRecords = try await billingService.fetchBillingList(limit: limit)
        } catch {
            message = error.localizedDescription
            if case BillingServiceError.unauthorized = error {
                billingRecords = []
            }
        }
    }

    private func loadDevices() async {
        isDevicesLoading = true
        defer { isDevicesLoading = false }
        do {
            devices = try await deviceService.fetchDevices()
        } catch {
            message = error.localizedDescription
            if case DeviceSessionServiceError.unauthorized = error {
                devices = []
            }
        }
    }

    private func formatAmount(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value)
        return String(format: "%.2f", number.doubleValue)
    }

    func amountText(_ value: Decimal) -> String {
        let base = formatAmount(value)
        let numeric = NSDecimalNumber(decimal: value).doubleValue
        return numeric >= 0 ? "+\(base)" : base
    }
}
