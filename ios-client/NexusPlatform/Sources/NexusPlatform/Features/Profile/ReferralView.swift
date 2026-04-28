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
                        recordsCard
                    }
                    .transition(NativeMotion.contentRevealTransition)
                }
            }
            .padding(24)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
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
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x6B4EFF, alpha: 0.28), Color(hex: 0xA04CFF, alpha: 0.2)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(height: 156)
                .overlay(alignment: .leading) {
                    VStack(alignment: .leading, spacing: 10) {
                        NativeSkeletonBlock(width: 124, height: 14, cornerRadius: 7)
                        NativeSkeletonBlock(width: 156, height: 28, cornerRadius: 10)
                        Spacer()
                        NativeSkeletonBlock(width: 132, height: 14, cornerRadius: 7)
                        NativeSkeletonBlock(width: 164, height: 28, cornerRadius: 10)
                    }
                    .padding(24)
                }

            skeletonCard {
                NativeSkeletonBlock(width: 96, height: 13, cornerRadius: 6)
                NativeSkeletonBlock(height: 20, cornerRadius: 10)
                HStack(spacing: 12) {
                    NativeSkeletonBlock(height: 18, cornerRadius: 9)
                    NativeSkeletonBlock(width: 72, height: 44, cornerRadius: 14)
                }
            }

            skeletonCard {
                NativeSkeletonBlock(width: 110, height: 13, cornerRadius: 6)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(0..<3, id: \.self) { _ in
                        NativeSkeletonBlock(height: 50, cornerRadius: 16)
                    }
                }
            }

            skeletonCard {
                NativeSkeletonBlock(width: 118, height: 13, cornerRadius: 6)
                ForEach(0..<4, id: \.self) { index in
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            NativeSkeletonBlock(width: 148, height: 16, cornerRadius: 7)
                            NativeSkeletonBlock(width: 182, height: 12, cornerRadius: 6)
                            NativeSkeletonBlock(width: 92, height: 12, cornerRadius: 6)
                        }
                        Spacer()
                        NativeSkeletonBlock(width: 52, height: 16, cornerRadius: 7)
                    }
                    .padding(.vertical, 8)

                    if index != 3 {
                        Rectangle()
                            .fill(Color(hex: 0x2D2D31))
                            .frame(height: 1)
                    }
                }
            }
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

    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(copy.summaryTitle)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.8))

            Text(String(format: copy.invitesSummary, viewModel.summary?.inviteCount ?? 0))
                .font(.system(size: 28, weight: .black))
                .foregroundStyle(.white)

            Text(copy.summaryHint)
                .font(.system(size: 13))
                .foregroundStyle(Color.white.opacity(0.72))

            Spacer(minLength: 0)

            VStack(alignment: .leading, spacing: 6) {
                Text(copy.rewardLabel)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.white.opacity(0.8))
                Text(String(format: copy.rewardSummary, viewModel.summary?.totalReward ?? "0"))
                    .font(.system(size: 24, weight: .heavy, design: .monospaced))
                    .foregroundStyle(.white)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, minHeight: 156, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ),
            in: RoundedRectangle(cornerRadius: 24, style: .continuous)
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

                HStack(spacing: 12) {
                    Text(copy.linkHint)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0xA0A0A0))

                    Spacer()

                    Button(copy.copy) {
                        UIPasteboard.general.string = linkText
                        toastMessage = copy.linkCopied
                    }
                    .buttonStyle(.plain)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(height: 44)
                    .padding(.horizontal, 18)
                    .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
        }
    }

    private var shareCard: some View {
        referralCard(title: copy.shareTitle) {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                shareButton(title: copy.shareMore, icon: "square.and.arrow.up", channel: "system", text: linkText)
                shareButton(title: copy.shareWhatsApp, icon: "message.fill", channel: "whatsapp", text: linkText)
                shareButton(title: copy.shareFacebook, icon: "person.2.fill", channel: "facebook", text: linkText)
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

    private func shareButton(title: String, icon: String, channel: String, text: String) -> some View {
        Button(title) {
            viewModel.markShared(channel)
            let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
            UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap(\.windows)
                .first { $0.isKeyWindow }?
                .rootViewController?
                .present(controller, animated: true)
        }
        .frame(maxWidth: .infinity, minHeight: 50)
        .background(Color(hex: 0x232326), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(alignment: .leading) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color(hex: 0x6B4EFF))
                .padding(.leading, 14)
        }
        .buttonStyle(.plain)
        .foregroundStyle(.white)
        .font(.system(size: 14, weight: .medium))
    }
}

private struct ReferralCopy {
    let title: String
    let summaryTitle: String
    let summaryHint: String
    let rewardLabel: String
    let invitesSummary: String
    let rewardSummary: String
    let linkTitle: String
    let linkHint: String
    let copy: String
    let shareTitle: String
    let shareMore: String
    let shareWhatsApp: String
    let shareFacebook: String
    let recordsTitle: String
    let noRecords: String
    let linkCopied: String
    let linkPlaceholder: String

    static func forLanguage(_ language: AppLanguage) -> ReferralCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "邀请奖励",
                summaryTitle: "邀请概览",
                summaryHint: "分享你的专属链接，让好友加入并获得奖励。",
                rewardLabel: "累计奖励",
                invitesSummary: "邀请人数：%d",
                rewardSummary: "累计奖励：%@",
                linkTitle: "邀请链接",
                linkHint: "复制后分享给好友",
                copy: "复制",
                shareTitle: "快捷分享",
                shareMore: "更多",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                recordsTitle: "邀请记录",
                noRecords: "暂无邀请记录",
                linkCopied: "邀请链接已复制",
                linkPlaceholder: "暂无链接"
            )
        case .traditionalChinese:
            return .init(
                title: "邀請獎勵",
                summaryTitle: "邀請概覽",
                summaryHint: "分享你的專屬連結，讓好友加入並獲得獎勵。",
                rewardLabel: "累計獎勵",
                invitesSummary: "邀請人數：%d",
                rewardSummary: "累計獎勵：%@",
                linkTitle: "邀請連結",
                linkHint: "複製後分享給好友",
                copy: "複製",
                shareTitle: "快速分享",
                shareMore: "更多",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                recordsTitle: "邀請記錄",
                noRecords: "暫無邀請記錄",
                linkCopied: "邀請連結已複製",
                linkPlaceholder: "暫無連結"
            )
        case .english:
            return .init(
                title: "Referral Rewards",
                summaryTitle: "Overview",
                summaryHint: "Share your personal link and earn rewards when friends join.",
                rewardLabel: "Total Reward",
                invitesSummary: "Invites: %d",
                rewardSummary: "Total Reward: %@",
                linkTitle: "Referral Link",
                linkHint: "Copy and share it with friends",
                copy: "Copy",
                shareTitle: "Quick Share",
                shareMore: "More",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                recordsTitle: "Referral Records",
                noRecords: "No referral records",
                linkCopied: "Referral link copied",
                linkPlaceholder: "No link yet"
            )
        }
    }
}
