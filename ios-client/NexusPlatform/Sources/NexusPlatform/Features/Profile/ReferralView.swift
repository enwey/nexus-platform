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
    @State private var toastMessage: String?

    var body: some View {
        ScrollView(showsIndicators: false) {
            ZStack(alignment: .topLeading) {
                if viewModel.isLoading {
                    referralSkeleton
                        .transition(NativeMotion.stateSwapTransition)
                } else {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(String(format: copy.invitesSummary, viewModel.summary?.inviteCount ?? 0))
                            .font(.system(size: 17))
                            .foregroundStyle(.white)

                        Text(String(format: copy.rewardSummary, viewModel.summary?.totalReward ?? "0"))
                            .font(.system(size: 17))
                            .foregroundStyle(.white)
                            .padding(.top, 4)

                        Spacer()
                            .frame(height: 12)

                        HStack(alignment: .top, spacing: 12) {
                            Text(linkText)
                                .font(.system(size: 14))
                                .foregroundStyle(Color(hex: 0x6B4EFF))
                                .frame(maxWidth: .infinity, alignment: .leading)

                            Button(copy.copy) {
                                UIPasteboard.general.string = linkText
                                toastMessage = copy.linkCopied
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(Color(hex: 0x6B4EFF))
                            .font(.system(size: 14, weight: .medium))
                        }

                        Spacer()
                            .frame(height: 12)

                        HStack(spacing: 16) {
                            shareButton(title: copy.shareMore, channel: "system", text: linkText)
                            shareButton(title: copy.shareWhatsApp, channel: "whatsapp", text: linkText)
                            shareButton(title: copy.shareFacebook, channel: "facebook", text: linkText)
                        }

                        Spacer()
                            .frame(height: 16)

                        if viewModel.records.isEmpty {
                            Text(copy.noRecords)
                                .font(.system(size: 14))
                                .foregroundStyle(Color(hex: 0xA0A0A0))
                        } else {
                            ForEach(viewModel.records) { record in
                                HStack(alignment: .top) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(record.title)
                                            .font(.system(size: 16, weight: .semibold))
                                            .foregroundStyle(.white)
                                        Text(record.subtitle)
                                            .font(.system(size: 12))
                                            .foregroundStyle(Color(hex: 0xA0A0A0))
                                        Text(record.createdAt)
                                            .font(.system(size: 12))
                                            .foregroundStyle(Color(hex: 0xA0A0A0))
                                    }
                                    Spacer()
                                    Text(record.reward)
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundStyle(.white)
                                }
                                .padding(.vertical, 8)
                            }
                        }
                    }
                    .transition(NativeMotion.contentRevealTransition)
                }

                if let message = viewModel.message {
                    Text(message)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .padding(.top, 12)
                }
            }
            .padding(.horizontal, 24)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
        .onAppear { viewModel.load() }
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading)
        .animation(NativeMotion.overlayTransition, value: viewModel.records.isEmpty)
        .overlay(alignment: .bottom) {
            NativeToastOverlay(message: $toastMessage)
        }
    }

    private var referralSkeleton: some View {
        VStack(alignment: .leading, spacing: 0) {
            NativeSkeletonBlock(width: 116, height: 16, cornerRadius: 8)
            Spacer().frame(height: 8)
            NativeSkeletonBlock(width: 132, height: 16, cornerRadius: 8)
            Spacer().frame(height: 12)
            HStack(spacing: 12) {
                NativeSkeletonBlock(height: 16, cornerRadius: 8)
                NativeSkeletonBlock(width: 44, height: 16, cornerRadius: 8)
            }
            Spacer().frame(height: 12)
            HStack(spacing: 16) {
                NativeSkeletonBlock(width: 62, height: 16, cornerRadius: 8)
                NativeSkeletonBlock(width: 78, height: 16, cornerRadius: 8)
                NativeSkeletonBlock(width: 72, height: 16, cornerRadius: 8)
            }
            Spacer().frame(height: 20)
            ForEach(0..<4, id: \.self) { _ in
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        NativeSkeletonBlock(width: 142, height: 14, cornerRadius: 7)
                        NativeSkeletonBlock(width: 176, height: 12, cornerRadius: 6)
                        NativeSkeletonBlock(width: 88, height: 12, cornerRadius: 6)
                    }
                    Spacer()
                    NativeSkeletonBlock(width: 46, height: 14, cornerRadius: 7)
                }
                .padding(.vertical, 8)
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

    private func shareButton(title: String, channel: String, text: String) -> some View {
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
        .buttonStyle(.plain)
        .foregroundStyle(Color(hex: 0x6B4EFF))
        .font(.system(size: 14, weight: .medium))
    }
}

private struct ReferralCopy {
    let title: String
    let invitesSummary: String
    let rewardSummary: String
    let copy: String
    let shareMore: String
    let shareWhatsApp: String
    let shareFacebook: String
    let noRecords: String
    let linkCopied: String
    let linkPlaceholder: String

    static func forLanguage(_ language: AppLanguage) -> ReferralCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "邀请奖励",
                invitesSummary: "邀请人数：%d",
                rewardSummary: "累计奖励：%@",
                copy: "复制",
                shareMore: "更多",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                noRecords: "暂无邀请记录",
                linkCopied: "邀请链接已复制",
                linkPlaceholder: "暂无链接"
            )
        case .traditionalChinese:
            return .init(
                title: "邀請獎勵",
                invitesSummary: "邀請人數：%d",
                rewardSummary: "累計獎勵：%@",
                copy: "複製",
                shareMore: "更多",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                noRecords: "暫無邀請記錄",
                linkCopied: "邀請連結已複製",
                linkPlaceholder: "暫無連結"
            )
        case .english:
            return .init(
                title: "Referral Rewards",
                invitesSummary: "Invites: %d",
                rewardSummary: "Total Reward: %@",
                copy: "Copy",
                shareMore: "More",
                shareWhatsApp: "WhatsApp",
                shareFacebook: "Facebook",
                noRecords: "No referral records",
                linkCopied: "Referral link copied",
                linkPlaceholder: "No link yet"
            )
        }
    }
}
