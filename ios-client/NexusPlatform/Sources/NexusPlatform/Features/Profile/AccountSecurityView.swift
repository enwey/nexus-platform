import SwiftUI
import UIKit

struct AccountSecurityView: View {
    let email: String
    let onSavedLanguage: (() -> Void)?
    let cloudSyncEnabled: Bool
    let onCloudSyncToggle: (Bool) -> Void
    let selectedLanguage: AppLanguage
    let onLanguageSelect: (AppLanguage) -> Void
    let onRequestLogin: (() -> Void)?
    let onLogoutCurrent: (() -> Void)?

    @Environment(\.dismiss) private var dismiss
    @State private var biometricEnabled = true
    @State private var showLogoutConfirm = false
    @State private var showChangePassword = false
    @State private var showDeviceManagement = false
    @State private var showAccountTermination = false
    @State private var toastMessage: String?

    private var copy: AccountSecurityCopy {
        .forLanguage(selectedLanguage)
    }

    private var isLoggedIn: Bool {
        email.isEmpty == false
    }

    var body: some View {
        VStack(spacing: 20) {
            menuCard

            if isLoggedIn == false {
                primaryButton(title: copy.loginButton, color: Color(hex: 0x6B4EFF)) {
                    onRequestLogin?()
                }
            } else {
                primaryButton(title: copy.logoutButton, color: Color(hex: 0xE5484D)) {
                    showLogoutConfirm = true
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding(.horizontal, 24)
        .padding(.top, 12)
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
        .confirmationDialog(copy.logoutButton, isPresented: $showLogoutConfirm, titleVisibility: .visible) {
            Button(copy.confirm, role: .destructive) {
                onLogoutCurrent?()
                toastMessage = copy.logoutButton
                dismiss()
            }
            Button(copy.cancel, role: .cancel) {}
        } message: {
            Text(copy.logoutConfirm)
        }
        .navigationDestination(isPresented: $showChangePassword) {
            ChangePasswordView(email: email, language: selectedLanguage)
        }
        .navigationDestination(isPresented: $showDeviceManagement) {
            DeviceManagementView(language: selectedLanguage)
        }
        .navigationDestination(isPresented: $showAccountTermination) {
            AccountTerminationView(language: selectedLanguage)
        }
        .overlay(alignment: .bottom) {
            NativeToastOverlay(message: $toastMessage)
        }
    }

    private var menuCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(copy.loginSection)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white)
                .padding(.bottom, 12)

            accountMenuItem(icon: "🔐", title: copy.changePassword, subtitle: copy.changePasswordSubtitle, showChevron: true) {
                showChangePassword = true
            }

            accountMenuItem(
                icon: "🧬",
                title: copy.biometric,
                subtitle: copy.biometricSubtitle,
                showSwitch: true,
                switchBinding: $biometricEnabled
            )

            divider
                .padding(.vertical, 20)

            Text(copy.deviceSection)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white)
                .padding(.bottom, 12)

            accountMenuItem(icon: "📋", title: copy.deviceManagement, subtitle: copy.deviceManagementSubtitle, showChevron: true) {
                showDeviceManagement = true
            }

            accountMenuItem(icon: "🗑️", title: copy.accountTermination, subtitle: copy.accountTerminationSubtitle, showChevron: true) {
                showAccountTermination = true
            }
        }
        .padding(20)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private func accountMenuItem(
        icon: String,
        title: String,
        subtitle: String,
        showChevron: Bool = false,
        showSwitch: Bool = false,
        switchBinding: Binding<Bool> = .constant(false),
        action: @escaping () -> Void = {}
    ) -> some View {
        Button {
            if showSwitch == false {
                action()
            }
        } label: {
            HStack(alignment: .center, spacing: 14) {
                Text(icon)
                    .font(.system(size: 22))

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .multilineTextAlignment(.leading)
                }

                Spacer()

                if showSwitch {
                    Toggle("", isOn: switchBinding)
                        .labelsHidden()
                        .tint(Color(hex: 0x6B4EFF))
                } else if showChevron {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                }
            }
            .padding(.vertical, 14)
        }
        .buttonStyle(.plain)
    }

    private var divider: some View {
        Rectangle()
            .fill(Color(hex: 0x2D2D31).opacity(0.35))
            .frame(height: 1)
    }

    private func primaryButton(title: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(color, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct ChangePasswordView: View {
    let email: String
    let language: AppLanguage

    @Environment(\.dismiss) private var dismiss
    @State private var code = ""
    @State private var password = ""
    @State private var passwordVisible = false
    @State private var confirmPassword = ""
    @State private var confirmPasswordVisible = false
    @State private var codeCooldown = 0
    @State private var message: String?

    private let authService: AuthServiceProtocol = AuthService()

    private var copy: ChangePasswordCopy {
        .forLanguage(language)
    }

    var body: some View {
        VStack(spacing: 0) {
            simpleHeader(title: copy.title)

            VStack(alignment: .leading, spacing: 16) {
                Text(email)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: 0xA0A0A0))

                HStack(spacing: 12) {
                    SecurityInputField(
                        title: copy.code,
                        text: $code,
                        keyboardType: .numberPad
                    )

                    Button(codeCooldown > 0 ? "\(codeCooldown)s" : copy.sendCode) {
                        sendCode()
                    }
                    .frame(width: 110, height: 56)
                    .background(Color.clear)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(Color(hex: 0x6B4EFF), lineWidth: 1)
                    )
                    .foregroundStyle(Color(hex: 0x6B4EFF))
                    .font(.system(size: 15, weight: .semibold))
                    .disabled(codeCooldown > 0 || email.isEmpty)
                }

                SecurityInputField(
                    title: copy.newPassword,
                    text: $password,
                    isSecure: true,
                    isSecureVisible: $passwordVisible
                )

                SecurityInputField(
                    title: copy.confirmPassword,
                    text: $confirmPassword,
                    isSecure: true,
                    isSecureVisible: $confirmPasswordVisible
                )

                Button(copy.update) {
                    submit()
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .foregroundStyle(.white)
                .font(.system(size: 17, weight: .bold))

                if let message {
                    Text(message)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                }
            }
            .padding(24)

            Spacer()
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
    }

    private func sendCode() {
        Task {
            do {
                try await authService.sendCode(
                    email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
                    purpose: "CHANGE_PASSWORD",
                    source: "ios-client",
                    scene: "PROFILE_CHANGE_PASSWORD"
                )
                await MainActor.run {
                    message = copy.codeSent
                    startCooldown()
                }
            } catch {
                await MainActor.run {
                    message = error.localizedDescription
                }
            }
        }
    }

    private func submit() {
        Task {
            guard code.isEmpty == false, password.isEmpty == false else {
                await MainActor.run {
                    message = copy.incomplete
                }
                return
            }
            guard password.count >= 8 else {
                await MainActor.run {
                    message = copy.passwordTooShort
                }
                return
            }
            guard password == confirmPassword else {
                await MainActor.run {
                    message = copy.passwordMismatch
                }
                return
            }
            do {
                try await authService.resetPassword(email: email, code: code, newPassword: password)
                await MainActor.run {
                    message = copy.success
                    code = ""
                    password = ""
                    passwordVisible = false
                    confirmPassword = ""
                    confirmPasswordVisible = false
                }
            } catch {
                await MainActor.run {
                    message = error.localizedDescription
                }
            }
        }
    }

    private func startCooldown() {
        codeCooldown = 60
        Task {
            while codeCooldown > 0 {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                codeCooldown -= 1
            }
        }
    }

    private func simpleHeader(title: String) -> some View {
        HStack(spacing: 0) {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
            }
            .buttonStyle(NativeNavigationButtonStyle())

            Text(title)
                .font(.system(size: 28, weight: .heavy))
                .foregroundStyle(.white)

            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
    }
}

private struct DeviceManagementView: View {
    let language: AppLanguage

    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = DeviceManagementViewModel()

    private var copy: DeviceManagementCopy {
        .forLanguage(language)
    }

    var body: some View {
        VStack(spacing: 0) {
            header

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    if viewModel.devices.isEmpty {
                        Text(copy.empty)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: 0xA0A0A0))
                    } else {
                        ForEach(viewModel.devices.indices, id: \.self) { index in
                            let device = viewModel.devices[index]
                            DeviceRow(device: device, copy: copy)
                            if index != viewModel.devices.indices.last {
                                Spacer()
                                    .frame(height: 8)
                            }
                        }
                    }
                }
                .padding(20)
                .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                )
                .padding(.horizontal, 24)
            }
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
        .onAppear { viewModel.load() }
    }

    private var header: some View {
        HStack(spacing: 0) {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
            }
            .buttonStyle(NativeNavigationButtonStyle())

            Text(copy.title)
                .font(.system(size: 28, weight: .heavy))
                .foregroundStyle(.white)

            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
    }
}

