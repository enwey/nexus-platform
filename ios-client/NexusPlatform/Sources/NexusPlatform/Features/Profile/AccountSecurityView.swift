import SwiftUI

struct AccountSecurityView: View {
    let email: String
    let onSavedLanguage: (() -> Void)?
    let cloudSyncEnabled: Bool
    let onCloudSyncToggle: (Bool) -> Void
    let selectedLanguage: AppLanguage
    let onLanguageSelect: (AppLanguage) -> Void

    @State private var biometricEnabled = true
    @State private var code = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var codeCooldown = 0
    @State private var message: String?

    private let authService: AuthServiceProtocol = AuthService()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("账号安全")
                        .font(.headline)
                    Toggle("生物识别", isOn: $biometricEnabled)
                    Toggle("云同步", isOn: Binding(
                        get: { cloudSyncEnabled },
                        set: { onCloudSyncToggle($0) }
                    ))
                }
                .padding(16)
                .nexusGlassCard()

                VStack(alignment: .leading, spacing: 10) {
                    Text("语言")
                        .font(.headline)
                    ForEach(AppLanguage.allCases) { language in
                        Button {
                            onLanguageSelect(language)
                            onSavedLanguage?()
                        } label: {
                            HStack {
                                Text(language.title)
                                Spacer()
                                if language == selectedLanguage {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                        .foregroundStyle(AppTheme.ColorToken.textPrimary)
                        if language != AppLanguage.allCases.last {
                            Divider()
                        }
                    }
                }
                .padding(16)
                .nexusGlassCard()

                VStack(alignment: .leading, spacing: 10) {
                    Text("修改密码")
                        .font(.headline)
                    Text(email)
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    HStack {
                        TextField("验证码", text: $code)
                            .keyboardType(.numberPad)
                        Button(codeCooldown > 0 ? "重发 \(codeCooldown)s" : "发送验证码") {
                            sendCode()
                        }
                        .disabled(codeCooldown > 0 || email.isEmpty)
                    }
                    SecureField("新密码", text: $password)
                    SecureField("确认新密码", text: $confirmPassword)
                    Button("更新密码") {
                        submit()
                    }
                    .nexusPrimaryCTA()

                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    }
                }
                .padding(16)
                .nexusGlassCard()
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("账号安全")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func sendCode() {
        Task {
            do {
                try await authService.sendCode(email: email, purpose: "CHANGE_PASSWORD")
                message = "验证码已发送"
                startCooldown()
            } catch {
                message = error.localizedDescription
            }
        }
    }

    private func submit() {
        Task {
            guard code.isEmpty == false, password.isEmpty == false else {
                message = "请填写完整信息"
                return
            }
            guard password == confirmPassword else {
                message = "两次输入的密码不一致"
                return
            }
            do {
                try await authService.resetPassword(email: email, code: code, newPassword: password)
                message = "密码已更新"
                code = ""
                password = ""
                confirmPassword = ""
            } catch {
                message = error.localizedDescription
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
}
