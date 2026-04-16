import SwiftUI
import UIKit

@MainActor
struct AuthFlowView: View {
    enum Screen {
        case login
        case register
        case reset
    }

    let onAuthenticated: (AuthSession) -> Void

    @Environment(\.openURL) private var openURL
    @Environment(\.presentationMode) private var presentationMode
    @State private var screen: Screen = .login
    @State private var language: AppLanguage = AppLanguageStore.currentSync()
    @State private var email = ""
    @State private var password = ""
    @State private var passwordVisible = false
    @State private var verificationCode = ""
    @State private var confirmPassword = ""
    @State private var confirmPasswordVisible = false
    @State private var agreedToTerms = true
    @State private var isLoading = false
    @State private var message: String?
    @State private var codeCooldown = 0
    @State private var cooldownTask: Task<Void, Never>?
    @State private var legalLinks: LegalLinks?

    private let service: AuthServiceProtocol = AuthService()
    private let legalConfigService: LegalConfigServiceProtocol = LegalConfigService()

    var body: some View {
        let copy = AuthCopy.forLanguage(language)

        ZStack {
            Color(hex: 0x121212)
                .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    topBar(copy: copy)
                    titleBlock(copy: copy)
                    formBlock(copy: copy)
                    footerBlock(copy: copy)
                }
                .padding(.horizontal, 24)
                .padding(.top, 12)
                .padding(.bottom, 32)
            }
        }
        .task {
            language = await AppLanguageStore.shared.current()
            legalLinks = await legalConfigService.fetchLinks()
        }
        .onDisappear {
            cooldownTask?.cancel()
            cooldownTask = nil
        }
    }

    @ViewBuilder
    private func topBar(copy: AuthCopy) -> some View {
        HStack {
            if screen == .login {
                Spacer()
                Button {
                    presentationMode.wrappedValue.dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
            } else {
                Button {
                    withAnimation(.easeInOut(duration: 0.24)) {
                        screen = .login
                        message = nil
                    }
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
                Spacer()
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private func titleBlock(copy: AuthCopy) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(copy.title(for: screen))
                .font(.system(size: 34, weight: .black))
                .foregroundStyle(.white)

            Text(copy.subtitle(for: screen))
                .font(.system(size: 16))
                .foregroundStyle(Color(hex: 0xA0A0A0))
                .padding(.top, 8)
        }
        .padding(.bottom, 40)
    }

    @ViewBuilder
    private func formBlock(copy: AuthCopy) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            AuthInputField(
                title: screen == .reset ? copy.accountLabelForgot : copy.accountLabel,
                text: $email,
                keyboardType: .emailAddress
            )

            if screen == .register || screen == .reset {
                HStack(spacing: 12) {
                    AuthInputField(
                        title: copy.codeLabel(for: screen),
                        text: $verificationCode,
                        keyboardType: .numberPad
                    )

                    Button(codeButtonTitle(copy: copy)) {
                        message = nil
                        sendCode(copy: copy)
                    }
                    .disabled(isLoading || normalizedEmail.isEmpty || codeCooldown > 0)
                    .frame(width: 110, height: 56)
                    .background(Color.clear)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(Color(hex: 0x6B4EFF), lineWidth: 1)
                    )
                    .foregroundStyle(Color(hex: 0x6B4EFF))
                    .font(.system(size: 15, weight: .semibold))
                }
            }

            AuthInputField(
                title: copy.passwordLabel(for: screen),
                text: $password,
                isSecure: true,
                isSecureVisible: $passwordVisible
            )

            if screen == .register {
                VStack(alignment: .leading, spacing: 16) {
                    AuthInputField(
                        title: copy.confirmPasswordLabel,
                        text: $confirmPassword,
                        isSecure: true,
                        isSecureVisible: $confirmPasswordVisible
                    )

                    HStack(alignment: .top, spacing: 10) {
                        Button {
                            agreedToTerms.toggle()
                        } label: {
                            ZStack {
                                RoundedRectangle(cornerRadius: 4, style: .continuous)
                                    .fill(agreedToTerms ? Color(hex: 0x6B4EFF, alpha: 0.2) : .clear)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 4, style: .continuous)
                                            .stroke(
                                                agreedToTerms ? Color.clear : Color(hex: 0xA0A0A0, alpha: 0.3),
                                                lineWidth: 1
                                            )
                                    )
                                if agreedToTerms {
                                    Text("✓")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(Color(hex: 0x6B4EFF))
                                }
                            }
                            .frame(width: 18, height: 18)
                        }
                        .buttonStyle(.plain)

                        legalText(copy: copy)
                    }
                    .padding(.top, 2)
                }
            }

            if screen == .login {
                HStack {
                    Spacer()
                    Button(copy.forgotPassword) {
                        withAnimation(.easeInOut(duration: 0.24)) {
                            screen = .reset
                            message = nil
                        }
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Color(hex: 0x6B4EFF))
                    .font(.system(size: 15, weight: .medium))
                }
            }

            Button(isLoading ? copy.loading : copy.submitTitle(for: screen)) {
                message = nil
                submit(copy: copy)
            }
            .disabled(isLoading)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(hex: 0x6B4EFF))
            )
            .foregroundStyle(.white)
            .font(.system(size: 17, weight: .bold))
            .padding(.top, screen == .login ? 20 : 8)

            if let message, message.isEmpty == false {
                Text(message)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .padding(.top, 4)
            }
        }
    }

    @ViewBuilder
    private func footerBlock(copy: AuthCopy) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: 24)
            HStack(spacing: 6) {
                Text(copy.footerPrefix(for: screen))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                Text(copy.footerAction(for: screen))
                    .foregroundStyle(Color(hex: 0x6B4EFF))
                    .fontWeight(.bold)
                    .onTapGesture {
                        withAnimation(.easeInOut(duration: 0.24)) {
                            screen = copy.footerTarget(for: screen)
                            message = nil
                        }
                    }
            }
            .font(.system(size: 15))
            .frame(maxWidth: .infinity)
            .padding(.top, 24)
        }
    }

    private var normalizedEmail: String {
        email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var normalizedCode: String {
        verificationCode.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func codeButtonTitle(copy: AuthCopy) -> String {
        codeCooldown > 0 ? "\(codeCooldown)s" : copy.getCode
    }

    private func sendCode(copy: AuthCopy) {
        guard isLoading == false else { return }
        isLoading = true
        Task {
            await performSendCode(copy: copy)
        }
    }

    private func submit(copy: AuthCopy) {
        guard isLoading == false else { return }
        isLoading = true
        Task {
            await performSubmit(copy: copy)
        }
    }

    private func performSendCode(copy: AuthCopy) async {
        defer { isLoading = false }

        guard normalizedEmail.isEmpty == false else {
            message = copy.emailRequired
            return
        }
        guard normalizedEmail.contains("@") else {
            message = copy.invalidEmail
            return
        }

        do {
            let purpose = screen == .register ? "REGISTER" : "RESET_PASSWORD"
            let scene = screen == .register ? "AUTH_REGISTER" : "AUTH_FORGOT_PASSWORD"
            try await service.sendCode(email: normalizedEmail, purpose: purpose, scene: scene)
            message = copy.codeSent
            startCooldown(seconds: 60)
        } catch {
            message = error.localizedDescription
        }
    }

    private func performSubmit(copy: AuthCopy) async {
        defer { isLoading = false }

        guard normalizedEmail.isEmpty == false else {
            message = copy.emailRequired
            return
        }

        do {
            switch screen {
            case .login:
                guard password.isEmpty == false else {
                    message = copy.passwordRequired
                    return
                }
                let session = try await service.login(email: normalizedEmail, password: password)
                await AuthSessionStore.shared.save(session)
                onAuthenticated(session)
                presentationMode.wrappedValue.dismiss()
            case .register:
                guard normalizedEmail.contains("@") else {
                    message = copy.invalidEmail
                    return
                }
                guard password.isEmpty == false else {
                    message = copy.passwordRequired
                    return
                }
                guard normalizedCode.isEmpty == false else {
                    message = copy.codeRequired
                    return
                }
                guard password == confirmPassword else {
                    message = copy.passwordMismatch
                    return
                }
                guard agreedToTerms else {
                    message = copy.termsRequired
                    return
                }
                let session = try await service.register(
                    email: normalizedEmail,
                    password: password,
                    code: normalizedCode
                )
                await AuthSessionStore.shared.save(session)
                onAuthenticated(session)
                presentationMode.wrappedValue.dismiss()
            case .reset:
                guard normalizedEmail.contains("@") else {
                    message = copy.invalidEmail
                    return
                }
                guard normalizedCode.isEmpty == false else {
                    message = copy.codeRequired
                    return
                }
                guard password.isEmpty == false else {
                    message = copy.passwordRequired
                    return
                }
                try await service.resetPassword(
                    email: normalizedEmail,
                    code: normalizedCode,
                    newPassword: password
                )
                message = copy.resetSuccess
                screen = .login
                password = ""
                passwordVisible = false
                verificationCode = ""
                confirmPassword = ""
                confirmPasswordVisible = false
            }
        } catch {
            message = error.localizedDescription
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

    @ViewBuilder
    private func legalText(copy: AuthCopy) -> some View {
        HStack(spacing: 0) {
            Text(copy.termsPrefix)
                .foregroundColor(Color(hex: 0xA0A0A0))

            Button(copy.userAgreement) {
                guard let url = legalLinks?.termsURL else { return }
                openURL(url)
            }
            .buttonStyle(.plain)
            .foregroundStyle(Color(hex: 0x6B4EFF))

            Text(copy.termsConnector)
                .foregroundColor(Color(hex: 0xA0A0A0))

            Button(copy.privacyPolicy) {
                guard let url = legalLinks?.privacyURL else { return }
                openURL(url)
            }
            .buttonStyle(.plain)
            .foregroundStyle(Color(hex: 0x6B4EFF))
        }
        .font(.system(size: 13))
        .fixedSize(horizontal: false, vertical: true)
    }
}

private struct AuthInputField: View {
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
        .background(Color.clear)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(
                    LinearGradient(
                        colors: [Color(hex: 0x6B4EFF), Color(hex: 0x6B4EFF, alpha: 0.45)],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    lineWidth: 1
                )
                .opacity(0.9)
        )
    }
}

private struct AuthCopy {
    let loginTitle: String
    let loginSubtitle: String
    let registerTitle: String
    let registerSubtitle: String
    let resetTitle: String
    let resetSubtitle: String
    let accountLabel: String
    let accountLabelForgot: String
    let passwordLabel: String
    let newPasswordLabel: String
    let confirmPasswordLabel: String
    let registerCodeLabel: String
    let resetCodeLabel: String
    let forgotPassword: String
    let loginAction: String
    let registerAction: String
    let resetAction: String
    let getCode: String
    let loading: String
    let footerLoginPrefix: String
    let footerLoginAction: String
    let footerRegisterPrefix: String
    let footerRegisterAction: String
    let footerResetPrefix: String
    let footerResetAction: String
    let termsPrefix: String
    let userAgreement: String
    let termsConnector: String
    let privacyPolicy: String
    let emailRequired: String
    let invalidEmail: String
    let passwordRequired: String
    let codeRequired: String
    let termsRequired: String
    let passwordMismatch: String
    let codeSent: String
    let resetSuccess: String

    func title(for screen: AuthFlowView.Screen) -> String {
        switch screen {
        case .login: return loginTitle
        case .register: return registerTitle
        case .reset: return resetTitle
        }
    }

    func subtitle(for screen: AuthFlowView.Screen) -> String {
        switch screen {
        case .login: return loginSubtitle
        case .register: return registerSubtitle
        case .reset: return resetSubtitle
        }
    }

    func passwordLabel(for screen: AuthFlowView.Screen) -> String {
        screen == .reset ? newPasswordLabel : passwordLabel
    }

    func codeLabel(for screen: AuthFlowView.Screen) -> String {
        screen == .register ? registerCodeLabel : resetCodeLabel
    }

    func submitTitle(for screen: AuthFlowView.Screen) -> String {
        switch screen {
        case .login: return loginAction
        case .register: return registerAction
        case .reset: return resetAction
        }
    }

    func footerPrefix(for screen: AuthFlowView.Screen) -> String {
        switch screen {
        case .login: return footerLoginPrefix
        case .register: return footerRegisterPrefix
        case .reset: return footerResetPrefix
        }
    }

    func footerAction(for screen: AuthFlowView.Screen) -> String {
        switch screen {
        case .login: return footerLoginAction
        case .register: return footerRegisterAction
        case .reset: return footerResetAction
        }
    }

    func footerTarget(for screen: AuthFlowView.Screen) -> AuthFlowView.Screen {
        switch screen {
        case .login: return .register
        case .register, .reset: return .login
        }
    }

    static func forLanguage(_ language: AppLanguage) -> AuthCopy {
        switch language {
        case .simplifiedChinese:
            return AuthCopy(
                loginTitle: "欢迎回来",
                loginSubtitle: "登录账号，继续你的游戏旅程",
                registerTitle: "创建账号",
                registerSubtitle: "注册后即可同步你的游戏进度与资产",
                resetTitle: "找回密码",
                resetSubtitle: "输入邮箱并重置你的登录密码",
                accountLabel: "邮箱",
                accountLabelForgot: "邮箱账号",
                passwordLabel: "密码",
                newPasswordLabel: "新密码",
                confirmPasswordLabel: "确认密码",
                registerCodeLabel: "验证码",
                resetCodeLabel: "验证码",
                forgotPassword: "忘记密码？",
                loginAction: "登录",
                registerAction: "注册",
                resetAction: "重置密码",
                getCode: "获取验证码",
                loading: "处理中...",
                footerLoginPrefix: "新用户？",
                footerLoginAction: "立即注册",
                footerRegisterPrefix: "已有账号？",
                footerRegisterAction: "返回登录",
                footerResetPrefix: "已有账号？",
                footerResetAction: "返回登录",
                termsPrefix: "我已阅读并同意",
                userAgreement: "《用户协议》",
                termsConnector: "和",
                privacyPolicy: "《隐私政策》",
                emailRequired: "请输入邮箱",
                invalidEmail: "请输入正确的邮箱地址",
                passwordRequired: "请输入密码",
                codeRequired: "请输入验证码",
                termsRequired: "请先勾选用户协议和隐私政策",
                passwordMismatch: "两次输入的密码不一致",
                codeSent: "验证码已发送，请检查邮箱",
                resetSuccess: "密码已重置，请使用新密码登录"
            )
        case .traditionalChinese:
            return AuthCopy(
                loginTitle: "歡迎回來",
                loginSubtitle: "登入帳號，繼續你的遊戲旅程",
                registerTitle: "建立帳號",
                registerSubtitle: "註冊後即可同步你的遊戲進度與資產",
                resetTitle: "找回密碼",
                resetSubtitle: "輸入信箱並重設你的登入密碼",
                accountLabel: "電子郵件",
                accountLabelForgot: "電子郵件帳號",
                passwordLabel: "密碼",
                newPasswordLabel: "新密碼",
                confirmPasswordLabel: "確認密碼",
                registerCodeLabel: "驗證碼",
                resetCodeLabel: "驗證碼",
                forgotPassword: "忘記密碼？",
                loginAction: "登入",
                registerAction: "註冊",
                resetAction: "重設密碼",
                getCode: "取得驗證碼",
                loading: "處理中...",
                footerLoginPrefix: "新用戶？",
                footerLoginAction: "立即註冊",
                footerRegisterPrefix: "已有帳號？",
                footerRegisterAction: "返回登入",
                footerResetPrefix: "已有帳號？",
                footerResetAction: "返回登入",
                termsPrefix: "我已閱讀並同意",
                userAgreement: "《使用者協議》",
                termsConnector: "和",
                privacyPolicy: "《隱私政策》",
                emailRequired: "請輸入電子郵件",
                invalidEmail: "請輸入正確的電子郵件地址",
                passwordRequired: "請輸入密碼",
                codeRequired: "請輸入驗證碼",
                termsRequired: "請先勾選使用者協議和隱私政策",
                passwordMismatch: "兩次輸入的密碼不一致",
                codeSent: "驗證碼已發送，請檢查信箱",
                resetSuccess: "密碼已重設，請使用新密碼登入"
            )
        case .english:
            return AuthCopy(
                loginTitle: "Welcome Back",
                loginSubtitle: "Sign in and jump back into your games",
                registerTitle: "Create Account",
                registerSubtitle: "Register to sync your progress and game assets",
                resetTitle: "Reset Password",
                resetSubtitle: "Enter your email and set a new password",
                accountLabel: "Email",
                accountLabelForgot: "Email",
                passwordLabel: "Password",
                newPasswordLabel: "New Password",
                confirmPasswordLabel: "Confirm Password",
                registerCodeLabel: "Verification Code",
                resetCodeLabel: "Verification Code",
                forgotPassword: "Forgot password?",
                loginAction: "Log In",
                registerAction: "Sign Up",
                resetAction: "Reset Password",
                getCode: "Get Code",
                loading: "Processing...",
                footerLoginPrefix: "New here?",
                footerLoginAction: "Create account",
                footerRegisterPrefix: "Already have an account?",
                footerRegisterAction: "Back to login",
                footerResetPrefix: "Already have an account?",
                footerResetAction: "Back to login",
                termsPrefix: "I have read and agree to ",
                userAgreement: "User Agreement",
                termsConnector: " and ",
                privacyPolicy: "Privacy Policy",
                emailRequired: "Please enter your email",
                invalidEmail: "Please enter a valid email address",
                passwordRequired: "Please enter your password",
                codeRequired: "Please enter the verification code",
                termsRequired: "Please agree to the user agreement and privacy policy first",
                passwordMismatch: "The passwords do not match",
                codeSent: "Verification code sent. Please check your inbox",
                resetSuccess: "Password reset complete. Please sign in again"
            )
        }
    }
}