@MainActor
private final class DeviceManagementViewModel: ObservableObject {
    @Published var devices: [DeviceSession] = []
    private let service: DeviceSessionServiceProtocol = DeviceSessionService()

    func load() {
        Task {
            devices = (try? await service.fetchDevices()) ?? []
        }
    }
}

private struct AccountTerminationView: View {
    let language: AppLanguage

    @Environment(\.dismiss) private var dismiss
    @State private var confirmText = ""
    @State private var message: String?
    @State private var isSubmitting = false
    @State private var countdown = 10

    private let authService: AuthServiceProtocol = AuthService()
    private var copy: AccountTerminationCopy {
        .forLanguage(language)
    }

    private var enabled: Bool {
        confirmText == copy.confirmTarget && countdown == 0
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                }
                .buttonStyle(NativeNavigationButtonStyle())

                Text(copy.title)
                    .font(.system(size: 28, weight: .heavy))
                    .foregroundStyle(.white)

                Spacer()
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 12)

            VStack(alignment: .leading, spacing: 0) {
                warningCard

                Spacer()
                    .frame(height: 20)

                riskCard

                Spacer()
                    .frame(height: 24)

                Button(countdown > 0 ? "\(copy.submit) (\(countdown)s)" : (isSubmitting ? copy.processing : copy.submit)) {
                    submit()
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(enabled ? LinearGradient(colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)], startPoint: .leading, endPoint: .trailing) : LinearGradient(colors: [Color(hex: 0xD1D5DB), Color(hex: 0xD1D5DB)], startPoint: .leading, endPoint: .trailing))
                )
                .foregroundStyle(enabled ? Color(hex: 0x121212) : Color(hex: 0x666666))
                .font(.system(size: 15, weight: .semibold))

                if let message {
                    Text(message)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .padding(.top, 12)
                }

                Text(copy.footer)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 12)
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
        .task {
            while countdown > 0 {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                countdown -= 1
            }
        }
    }

    private func submit() {
        Task {
            guard enabled else {
                await MainActor.run {
                    message = countdown > 0 ? String(format: copy.waitingHint, countdown) : String(format: copy.inputHint, copy.confirmTarget)
                }
                return
            }
            guard let session = await AuthSessionStore.shared.current() else {
                await MainActor.run { message = copy.notLoggedIn }
                return
            }
            isSubmitting = true
            defer { isSubmitting = false }
            do {
                try await authService.terminateAccount(accessToken: session.accessToken, confirmText: confirmText)
                await AuthSessionStore.shared.clear()
                await MainActor.run {
                    message = copy.success
                }
            } catch {
                await MainActor.run {
                    message = error.localizedDescription
                }
            }
        }
    }

    private var warningCard: some View {
        HStack(alignment: .center, spacing: 12) {
            Text("⚠")
                .font(.system(size: 28))
            VStack(alignment: .leading, spacing: 4) {
                Text(copy.warningTitle)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(Color(hex: 0xFF7875))
                Text(copy.warning)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .lineSpacing(4)
            }
        }
        .padding(20)
        .background(Color(hex: 0xFF4D4F, alpha: 0.08), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0xFF4D4F, alpha: 0.4), lineWidth: 1)
        )
    }

    private var riskCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            RiskItemView(icon: "🎮", title: copy.risk1Title, description: copy.risk1Description)
            RiskItemView(icon: "💰", title: copy.risk2Title, description: copy.risk2Description)
            RiskItemView(icon: "👥", title: copy.risk3Title, description: copy.risk3Description)
            Spacer()
                .frame(height: 8)
            Text(copy.confirmLabel)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0xA0A0A0))
            Spacer()
                .frame(height: 8)
            SecurityInputField(title: copy.confirmPlaceholder, text: $confirmText)
        }
        .padding(20)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }
}

