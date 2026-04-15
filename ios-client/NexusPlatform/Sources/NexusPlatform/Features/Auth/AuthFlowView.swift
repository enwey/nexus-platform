import SwiftUI

struct AuthFlowView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case login = "登录"
        case register = "注册"
        case reset = "忘记密码"

        var id: String { rawValue }
    }

    let onAuthenticated: (AuthSession) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var mode: Mode = .login
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var confirmPassword: String = ""
    @State private var code: String = ""
    @State private var isLoading = false
    @State private var message: String?
    @State private var codeCooldown = 0
    @State private var cooldownTask: Task<Void, Never>?

    private let service: AuthServiceProtocol = AuthService()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Picker("模式", selection: $mode) {
                    ForEach(Mode.allCases) { item in
                        Text(item.rawValue).tag(item)
                    }
                }
                .pickerStyle(.segmented)

                Group {
                    TextField("邮箱", text: $email)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                    if mode == .login || mode == .register || mode == .reset {
                        SecureField(mode == .reset ? "新密码" : "密码", text: $password)
                    }
                    if mode == .register {
                        SecureField("确认密码", text: $confirmPassword)
                    }
                    if mode == .register || mode == .reset {
                        HStack {
                            TextField("验证码", text: $code)
                                .keyboardType(.numberPad)
                            Button(codeCooldown > 0 ? "重发 \(codeCooldown)s" : "发送验证码") {
                                sendCode()
                            }
                            .disabled(isLoading || normalizedEmail.isEmpty || codeCooldown > 0)
                        }
                    }
                }
                .padding(10)
                .background(AppTheme.ColorToken.surfaceSecondary, in: RoundedRectangle(cornerRadius: 10, style: .continuous))

                Button(isLoading ? "处理中..." : submitTitle) {
                    submit()
                }
                .disabled(isLoading)
                .nexusPrimaryCTA()

                if let message {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                }
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("账号")
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear {
            cooldownTask?.cancel()
            cooldownTask = nil
        }
    }

    private var submitTitle: String {
        switch mode {
        case .login:
            return "邮箱登录"
        case .register:
            return "注册并登录"
        case .reset:
            return "重置密码"
        }
    }

    private var normalizedEmail: String {
        email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func sendCode() {
        Task {
            isLoading = true
            defer { isLoading = false }
            do {
                let purpose = mode == .register ? "REGISTER" : "RESET"
                try await service.sendCode(email: normalizedEmail, purpose: purpose)
                message = "验证码已发送，请检查邮箱"
                startCooldown(seconds: 60)
            } catch {
                message = error.localizedDescription
            }
        }
    }

    private func submit() {
        Task {
            guard normalizedEmail.isEmpty == false else {
                message = "请输入邮箱"
                return
            }
            isLoading = true
            defer { isLoading = false }

            do {
                switch mode {
                case .login:
                    guard password.isEmpty == false else {
                        message = "请输入密码"
                        return
                    }
                    let session = try await service.login(email: normalizedEmail, password: password)
                    await AuthSessionStore.shared.save(session)
                    await MainActor.run {
                        onAuthenticated(session)
                        dismiss()
                    }
                case .register:
                    guard password.isEmpty == false, code.isEmpty == false else {
                        message = "请填写完整信息"
                        return
                    }
                    guard password == confirmPassword else {
                        message = "两次输入的密码不一致"
                        return
                    }
                    let session = try await service.register(email: normalizedEmail, password: password, code: code)
                    await AuthSessionStore.shared.save(session)
                    await MainActor.run {
                        onAuthenticated(session)
                        dismiss()
                    }
                case .reset:
                    guard password.isEmpty == false, code.isEmpty == false else {
                        message = "请填写新密码和验证码"
                        return
                    }
                    try await service.resetPassword(email: normalizedEmail, code: code, newPassword: password)
                    await MainActor.run {
                        message = "密码已重置，请使用新密码登录"
                        mode = .login
                        self.password = ""
                        self.code = ""
                        self.confirmPassword = ""
                    }
                }
            } catch {
                message = error.localizedDescription
            }
        }
    }

    private func startCooldown(seconds: Int) {
        cooldownTask?.cancel()
        codeCooldown = max(seconds, 0)
        cooldownTask = Task {
            var remaining = max(seconds, 0)
            while remaining > 0, Task.isCancelled == false {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                remaining -= 1
                await MainActor.run {
                    codeCooldown = max(0, remaining)
                }
            }
        }
    }
}
