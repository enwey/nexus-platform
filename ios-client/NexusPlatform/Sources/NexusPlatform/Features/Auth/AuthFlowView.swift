import SwiftUI
import UIKit

@MainActor
struct AuthFlowView: View {
    enum Screen {
        case login
        case register
        case reset
    }

    private enum NavigationDirection {
        case forward
        case backward
    }

    private struct LoginFormState {
        var email = ""
        var password = ""
        var passwordVisible = false
        var message: String?
    }

    private struct RegisterFormState {
        var email = ""
        var verificationCode = ""
        var password = ""
        var passwordVisible = false
        var confirmPassword = ""
        var confirmPasswordVisible = false
        var agreedToTerms = true
        var message: String?
        var codeCooldown = 0
    }

    private struct ResetFormState {
        var email = ""
        var verificationCode = ""
        var password = ""
        var passwordVisible = false
        var message: String?
        var codeCooldown = 0
    }

    let onAuthenticated: (AuthSession) -> Void

    @Environment(\.openURL) private var openURL
    @Environment(\.dismiss) private var dismiss
    @State private var screen: Screen = .login
    @State private var language: AppLanguage = AppLanguageStore.currentSync()
    @State private var navigationDirection: NavigationDirection = .forward
    @State private var loginForm = LoginFormState()
    @State private var registerForm = RegisterFormState()
    @State private var resetForm = ResetFormState()
    @State private var isLoading = false
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
                screenView(copy: copy)
                    .id(screen)
                    .transition(screenTransition)
                    .padding(.horizontal, 24)
                    .padding(.top, 12)
                    .padding(.bottom, 32)
            }
        }
        .animation(.snappy(duration: 0.32, extraBounce: 0.02), value: screen)
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
    private func screenView(copy: AuthCopy) -> some View {
        switch screen {
        case .login:
            LoginScreenView(
                copy: copy,
                email: $loginForm.email,
                password: $loginForm.password,
                passwordVisible: $loginForm.passwordVisible,
                isLoading: isLoading,
                message: loginForm.message,
                onDismiss: dismiss.callAsFunction,
                onForgotPassword: { switchScreen(to: .reset) },
                onSubmit: {
                    loginForm.message = nil
                    submit(copy: copy)
                },
                onShowRegister: { switchScreen(to: .register) }
            )
        case .register:
            RegisterScreenView(
                copy: copy,
                email: $registerForm.email,
                verificationCode: $registerForm.verificationCode,
                password: $registerForm.password,
                passwordVisible: $registerForm.passwordVisible,
                confirmPassword: $registerForm.confirmPassword,
                confirmPasswordVisible: $registerForm.confirmPasswordVisible,
                agreedToTerms: $registerForm.agreedToTerms,
                isLoading: isLoading,
                message: registerForm.message,
                codeCooldown: registerForm.codeCooldown,
                onBack: { switchScreen(to: .login) },
                onOpenTerms: { openLegalURL(legalLinks?.termsURL) },
                onOpenPrivacy: { openLegalURL(legalLinks?.privacyURL) },
                onSendCode: {
                    registerForm.message = nil
                    sendCode(copy: copy)
                },
                onSubmit: {
                    registerForm.message = nil
                    submit(copy: copy)
                },
                onShowLogin: { switchScreen(to: .login) }
            )
        case .reset:
            ResetPasswordScreenView(
                copy: copy,
                email: $resetForm.email,
                verificationCode: $resetForm.verificationCode,
                password: $resetForm.password,
                passwordVisible: $resetForm.passwordVisible,
                isLoading: isLoading,
                message: resetForm.message,
                codeCooldown: resetForm.codeCooldown,
                onBack: { switchScreen(to: .login) },
                onSendCode: {
                    resetForm.message = nil
                    sendCode(copy: copy)
                },
                onSubmit: {
                    resetForm.message = nil
                    submit(copy: copy)
                },
                onShowLogin: { switchScreen(to: .login) }
            )
        }
    }

    private var screenTransition: AnyTransition {
        switch navigationDirection {
        case .forward:
            return .asymmetric(
                insertion: .move(edge: .trailing).combined(with: .opacity),
                removal: .move(edge: .leading).combined(with: .opacity)
            )
        case .backward:
            return .asymmetric(
                insertion: .move(edge: .leading).combined(with: .opacity),
                removal: .move(edge: .trailing).combined(with: .opacity)
            )
        }
    }

    private var normalizedEmail: String {
        formEmail(for: screen).trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    private var normalizedCode: String {
        formCode(for: screen).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func codeButtonTitle(copy: AuthCopy) -> String {
        currentCodeCooldown > 0 ? "\(currentCodeCooldown)s" : copy.getCode
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
            setMessage(copy.emailRequired, for: screen)
            return
        }
        guard normalizedEmail.contains("@") else {
            setMessage(copy.invalidEmail, for: screen)
            return
        }

        do {
            let purpose = screen == .register ? "REGISTER" : "RESET_PASSWORD"
            let scene = screen == .register ? "AUTH_REGISTER" : "AUTH_FORGOT_PASSWORD"
            try await service.sendCode(email: normalizedEmail, purpose: purpose, source: "ios-client", scene: scene)
            setMessage(copy.codeSent, for: screen)
            startCooldown(seconds: 60)
        } catch {
            setMessage(error.localizedDescription, for: screen)
        }
    }

    private func performSubmit(copy: AuthCopy) async {
        defer { isLoading = false }

        guard normalizedEmail.isEmpty == false else {
            setMessage(copy.emailRequired, for: screen)
            return
        }

        do {
            switch screen {
            case .login:
                guard loginForm.password.isEmpty == false else {
                    setMessage(copy.passwordRequired, for: .login)
                    return
                }
                guard loginForm.password.count >= 8 else {
                    setMessage(copy.passwordTooShort, for: .login)
                    return
                }
                let session = try await service.login(email: normalizedEmail, password: loginForm.password)
                await AuthSessionStore.shared.save(session)
                onAuthenticated(session)
                dismiss()
            case .register:
                guard normalizedEmail.contains("@") else {
                    setMessage(copy.invalidEmail, for: .register)
                    return
                }
                guard registerForm.password.isEmpty == false else {
                    setMessage(copy.passwordRequired, for: .register)
                    return
                }
                guard registerForm.password.count >= 8 else {
                    setMessage(copy.passwordTooShort, for: .register)
                    return
                }
                guard normalizedCode.isEmpty == false else {
                    setMessage(copy.codeRequired, for: .register)
                    return
                }
                guard registerForm.password == registerForm.confirmPassword else {
                    setMessage(copy.passwordMismatch, for: .register)
                    return
                }
                guard registerForm.agreedToTerms else {
                    setMessage(copy.termsRequired, for: .register)
                    return
                }
                let session = try await service.register(
                    email: normalizedEmail,
                    password: registerForm.password,
                    code: normalizedCode,
                    accountType: "PLAYER"
                )
                await AuthSessionStore.shared.save(session)
                onAuthenticated(session)
                dismiss()
            case .reset:
                guard normalizedEmail.contains("@") else {
                    setMessage(copy.invalidEmail, for: .reset)
                    return
                }
                guard normalizedCode.isEmpty == false else {
                    setMessage(copy.codeRequired, for: .reset)
                    return
                }
                guard resetForm.password.isEmpty == false else {
                    setMessage(copy.passwordRequired, for: .reset)
                    return
                }
                guard resetForm.password.count >= 8 else {
                    setMessage(copy.passwordTooShort, for: .reset)
                    return
                }
                try await service.resetPassword(
                    email: normalizedEmail,
                    code: normalizedCode,
                    newPassword: resetForm.password
                )
                resetForm.message = copy.resetSuccess
                navigationDirection = .backward
                screen = .login
                resetForm.password = ""
                resetForm.passwordVisible = false
                resetForm.verificationCode = ""
            }
        } catch {
            setMessage(error.localizedDescription, for: screen)
        }
    }

    private func startCooldown(seconds: Int) {
        cooldownTask?.cancel()
        setCodeCooldown(max(seconds, 0), for: screen)
        let cooldownScreen = screen
        cooldownTask = Task {
            var remaining = max(seconds, 0)
            while remaining > 0, Task.isCancelled == false {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                remaining -= 1
                await MainActor.run {
                    setCodeCooldown(max(0, remaining), for: cooldownScreen)
                }
            }
        }
    }

    private func switchScreen(to target: Screen) {
        navigationDirection = navigationDirectionForTransition(from: screen, to: target)
        setMessage(nil, for: screen)
        screen = target
    }

    private func navigationDirectionForTransition(from current: Screen, to target: Screen) -> NavigationDirection {
        screenRank(target) >= screenRank(current) ? .forward : .backward
    }

    private func screenRank(_ screen: Screen) -> Int {
        switch screen {
        case .login:
            return 0
        case .register:
            return 1
        case .reset:
            return 2
        }
    }

    private func openLegalURL(_ url: URL?) {
        guard let url else { return }
        openURL(url)
    }

    private var currentCodeCooldown: Int {
        switch screen {
        case .login:
            return 0
        case .register:
            return registerForm.codeCooldown
        case .reset:
            return resetForm.codeCooldown
        }
    }

    private func formEmail(for screen: Screen) -> String {
        switch screen {
        case .login:
            return loginForm.email
        case .register:
            return registerForm.email
        case .reset:
            return resetForm.email
        }
    }

    private func formCode(for screen: Screen) -> String {
        switch screen {
        case .login:
            return ""
        case .register:
            return registerForm.verificationCode
        case .reset:
            return resetForm.verificationCode
        }
    }

    private func setMessage(_ value: String?, for screen: Screen) {
        switch screen {
        case .login:
            loginForm.message = value
        case .register:
            registerForm.message = value
        case .reset:
            resetForm.message = value
        }
    }

    private func setCodeCooldown(_ value: Int, for screen: Screen) {
        switch screen {
        case .login:
            break
        case .register:
            registerForm.codeCooldown = value
        case .reset:
            resetForm.codeCooldown = value
        }
    }
}