private struct AccountSecurityCopy {
    let title: String
    let loginSection: String
    let changePassword: String
    let changePasswordSubtitle: String
    let biometric: String
    let biometricSubtitle: String
    let deviceSection: String
    let deviceManagement: String
    let deviceManagementSubtitle: String
    let accountTermination: String
    let accountTerminationSubtitle: String
    let loginButton: String
    let logoutButton: String
    let logoutConfirm: String
    let cancel: String
    let confirm: String

    static func forLanguage(_ language: AppLanguage) -> AccountSecurityCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "账号安全",
                loginSection: "登录与验证",
                changePassword: "修改密码",
                changePasswordSubtitle: "更新当前账号的登录密码",
                biometric: "生物识别",
                biometricSubtitle: "使用 Face ID 或 Touch ID 快速验证",
                deviceSection: "设备与账号",
                deviceManagement: "设备管理",
                deviceManagementSubtitle: "查看当前已登录设备",
                accountTermination: "注销账号",
                accountTerminationSubtitle: "该操作不可恢复，请谨慎操作",
                loginButton: "登录账号",
                logoutButton: "退出登录",
                logoutConfirm: "确认退出当前账号？",
                cancel: "取消",
                confirm: "确认"
            )
        case .traditionalChinese:
            return .init(
                title: "帳號安全",
                loginSection: "登入與驗證",
                changePassword: "修改密碼",
                changePasswordSubtitle: "更新目前帳號的登入密碼",
                biometric: "生物辨識",
                biometricSubtitle: "使用 Face ID 或 Touch ID 快速驗證",
                deviceSection: "裝置與帳號",
                deviceManagement: "裝置管理",
                deviceManagementSubtitle: "查看目前已登入裝置",
                accountTermination: "註銷帳號",
                accountTerminationSubtitle: "此操作不可恢復，請謹慎處理",
                loginButton: "登入帳號",
                logoutButton: "登出",
                logoutConfirm: "確認退出目前帳號？",
                cancel: "取消",
                confirm: "確認"
            )
        case .english:
            return .init(
                title: "Account Security",
                loginSection: "Login & Verification",
                changePassword: "Change Password",
                changePasswordSubtitle: "Update the password for this account",
                biometric: "Biometric Login",
                biometricSubtitle: "Use Face ID or Touch ID for quick verification",
                deviceSection: "Devices & Account",
                deviceManagement: "Device Management",
                deviceManagementSubtitle: "See devices currently signed in",
                accountTermination: "Delete Account",
                accountTerminationSubtitle: "This action cannot be undone",
                loginButton: "Sign In",
                logoutButton: "Log Out",
                logoutConfirm: "Log out of the current account?",
                cancel: "Cancel",
                confirm: "Confirm"
            )
        }
    }
}

