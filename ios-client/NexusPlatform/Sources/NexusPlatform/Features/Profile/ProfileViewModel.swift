import Foundation

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var email: String = ""
    @Published var password: String = ""
    @Published private(set) var currentEmail: String?
    @Published private(set) var isLoading = false
    @Published private(set) var message: String?

    private let authService: AuthServiceProtocol
    private let authStore: AuthSessionStore

    init(
        authService: AuthServiceProtocol = AuthService(),
        authStore: AuthSessionStore = .shared
    ) {
        self.authService = authService
        self.authStore = authStore
    }

    func loadSession() {
        Task {
            let session = await authStore.current()
            currentEmail = session?.email
        }
    }

    var isLoggedIn: Bool {
        currentEmail?.isEmpty == false
    }

    func login() {
        guard email.isEmpty == false, password.isEmpty == false else {
            message = "请输入邮箱和密码"
            return
        }
        Task {
            isLoading = true
            defer { isLoading = false }
            do {
                let session = try await authService.login(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password)
                await authStore.save(session)
                currentEmail = session.email
                password = ""
                message = "登录成功"
            } catch {
                message = error.localizedDescription
            }
        }
    }

    func logout() {
        Task {
            await authStore.clear()
            currentEmail = nil
            message = "已退出登录"
        }
    }
}