private struct LoginScreenView: View {
    let copy: AuthCopy
    @Binding var email: String
    @Binding var password: String
    @Binding var passwordVisible: Bool
    let isLoading: Bool
    let message: String?
    let onDismiss: () -> Void
    let onForgotPassword: () -> Void
    let onSubmit: () -> Void
    let onShowRegister: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            AuthTopBar(kind: .close, action: onDismiss)
            AuthTitleBlock(title: copy.loginTitle, subtitle: copy.loginSubtitle)

            VStack(alignment: .leading, spacing: 16) {
                AuthInputField(
                    title: copy.accountLabel,
                    text: $email,
                    keyboardType: .emailAddress
                )

                AuthInputField(
                    title: copy.passwordLabel,
                    text: $password,
                    isSecure: true,
                    isSecureVisible: $passwordVisible
                )

                HStack {
                    Spacer()
                    Button(copy.forgotPassword, action: onForgotPassword)
                        .buttonStyle(.plain)
                        .foregroundStyle(Color(hex: 0x6B4EFF))
                        .font(.system(size: 15, weight: .medium))
                }

                AuthPrimaryButton(
                    title: isLoading ? copy.loading : copy.loginAction,
                    isDisabled: isLoading,
                    topPadding: 20,
                    action: onSubmit
                )

                AuthMessageView(message: message)
            }

