import SwiftUI
import UIKit

@MainActor
final class ReferralViewModel: ObservableObject {
    @Published private(set) var isLoading = false
    @Published private(set) var summary: ReferralSummary?
    @Published private(set) var records: [ReferralRecord] = []
    @Published private(set) var message: String?

    private let service: ReferralServiceProtocol

    init(service: ReferralServiceProtocol = ReferralService()) {
        self.service = service
    }

    func load() {
        Task {
            isLoading = true
            defer { isLoading = false }
            do {
                async let summaryTask = service.fetchSummary()
                async let recordsTask = service.fetchRecords(limit: 20)
                let (summary, records) = try await (summaryTask, recordsTask)
                self.summary = summary
                self.records = records
            } catch {
                message = error.localizedDescription
            }
        }
    }

    func markShared(_ channel: String) {
        Task { try? await service.markShared(channel: channel) }
    }
}

struct ReferralView: View {
    @StateObject private var viewModel = ReferralViewModel()
    @State private var hasLoaded = false
    @State private var toastMessage: String?

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                headerHero

                VStack(alignment: .leading, spacing: 20) {
                    if let message = viewModel.message {
                        NativeStateCard {
                            Text(message)
                                .font(.system(size: 13))
                                .foregroundStyle(Color(hex: 0xA0A0A0))
                        }
                    }

                    if viewModel.isLoading {
                        referralSkeleton
                            .transition(NativeMotion.stateSwapTransition)
                    } else {
                        VStack(alignment: .leading, spacing: 20) {
                            summaryCard
                            linkCard
                            shareCard
                            rulesCard
                            recordsCard
                        }
                        .transition(NativeMotion.contentRevealTransition)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 30)
                .padding(.bottom, 96)
                .background(Color(hex: 0x121212))
                .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                .offset(y: -30)
                .padding(.bottom, -30)
            }
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear {
            guard hasLoaded == false else { return }
            hasLoaded = true
            viewModel.load()
        }
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading)
        .animation(NativeMotion.overlayTransition, value: viewModel.records.isEmpty)
        .overlay(alignment: .bottom) {
            NativeToastOverlay(message: $toastMessage)
        }
    }

    private var referralSkeleton: some View {
        VStack(alignment: .leading, spacing: 20) {
            summaryCard.redacted(reason: .placeholder)
            linkCard.redacted(reason: .placeholder)
            shareCard.redacted(reason: .placeholder)
            rulesCard.redacted(reason: .placeholder)
        }
    }

    private var copy: ReferralCopy {
        let language = AppLanguageStore.currentSync()
        return .forLanguage(language)
    }

    private var linkText: String {
        let raw = viewModel.summary?.referralLink ?? ""
        return raw.isEmpty ? copy.linkPlaceholder : raw
    }

    private var headerHero: some View {
        VStack(spacing: 10) {
            Text("🎁")
                .font(.system(size: 64))
            Text(copy.heroTitle)
                .font(.system(size: 28, weight: .black))
                .foregroundStyle(.white)
            Text(copy.heroSubtitle)
                .font(.system(size: 15))
                .foregroundStyle(Color.white.opacity(0.82))
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 24)
        .padding(.top, 40)
        .padding(.bottom, 60)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }

    private var summaryCard: some View {
        HStack(spacing: 0) {
            summaryMetric(
                value: "\(viewModel.summary?.inviteCount ?? 0)",
                label: copy.summaryInviteLabel,
                valueColor: .white
            )
            Rectangle()
                .fill(Color(hex: 0x2D2D31))
                .frame(width: 1)
                .padding(.vertical, 8)
            summaryMetric(
                value: viewModel.summary?.totalReward ?? "0",
                label: copy.summaryRewardLabel,
                valueColor: Color(hex: 0x36C282)
            )
        }
        .padding(24)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private var linkCard: some View {
        referralCard(title: copy.linkTitle) {
            VStack(alignment: .leading, spacing: 14) {
                Text(linkText)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(Color(hex: 0x6B4EFF))
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(copy.linkHint)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))

                HStack(spacing: 12) {
                    Spacer()

                    Button(copy.copy) {
                        UIPasteboard.general.string = linkText
                        toastMessage = copy.linkCopied
                    }
                    .buttonStyle(.plain)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(height: 40)
                    .padding(.horizontal, 16)
                    .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
        }
    }

    private var shareCard: some View {
        referralCard(title: copy.shareTitle) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 15), count: 4), spacing: 15) {
                shareButton(title: copy.shareWhatsApp, icon: "💬", fill: Color(hex: 0x25D366), channel: "whatsapp", text: linkText)
                shareButton(title: copy.shareFacebook, icon: "f", fill: Color(hex: 0x1877F2), channel: "facebook", text: linkText)
                shareButton(title: copy.shareXiaohongshu, icon: "📕", fill: Color(hex: 0xFF2442), channel: "xiaohongshu", text: linkText)
                shareButton(title: copy.shareMore, icon: "🔗", fill: Color(hex: 0x232326), channel: "system", text: linkText)
            }
        }
    }

    private var rulesCard: some View {
        referralCard(title: copy.rulesTitle) {
            VStack(alignment: .leading, spacing: 18) {
                rulesRow(index: "1", text: copy.ruleOne)
                rulesRow(index: "2", text: copy.ruleTwo)
                rulesRow(index: "3", text: copy.ruleThree)

                Rectangle()
                    .fill(Color(hex: 0x2D2D31))
                    .frame(height: 1)

                Text(copy.disclaimer)
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: 0x66666C))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var recordsCard: some View {
        referralCard(title: copy.recordsTitle) {
            if viewModel.records.isEmpty {
                Text(copy.noRecords)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(viewModel.records.enumerated()), id: \.element.id) { index, record in
                        HStack(alignment: .top, spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(record.title)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(.white)
                                if record.subtitle.isEmpty == false {
                                    Text(record.subtitle)
                                        .font(.system(size: 12))
                                        .foregroundStyle(Color(hex: 0xA0A0A0))
                                }
                                if record.createdAt.isEmpty == false {
                                    Text(record.createdAt)
                                        .font(.system(size: 12))
                                        .foregroundStyle(Color(hex: 0xA0A0A0))
                                }
                            }

                            Spacer()

                            Text(record.reward)
                                .font(.system(size: 15, weight: .bold, design: .monospaced))
                                .foregroundStyle(.white)
                        }
                        .padding(.vertical, 14)

                        if index != viewModel.records.count - 1 {
                            Rectangle()
                                .fill(Color(hex: 0x2D2D31))
                                .frame(height: 1)
                        }
                    }
                }
            }
        }
    }

    private func referralCard<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Color(hex: 0xA0A0A0))
            content()
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private func skeletonCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            content()
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private func shareButton(title: String, icon: String, fill: Color, channel: String, text: String) -> some View {
        Button {
            viewModel.markShared(channel)
            let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
            UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap(\.windows)
                .first { $0.isKeyWindow }?
                .rootViewController?
                .present(controller, animated: true)
        } label: {
            VStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .fill(fill)
                    .frame(height: 50)
                    .overlay(
                        Text(icon)
                            .font(.system(size: icon == "f" ? 24 : 22, weight: icon == "f" ? .black : .regular))
                            .foregroundStyle(.white)
                    )

                Text(title)
                    .font(.system(size: 10))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
            }
        }
        .buttonStyle(.plain)
    }

    private func summaryMetric(value: String, label: String, valueColor: Color) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 24, weight: .black))
                .foregroundStyle(valueColor)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Color(hex: 0xA0A0A0))
        }
        .frame(maxWidth: .infinity)
    }

    private func rulesRow(index: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(index)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(Color(hex: 0x6B4EFF))
                .frame(width: 22, height: 22)
                .background(Color(hex: 0x232326), in: Circle())
                .overlay(
                    Circle()
                        .stroke(Color(hex: 0x6B4EFF), lineWidth: 1)
                )

            Text(text)
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: 0xA0A0A0))
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct ReferralCopy {
    let title: String
    let heroTitle: String
    let heroSubtitle: String
    let summaryInviteLabel: String
    let summaryRewardLabel: String
    let linkTitle: String
    let linkHint: String
    let copy: String
    let shareTitle: String
    let shareMore: String
    let shareWhatsApp: String
    let shareFacebook: String
    let shareXiaohongshu: String
    let rulesTitle: String
    let ruleOne: String
    let ruleTwo: String
    let ruleThree: String
    let disclaimer: String
    let recordsTitle: String
    let noRecords: String
    let linkCopied: String
    let linkPlaceholder: String

    static func forLanguage(_ language: AppLanguage) -> ReferralCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "邀请奖励",
                heroTitle: "邀请奖励",
                heroSubtitle: "分享专属链接，与好友瓜分平台币",
                summaryInviteLabel: "成功邀请 (人)",
                summaryRewardLabel: "累计奖励 (🪙)",
                linkTitle: "邀请链接",
                linkHint: "你的专属邀请链接",
                copy: "复制",
                shareTitle: "社交分享",
                shareMore: "更多分享",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                shareXiaohongshu: "小红书",
                rulesTitle: "奖励规则与指南",
                ruleOne: "发送链接：点击上方分享按钮，将你的专属链接发送给未注册过 Nexus 的好友。",
                ruleTwo: "好友注册：好友通过链接进入页面，完成注册或平台要求的活动。",
                ruleThree: "获得奖励：好友完成首个有效目标后，奖励会自动累计到你的钱包余额。",
                disclaimer: "* 严禁任何形式的刷号行为，一经发现将冻结账户及相关奖励。",
                recordsTitle: "邀请记录",
                noRecords: "暂无邀请记录",
                linkCopied: "邀请链接已复制",
                linkPlaceholder: "暂无链接"
            )
        case .traditionalChinese:
            return .init(
                title: "邀請獎勵",
                heroTitle: "邀請獎勵",
                heroSubtitle: "分享專屬連結，與好友一起獲得平台幣",
                summaryInviteLabel: "成功邀請 (人)",
                summaryRewardLabel: "累計獎勵 (🪙)",
                linkTitle: "邀請連結",
                linkHint: "你的專屬邀請連結",
                copy: "複製",
                shareTitle: "社交分享",
                shareMore: "更多分享",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                shareXiaohongshu: "小紅書",
                rulesTitle: "獎勵規則與指南",
                ruleOne: "發送連結：點擊上方分享按鈕，將你的專屬連結發送給未註冊過 Nexus 的好友。",
                ruleTwo: "好友註冊：好友透過連結進入頁面，完成註冊或平台要求的活動。",
                ruleThree: "獲得獎勵：好友完成首個有效目標後，獎勵會自動累計到你的錢包餘額。",
                disclaimer: "* 嚴禁任何形式的刷號行為，一經發現將凍結帳戶及相關獎勵。",
                recordsTitle: "邀請記錄",
                noRecords: "暫無邀請記錄",
                linkCopied: "邀請連結已複製",
                linkPlaceholder: "暫無連結"
            )
        case .english:
            return .init(
                title: "Referral Rewards",
                heroTitle: "Referral Rewards",
                heroSubtitle: "Share your personal link and earn coins with friends",
                summaryInviteLabel: "Successful Invites",
                summaryRewardLabel: "Total Reward",
                linkTitle: "Referral Link",
                linkHint: "Your personal referral link",
                copy: "Copy",
                shareTitle: "Quick Share",
                shareMore: "More",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                shareXiaohongshu: "Xiaohongshu",
                rulesTitle: "Reward Rules",
                ruleOne: "Send your personal link to friends who have not registered for Nexus before.",
                ruleTwo: "Friends open the page through your link and complete registration or the required campaign action.",
                ruleThree: "Once the first valid goal is completed, rewards are added to your wallet automatically.",
                disclaimer: "* Fraudulent referrals are prohibited and may result in account and reward suspension.",
                recordsTitle: "Referral Records",
                noRecords: "No referral records",
                linkCopied: "Referral link copied",
                linkPlaceholder: "No link yet"
            )
        }
    }
}