private struct ChangePasswordCopy {
    let title: String
    let code: String
    let sendCode: String
    let newPassword: String
    let confirmPassword: String
    let update: String
    let codeSent: String
    let incomplete: String
    let passwordTooShort: String
    let passwordMismatch: String
    let success: String

    static func forLanguage(_ language: AppLanguage) -> ChangePasswordCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "修改密码", code: "验证码", sendCode: "发送验证码", newPassword: "新密码", confirmPassword: "确认新密码", update: "更新密码", codeSent: "验证码已发送", incomplete: "请填写完整信息", passwordTooShort: "密码长度不能少于 8 位", passwordMismatch: "两次输入的密码不一致", success: "密码已更新")
        case .traditionalChinese:
            return .init(title: "修改密碼", code: "驗證碼", sendCode: "發送驗證碼", newPassword: "新密碼", confirmPassword: "確認新密碼", update: "更新密碼", codeSent: "驗證碼已發送", incomplete: "請填寫完整資訊", passwordTooShort: "密碼長度不能少於 8 位", passwordMismatch: "兩次輸入的密碼不一致", success: "密碼已更新")
        case .english:
            return .init(title: "Change Password", code: "Code", sendCode: "Send Code", newPassword: "New Password", confirmPassword: "Confirm Password", update: "Update Password", codeSent: "Code sent", incomplete: "Please complete all fields", passwordTooShort: "Password must be at least 8 characters", passwordMismatch: "Passwords do not match", success: "Password updated")
        }
    }
}