            AuthFooter(
                prefix: copy.footerLoginPrefix,
                action: copy.footerLoginAction,
                onTap: onShowRegister
            )
        }
    }
}

private struct RegisterScreenView: View {
    let copy: AuthCopy
    @Binding var email: String
    @Binding var verificationCode: String
    @Binding var password: String
    @Binding var passwordVisible: Bool
    @Binding var confirmPassword: String
    @Binding var confirmPasswordVisible: Bool
    @Binding var agreedToTerms: Bool
    let isLoading: Bool
    let message: String?
    let codeCooldown: Int
    let onBack: () -> Void
    let onOpenTerms: () -> Void
    let onOpenPrivacy: () -> Void
    let onSendCode: () -> Void
    let onSubmit: () -> Void
    let onShowLogin: () -> Void

    private var normalizedEmail: String {
        email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            AuthTopBar(kind: .back, action: onBack)
            AuthTitleBlock(title: copy.registerTitle, subtitle: copy.registerSubtitle)

            VStack(alignment: .leading, spacing: 16) {
                AuthInputField(
                    title: copy.accountLabel,
                    text: $email,
                    keyboardType: .emailAddress
                )

                AuthCodeRow(
                    title: copy.registerCodeLabel,
                    text: $verificationCode,
                    buttonTitle: codeCooldown > 0 ? "\(codeCooldown)s" : copy.getCode,
                    isButtonDisabled: isLoading || normalizedEmail.isEmpty || codeCooldown > 0,
                    onTap: onSendCode
                )

                AuthInputField(
                    title: copy.passwordLabel,
                    text: $password,
                    isSecure: true,
                    isSecureVisible: $passwordVisible
                )

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

                    AuthLegalText(
                        copy: copy,
                        onOpenTerms: onOpenTerms,
                        onOpenPrivacy: onOpenPrivacy
                    )
                }
                .padding(.top, 2)

                AuthPrimaryButton(
                    title: isLoading ? copy.loading : copy.registerAction,
                    isDisabled: isLoading,
                    topPadding: 8,
                    action: onSubmit
                )

                AuthMessageView(message: message)
            }

            AuthFooter(
                prefix: copy.footerRegisterPrefix,
                action: copy.footerRegisterAction,
                onTap: onShowLogin
            )
        }
    }
}

private struct ResetPasswordScreenView: View {
    let copy: AuthCopy
    @Binding var email: String
    @Binding var verificationCode: String
    @Binding var password: String
    @Binding var passwordVisible: Bool
    let isLoading: Bool
    let message: String?
    let codeCooldown: Int
    let onBack: () -> Void
    let onSendCode: () -> Void
    let onSubmit: () -> Void
    let onShowLogin: () -> Void

