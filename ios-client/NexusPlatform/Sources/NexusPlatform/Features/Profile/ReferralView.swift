import SwiftUI
import UIKit

@MainActor
final class ReferralViewModel: ObservableObject {
    @Published private(set) var summary: ReferralSummary?
    @Published private(set) var records: [ReferralRecord] = []
    @Published private(set) var message: String?

    private let service: ReferralServiceProtocol

    init(service: ReferralServiceProtocol = ReferralService()) {
        self.service = service
    }

    func load() {
        Task {
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
    @State private var tip: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    metric(title: "邀请人数", value: "\(viewModel.summary?.inviteCount ?? 0)")
                    metric(title: "累计奖励", value: viewModel.summary?.totalReward ?? "0")
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("邀请链接")
                        .font(.headline)
                    Text(viewModel.summary?.referralLink.isEmpty == false ? (viewModel.summary?.referralLink ?? "") : "暂无链接")
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.auroraBlue)

                    HStack(spacing: 12) {
                        actionButton("复制链接") {
                            UIPasteboard.general.string = viewModel.summary?.referralLink ?? ""
                            tip = "邀请链接已复制"
                        }
                        actionButton("系统分享") {
                            viewModel.markShared("system")
                            UIPasteboard.general.string = viewModel.summary?.referralLink ?? ""
                            tip = "链接已准备好，可直接分享"
                        }
                        actionButton("WhatsApp") {
                            viewModel.markShared("whatsapp")
                            UIPasteboard.general.string = viewModel.summary?.referralLink ?? ""
                            tip = "WhatsApp 分享文案已复制"
                        }
                    }
                }
                .padding(16)
                .nexusGlassCard()

                VStack(alignment: .leading, spacing: 10) {
                    Text("邀请记录")
                        .font(.headline)
                    if viewModel.records.isEmpty {
                        Text("暂无邀请记录")
                            .font(.footnote)
                            .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    } else {
                        ForEach(viewModel.records) { record in
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(record.title)
                                        .font(.subheadline.weight(.semibold))
                                    Text(record.subtitle)
                                        .font(.caption)
                                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                                    Text(record.createdAt)
                                        .font(.caption2)
                                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                                }
                                Spacer()
                                Text(record.reward)
                                    .font(.subheadline.monospacedDigit())
                            }
                            if record.id != viewModel.records.last?.id {
                                Divider()
                            }
                        }
                    }
                }
                .padding(16)
                .nexusGlassCard()

                if let tip {
                    Text(tip)
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                }
                if let message = viewModel.message {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                }
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("邀请奖励")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { viewModel.load() }
    }

    private func metric(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(AppTheme.ColorToken.textSecondary)
            Text(value)
                .font(.title3.bold())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .nexusGlassCard()
    }

    private func actionButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(title, action: action)
            .font(.footnote.weight(.semibold))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(AppTheme.ColorToken.surfaceSecondary, in: Capsule())
    }
}
