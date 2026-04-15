import SwiftUI

struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()
    @State private var showLogoutAlert = false
    @State private var showAuthFlow = false
    @State private var clearResult: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                authCard
                walletCard
                quickActionsCard
                billingCard
                deviceCard
                actionsCard
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("我的")
        .onAppear { viewModel.loadSession() }
        .sheet(isPresented: $showAuthFlow) {
            NavigationStack {
                AuthFlowView { session in
                    viewModel.applyAuthenticatedSession(session)
                }
            }
        }
        .alert("确认注销账号？", isPresented: $showLogoutAlert) {
            Button("取消", role: .cancel) {}
            Button("确认", role: .destructive) {
                viewModel.terminateAccount()
            }
        } message: {
            Text("该操作不可恢复，将清空账号并退出登录。")
        }
    }

    private var authCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("账号")
                .font(.headline)

            if viewModel.isLoggedIn {
                Text(viewModel.displayName ?? viewModel.currentEmail ?? "")
                    .font(.subheadline.weight(.semibold))
                Text(viewModel.currentEmail ?? "")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                Button("退出登录") {
                    viewModel.logout()
                }
                .foregroundStyle(AppTheme.ColorToken.danger)
            } else {
                Text("支持邮箱登录、注册和忘记密码。")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                Button("登录 / 注册") {
                    showAuthFlow = true
                }
                .nexusPrimaryCTA()
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

    private var quickActionsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("偏好与安全")
                .font(.headline)

            NavigationLink(
                destination: ReferralView()
            ) {
                actionRow(title: "邀请奖励", subtitle: "查看邀请人数、奖励和分享链接")
            }
            .buttonStyle(.plain)

            NavigationLink(
                destination: AccountSecurityView(
                    email: viewModel.currentEmail ?? "",
                    onSavedLanguage: nil,
                    cloudSyncEnabled: viewModel.cloudSyncEnabled,
                    onCloudSyncToggle: { viewModel.setCloudSyncEnabled($0) },
                    selectedLanguage: viewModel.selectedLanguage,
                    onLanguageSelect: { viewModel.setLanguage($0) }
                )
            ) {
                actionRow(
                    title: "账号安全",
                    subtitle: "云同步：\(viewModel.cloudSyncEnabled ? "已开启" : "已关闭")  ·  语言：\(viewModel.selectedLanguage.title)"
                )
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .nexusGlassCard()
    }

    private var walletCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("钱包余额")
                .font(.subheadline)
                .foregroundStyle(AppTheme.ColorToken.textSecondary)
            Text("¥\(viewModel.balanceText)")
                .font(.system(size: 34, weight: .heavy, design: .rounded))

            if viewModel.isWalletLoading {
                ProgressView("同步中...")
                    .font(.footnote)
            } else {
                Text("可用余额 ¥\(viewModel.availableBalanceText)")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
            }

            Button("刷新钱包") {
                viewModel.loadWalletManually()
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

    private var billingCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("账单记录")
                    .font(.headline)
                Spacer()
                Button("刷新") {
                    viewModel.loadBillingManually()
                }
                .font(.footnote.weight(.semibold))
            }

            if viewModel.isBillingLoading {
                ProgressView("加载中...")
                    .font(.footnote)
            } else if viewModel.billingRecords.isEmpty {
                Text("暂无账单记录")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
            } else {
                ForEach(viewModel.billingRecords.prefix(6)) { record in
                    NavigationLink(destination: BillingDetailView(record: record)) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(record.title)
                                    .font(.subheadline.weight(.semibold))
                                Text(record.subtitle.isEmpty ? record.type : record.subtitle)
                                    .font(.caption)
                                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                                if record.createdAtText.isEmpty == false {
                                    Text(record.createdAtText)
                                        .font(.caption2)
                                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                                }
                            }
                            Spacer()
                            let amount = viewModel.amountText(record.amount)
                            Text("¥\(amount)")
                                .font(.subheadline.monospacedDigit())
                                .foregroundStyle(amount.hasPrefix("-") ? AppTheme.ColorToken.danger : AppTheme.ColorToken.success)
                        }
                    }
                    .buttonStyle(.plain)
                    if record.id != viewModel.billingRecords.prefix(6).last?.id {
                        Divider()
                    }
                }
            }
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
            .disabled(viewModel.isTerminating)
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

    private var deviceCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("设备管理")
                    .font(.headline)
                Spacer()
                Button("刷新") {
                    viewModel.loadDevicesManually()
                }
                .font(.footnote.weight(.semibold))
            }

            if viewModel.isDevicesLoading {
                ProgressView("加载中...")
                    .font(.footnote)
            } else if viewModel.devices.isEmpty {
                Text("暂无设备记录")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
            } else {
                ForEach(viewModel.devices.prefix(6)) { device in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(device.deviceName.isEmpty ? "Unknown Device" : device.deviceName)
                                .font(.subheadline.weight(.semibold))
                            if device.current {
                                Text("当前")
                                    .font(.caption2.bold())
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(AppTheme.ColorToken.success.opacity(0.2), in: Capsule())
                            }
                            Spacer()
                            if device.current == false {
                                Button("下线") {
                                    viewModel.kickDevice(device.id)
                                }
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(AppTheme.ColorToken.danger)
                            }
                        }
                        Text([device.model, device.ip].filter { $0.isEmpty == false }.joined(separator: " · "))
                            .font(.caption)
                            .foregroundStyle(AppTheme.ColorToken.textSecondary)
                        if device.lastActiveAt.isEmpty == false {
                            Text(device.lastActiveAt)
                                .font(.caption2)
                                .foregroundStyle(AppTheme.ColorToken.textSecondary)
                        }
                    }
                    if device.id != viewModel.devices.prefix(6).last?.id {
                        Divider()
                    }
                }

                Button("退出其他设备") {
                    viewModel.logoutAllDevices()
                }
                .font(.footnote.weight(.semibold))
                .foregroundStyle(AppTheme.ColorToken.danger)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .nexusGlassCard()
    }

    private func actionRow(title: String, subtitle: String) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.ColorToken.textPrimary)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(AppTheme.ColorToken.textSecondary)
        }
    }

}
