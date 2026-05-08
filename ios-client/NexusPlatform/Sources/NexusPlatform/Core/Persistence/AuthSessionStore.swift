import Foundation
import LocalAuthentication
import Security
import SwiftUI

struct AuthSession: Codable, Sendable {
    let accessToken: String
    let refreshToken: String
    let email: String
}

actor AuthSessionStore {
    static let shared = AuthSessionStore()

    private let defaults = UserDefaults.standard
    private let sessionKey = "nexus.auth.session"
    private let keychainAccount = "nexus.auth.session.keychain"
    private let keychainService = "com.nexusplatform.apple.host.auth"

    func save(_ session: AuthSession) {
        if let data = try? JSONEncoder().encode(session) {
            _ = storeKeychainValue(data)
            defaults.removeObject(forKey: sessionKey)
        }
    }

    func current() -> AuthSession? {
        if let data = loadKeychainValue(),
           let session = try? JSONDecoder().decode(AuthSession.self, from: data) {
            return session
        }

        if let legacyData = defaults.data(forKey: sessionKey),
           let session = try? JSONDecoder().decode(AuthSession.self, from: legacyData) {
            save(session)
            defaults.removeObject(forKey: sessionKey)
            removeLegacyTokenMirrors()
            return session
        }

        return nil
    }

    func clear() {
        removeKeychainValue()
        defaults.removeObject(forKey: sessionKey)
        removeLegacyTokenMirrors()
    }

    private func keychainQuery() -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount
        ]
    }

    private func loadKeychainValue() -> Data? {
        var query = keychainQuery()
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess else { return nil }
        return item as? Data
    }

    @discardableResult
    private func storeKeychainValue(_ data: Data) -> Bool {
        var query = keychainQuery()
        let attributes = [kSecValueData as String: data]
        let updateStatus = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if updateStatus == errSecSuccess {
            return true
        }

        query[kSecValueData as String] = data
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        let addStatus = SecItemAdd(query as CFDictionary, nil)
        return addStatus == errSecSuccess
    }

    private func removeKeychainValue() {
        SecItemDelete(keychainQuery() as CFDictionary)
    }

    private func removeLegacyTokenMirrors() {
        defaults.removeObject(forKey: "access_token")
        defaults.removeObject(forKey: "token")
        defaults.removeObject(forKey: "auth_token")
        defaults.removeObject(forKey: "authorization")
    }
}

enum BiometricKind: Sendable {
    case none
    case faceID
    case touchID

    func title(for language: AppLanguage) -> String {
        switch self {
        case .none:
            switch language {
            case .simplifiedChinese: return "生物识别"
            case .traditionalChinese: return "生物辨識"
            case .english: return "Biometric Login"
            }
        case .faceID:
            return "Face ID"
        case .touchID:
            return "Touch ID"
        }
    }
}

struct BiometricAvailability: Sendable {
    let supported: Bool
    let kind: BiometricKind
}

enum BiometricToggleResult: Sendable {
    case enabled
    case disabled
    case failed(String)
}

actor BiometricPreferenceStore {
    static let shared = BiometricPreferenceStore()

    private let defaults = UserDefaults.standard
    private let enabledKey = "nexus.security.biometric.enabled"

    func isEnabled() -> Bool {
        defaults.bool(forKey: enabledKey)
    }

    func setEnabled(_ enabled: Bool) {
        defaults.set(enabled, forKey: enabledKey)
    }
}

struct BiometricAuthService {
    func availability() -> BiometricAvailability {
        let context = LAContext()
        var error: NSError?
        let supported = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        let kind: BiometricKind
        switch context.biometryType {
        case .faceID:
            kind = .faceID
        case .touchID:
            kind = .touchID
        default:
            kind = .none
        }
        return BiometricAvailability(supported: supported, kind: supported ? kind : .none)
    }

    func authenticate(reason: String) async -> Bool {
        await withCheckedContinuation { continuation in
            let context = LAContext()
            context.localizedCancelTitle = "Cancel"
            var error: NSError?
            guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
                continuation.resume(returning: false)
                return
            }
            context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, _ in
                continuation.resume(returning: success)
            }
        }
    }
}

@MainActor
final class BiometricAuthController: ObservableObject {
    static let shared = BiometricAuthController()

    @Published private(set) var isEnabled = false
    @Published private(set) var availability = BiometricAvailability(supported: false, kind: .none)
    @Published private(set) var isLocked = false
    @Published private(set) var isAuthenticating = false

    private let preferences: BiometricPreferenceStore
    private let service: BiometricAuthService
    private var hasLoaded = false

    init(
        preferences: BiometricPreferenceStore = .shared,
        service: BiometricAuthService = BiometricAuthService()
    ) {
        self.preferences = preferences
        self.service = service
    }

    func loadIfNeeded() {
        guard hasLoaded == false else { return }
        hasLoaded = true
        availability = service.availability()
        Task {
            let enabled = await preferences.isEnabled()
            let hasSession = await AuthSessionStore.shared.current() != nil
            await MainActor.run {
                isEnabled = enabled && availability.supported
                isLocked = enabled && availability.supported && hasSession
            }
            if enabled && availability.supported == false {
                await preferences.setEnabled(false)
            }
        }
    }

    func refreshAvailability() {
        availability = service.availability()
        if availability.supported == false && isEnabled {
            Task {
                await preferences.setEnabled(false)
                await MainActor.run {
                    isEnabled = false
                    isLocked = false
                }
            }
        }
    }

    func updateEnabled(_ enabled: Bool, language: AppLanguage) async -> BiometricToggleResult {
        refreshAvailability()

        if enabled == false {
            await preferences.setEnabled(false)
            isEnabled = false
            isLocked = false
            return .disabled
        }

        guard availability.supported else {
            await preferences.setEnabled(false)
            isEnabled = false
            return .failed(AppText.biometricUnavailable(language))
        }

        let success = await authenticate(reason: AppText.biometricEnableReason(language))
        guard success else {
            await preferences.setEnabled(false)
            isEnabled = false
            return .failed(AppText.biometricEnableFailed(language))
        }

        await preferences.setEnabled(true)
        isEnabled = true
        isLocked = false
        return .enabled
    }

    func handleScenePhase(_ phase: ScenePhase) {
        guard isEnabled else { return }
        switch phase {
        case .inactive, .background:
            Task {
                if await AuthSessionStore.shared.current() != nil {
                    await MainActor.run {
                        isLocked = true
                    }
                }
            }
        case .active:
            Task {
                await unlockIfNeeded(reason: AppText.biometricUnlockReason(AppLanguageStore.currentSync()))
            }
        @unknown default:
            break
        }
    }

    func unlockIfNeeded(reason: String) async {
        guard isEnabled, isLocked, isAuthenticating == false else { return }
        guard await AuthSessionStore.shared.current() != nil else {
            isLocked = false
            return
        }
        let success = await authenticate(reason: reason)
        if success {
            isLocked = false
        }
    }

    func unlock(language: AppLanguage) {
        Task {
            await unlockIfNeeded(reason: AppText.biometricUnlockReason(language))
        }
    }

    private func authenticate(reason: String) async -> Bool {
        isAuthenticating = true
        defer { isAuthenticating = false }
        return await service.authenticate(reason: reason)
    }
}