private struct DeviceManagementCopy {
    let title: String
    let empty: String
    let unknown: String
    let current: String
    let offline: String

    static func forLanguage(_ language: AppLanguage) -> DeviceManagementCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "设备管理", empty: "暂无设备记录", unknown: "未知设备", current: "当前", offline: "离线")
        case .traditionalChinese:
            return .init(title: "裝置管理", empty: "暫無裝置記錄", unknown: "未知裝置", current: "目前", offline: "離線")
        case .english:
            return .init(title: "Device Management", empty: "No device records", unknown: "Unknown Device", current: "Current", offline: "Offline")
        }
    }
}

private struct AccountTerminationCopy {
    let title: String
    let warningTitle: String
    let warning: String
    let risk1Title: String
    let risk1Description: String
    let risk2Title: String
    let risk2Description: String
    let risk3Title: String
    let risk3Description: String
    let confirmLabel: String
    let confirmPlaceholder: String
    let confirmTarget: String
    let submit: String
    let processing: String
    let waitingHint: String
    let inputHint: String
    let footer: String
    let success: String
    let notLoggedIn: String

    static func forLanguage(_ language: AppLanguage) -> AccountTerminationCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "注销账号",
                warningTitle: "高风险操作提醒",
                warning: "注销账号后将永久清空账号数据，且无法恢复。",
                risk1Title: "游戏进度将被清除",
                risk1Description: "当前账号下的游戏存档、进度和成就将无法找回。",
                risk2Title: "余额与权益将失效",
                risk2Description: "钱包余额、奖励和相关权益将在注销后失效。",
                risk3Title: "关联关系将解除",
                risk3Description: "邀请记录、设备登录状态与账号绑定信息将一并移除。",
                confirmLabel: "请输入确认文本",
                confirmPlaceholder: "请输入确认文本",
                confirmTarget: "确认注销",
                submit: "确认注销",
                processing: "处理中...",
                waitingHint: "请等待 %d 秒后再试",
                inputHint: "请输入「%@」后继续",
                footer: "注销后无法恢复，请再次确认。",
                success: "账号已注销",
                notLoggedIn: "请先登录"
            )
        case .traditionalChinese:
            return .init(
                title: "註銷帳號",
                warningTitle: "高風險操作提醒",
                warning: "註銷後將永久清空帳號資料，且無法恢復。",
                risk1Title: "遊戲進度將被清除",
                risk1Description: "目前帳號下的遊戲存檔、進度與成就將無法找回。",
                risk2Title: "餘額與權益將失效",
                risk2Description: "錢包餘額、獎勵與相關權益將在註銷後失效。",
                risk3Title: "關聯資料將解除",
                risk3Description: "邀請記錄、裝置登入狀態與帳號綁定資訊將一併移除。",
                confirmLabel: "請輸入確認文字",
                confirmPlaceholder: "請輸入確認文字",
                confirmTarget: "確認註銷",
                submit: "確認註銷",
                processing: "處理中...",
                waitingHint: "請等待 %d 秒後再試",
                inputHint: "請輸入「%@」後繼續",
                footer: "註銷後無法恢復，請再次確認。",
                success: "帳號已註銷",
                notLoggedIn: "請先登入"
            )
        case .english:
            return .init(
                title: "Delete Account",
                warningTitle: "High-Risk Action",
                warning: "Deleting your account will permanently remove account data and cannot be undone.",
                risk1Title: "Game progress will be removed",
                risk1Description: "Saved games, progress, and achievements under this account will be lost.",
                risk2Title: "Balance and benefits will expire",
                risk2Description: "Wallet balance, rewards, and related benefits will no longer be available.",
                risk3Title: "Linked data will be cleared",
                risk3Description: "Invites, device sessions, and account bindings will be removed.",
                confirmLabel: "Enter the confirmation text",
                confirmPlaceholder: "Enter the confirmation text",
                confirmTarget: "DELETE ACCOUNT",
                submit: "Delete Account",
                processing: "Processing...",
                waitingHint: "Please wait %d seconds",
                inputHint: "Enter \"%@\" to continue",
                footer: "This action cannot be undone.",
                success: "Account deleted",
                notLoggedIn: "Please sign in first"
            )
        }
    }
}

