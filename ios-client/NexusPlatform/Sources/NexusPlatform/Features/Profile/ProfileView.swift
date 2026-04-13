import SwiftUI

struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()
    @State private var balance: String = "88.00"
    @State private var showCashier = false
    @State private var showLogoutAlert = false
    @State private var clearResult: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                authCard
                walletCard
                actionsCard
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("我的")
        .onAppear { viewModel.loadSession() }
        .sheet(isPresented: $showCashier) {
            cashierSheet
                .presentationDetents([.medium])
        }
        .alert("确认注销账号？", isPresented: $showLogoutAlert) {
            Button("取消", role: .cancel) {}
            Button("确认", role: .destructive) {
                clearResult = "账号已注销（演示态）"
            }
        }
    }

    private var authCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("账号")
                .font(.headline)

            if viewModel.isLoggedIn {
                Text(viewModel.currentEmail ?? "")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                Button("退出登录") {
                    viewModel.logout()
                }
                .foregroundStyle(AppTheme.ColorToken.danger)
            } else {
                TextField("邮箱", text: $viewModel.email)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
                    .padding(10)
                    .background(AppTheme.ColorToken.surfaceSecondary, in: RoundedRectangle(cornerRadius: 10))
                SecureField("密码", text: $viewModel.password)
                    .padding(10)
                    .background(AppTheme.ColorToken.surfaceSecondary, in: RoundedRectangle(cornerRadius: 10))
                Button(viewModel.isLoading ? "登录中..." : "邮箱登录") {
                    viewModel.login()
                }
                .disabled(viewModel.isLoading)
                .font(.subheadline.bold())
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(AppTheme.GradientToken.hero, in: Capsule())
                .foregroundStyle(Color.white)
            }

            if let message = viewModel.message {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .nexusGlassCard()
    }

    private var walletCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("钱包余额")
                .font(.subheadline)
                .foregroundStyle(AppTheme.ColorToken.textSecondary)
            Text("¥\(balance)")
                .font(.system(size: 34, weight: .heavy, design: .rounded))
            Button("充值") {
                showCashier = true
            }
            .font(.subheadline.bold())
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(AppTheme.ColorToken.surfaceSecondary, in: Capsule())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .nexusGlassCard()
    }

    private var actionsCard: some View {
        VStack(spacing: 10) {
            Button("清理本地缓存") {
                Task {
                    let storage = VersionedGameStorageManager()
                    try? await storage.clearAllLocalCaches()
                    clearResult = "缓存已清理"
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button("注销账号") {
                showLogoutAlert = true
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(AppTheme.ColorToken.danger)

            if let clearResult {
                Divider()
                Text(clearResult)
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(16)
        .nexusGlassCard()
    }

    private var cashierSheet: some View {
        VStack(spacing: 12) {
            Text("原生收银台")
                .font(.headline)
            Button("购买 ¥6.00") { balance = "94.00" }
            Button("购买 ¥30.00") { balance = "118.00" }
            Button("关闭") { showCashier = false }
                .foregroundStyle(AppTheme.ColorToken.textSecondary)
        }
        .padding(20)
    }
}