    private var normalizedEmail: String {
        email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            AuthTopBar(kind: .back, action: onBack)
            AuthTitleBlock(title: copy.resetTitle, subtitle: copy.resetSubtitle)

            VStack(alignment: .leading, spacing: 16) {
                AuthInputField(
                    title: copy.accountLabelForgot,
                    text: $email,
                    keyboardType: .emailAddress
                )

                AuthCodeRow(
                    title: copy.resetCodeLabel,
                    text: $verificationCode,
                    buttonTitle: codeCooldown > 0 ? "\(codeCooldown)s" : copy.getCode,
                    isButtonDisabled: isLoading || normalizedEmail.isEmpty || codeCooldown > 0,
                    onTap: onSendCode
                )

                AuthInputField(
                    title: copy.newPasswordLabel,
                    text: $password,
                    isSecure: true,
                    isSecureVisible: $passwordVisible
                )

                AuthPrimaryButton(
                    title: isLoading ? copy.loading : copy.resetAction,
                    isDisabled: isLoading,
                    topPadding: 8,
                    action: onSubmit
                )

                AuthMessageView(message: message)
            }

            AuthFooter(
                prefix: copy.footerResetPrefix,
                action: copy.footerResetAction,
                onTap: onShowLogin
            )
        }
    }
}

private struct AuthTopBar: View {
    enum Kind {
        case close
        case back
    }

    let kind: Kind
    let action: () -> Void

    var body: some View {
        HStack {
            if kind == .back {
                Button(action: action) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(NativeNavigationButtonStyle())
                Spacer()
            } else {
                Spacer()
                Button(action: action) {
                    Image(systemName: "xmark")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(NativeNavigationButtonStyle())
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }
}

private struct AuthTitleBlock: View {
    let title: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.system(size: 34, weight: .black))
                .foregroundStyle(.white)

            Text(subtitle)
                .font(.system(size: 16))
                .foregroundStyle(Color(hex: 0xA0A0A0))
                .padding(.top, 8)
        }
        .padding(.bottom, 40)
    }
}

private struct AuthCodeRow: View {
    let title: String
    @Binding var text: String
    let buttonTitle: String
    let isButtonDisabled: Bool
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            AuthInputField(
                title: title,
                text: $text,
                keyboardType: .numberPad
            )

            Button(buttonTitle, action: onTap)
                .disabled(isButtonDisabled)
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
}

private struct AuthPrimaryButton: View {
    let title: String
    let isDisabled: Bool
    let topPadding: CGFloat
    let action: () -> Void

    var body: some View {
        Button(title, action: action)
            .disabled(isDisabled)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(hex: 0x6B4EFF))
            )
            .foregroundStyle(.white)
            .font(.system(size: 17, weight: .bold))
            .padding(.top, topPadding)
    }
}

private struct AuthMessageView: View {
    let message: String?

    var body: some View {
        if let message, message.isEmpty == false {
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: 0xA0A0A0))
                .padding(.top, 4)
        }
    }
}

private struct AuthFooter: View {
    let prefix: String
    let action: String
    let onTap: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 24)
            HStack(spacing: 6) {
                Text(prefix)
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                Text(action)
                    .foregroundStyle(Color(hex: 0x6B4EFF))
                    .fontWeight(.bold)
                    .onTapGesture(perform: onTap)
            }
            .font(.system(size: 15))
            .frame(maxWidth: .infinity)
            .padding(.top, 24)
        }
    }
}

private struct AuthLegalText: View {
    let copy: AuthCopy
    let onOpenTerms: () -> Void
    let onOpenPrivacy: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Text(copy.termsPrefix)
                .foregroundColor(Color(hex: 0xA0A0A0))

            Button(copy.userAgreement, action: onOpenTerms)
                .buttonStyle(.plain)
                .foregroundStyle(Color(hex: 0x6B4EFF))

            Text(copy.termsConnector)
                .foregroundColor(Color(hex: 0xA0A0A0))

            Button(copy.privacyPolicy, action: onOpenPrivacy)
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
    let passwordTooShort: String
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
                passwordTooShort: "密码长度不能少于 8 位",
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
                passwordTooShort: "密碼長度不能少於 8 位",
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
                passwordTooShort: "Password must be at least 8 characters",
                codeRequired: "Please enter the verification code",
                termsRequired: "Please agree to the user agreement and privacy policy first",
                passwordMismatch: "The passwords do not match",
                codeSent: "Verification code sent. Please check your inbox",
                resetSuccess: "Password reset complete. Please sign in again"
            )
        }
    }
}