private struct DeviceRow: View {
    let device: DeviceSession
    let copy: DeviceManagementCopy

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color(hex: 0x232326))
                    .frame(width: 48, height: 48)
                Text("💻")
                    .font(.system(size: 22))
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(device.deviceName.isEmpty ? copy.unknown : device.deviceName)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(.white)
                Text("\(device.model)  \(device.ip)")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                Text(device.lastActiveAt)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
            }
            Spacer()
            Text(device.current ? copy.current : copy.offline)
                .font(.system(size: 12))
                .foregroundStyle(device.current ? Color(hex: 0x6B4EFF) : Color(hex: 0xA0A0A0))
        }
        .padding(.vertical, 12)
    }
}

private struct RiskItemView: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color(hex: 0xFF4D4F, alpha: 0.08))
                    .frame(width: 36, height: 36)
                Text(icon)
                    .font(.system(size: 18))
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Color(hex: 0xFF7875))
                Text(description)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .lineSpacing(4)
            }
        }
        .padding(.vertical, 12)
    }
}

private struct SecurityInputField: View {
    let title: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default
    var isSecure = false
    @Binding var isSecureVisible: Bool

    init(
        title: String,
        text: Binding<String>,
        keyboardType: UIKeyboardType = .default,
        isSecure: Bool = false,
        isSecureVisible: Binding<Bool> = .constant(false)
    ) {
        self.title = title
        _text = text
        self.keyboardType = keyboardType
        self.isSecure = isSecure
        _isSecureVisible = isSecureVisible
    }

    var body: some View {
        HStack(spacing: 8) {
            Group {
                if isSecure, isSecureVisible == false {
                    SecureField(title, text: $text)
                } else {
                    TextField(title, text: $text)
                }
            }
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .keyboardType(keyboardType)
            .foregroundStyle(.white)
            .tint(Color(hex: 0x6B4EFF))

            if isSecure {
                Button {
                    isSecureVisible.toggle()
                } label: {
                    Image(systemName: isSecureVisible ? "eye.slash" : "eye")
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                }
                .buttonStyle(.plain)
            }
        }
        .font(.system(size: 16))
        .padding(.horizontal, 16)
        .frame(height: 56)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color(hex: 0x6B4EFF), lineWidth: 1)
        )
    }
}
