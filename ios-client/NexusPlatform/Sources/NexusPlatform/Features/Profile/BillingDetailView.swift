import SwiftUI
import UIKit

@MainActor
final class BillingDetailViewModel: ObservableObject {
    private static var cache: [Int64: BillingDetail] = [:]

    @Published private(set) var detail: BillingDetail?
    @Published private(set) var isLoading = false
    @Published private(set) var message: String?

    private let service: BillingServiceProtocol

    init(service: BillingServiceProtocol = BillingService()) {
        self.service = service
    }

    func load(id: Int64, fallback: BillingRecord) {
        if let cached = Self.cache[id] {
            detail = cached
        } else if detail == nil {
            detail = BillingDetail(
                id: fallback.id,
                type: fallback.type,
                title: fallback.title,
                subtitle: fallback.subtitle,
                amount: fallback.amount,
                createdAtText: fallback.createdAtText,
                receiptURL: ""
            )
        }

        Task {
            isLoading = true
            defer { isLoading = false }
            do {
                let loaded = try await service.fetchBillingDetail(id: id)
                detail = loaded
                Self.cache[id] = loaded
            } catch {
                message = error.localizedDescription
            }
        }
    }

    func amountText(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value).doubleValue
        return String(format: "%.2f", number)
    }
}

struct BillingDetailView: View {
    let record: BillingRecord

    @StateObject private var viewModel = BillingDetailViewModel()
    @State private var toastMessage: String?

    var body: some View {
        ZStack(alignment: .topLeading) {
            if let detail = viewModel.detail {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        detailHeader(detail)

                        Spacer().frame(height: 24)

                        detailReceiptCard(detail)

                        Spacer().frame(height: 30)

                        HStack(spacing: 12) {
                            secondaryActionButton(copy.copyID) {
                                UIPasteboard.general.string = orderNumber(detail)
                                toastMessage = copy.copied
                            }

                            primaryActionButton(copy.reportAction) {
                                presentActivitySheet(items: [reportText(detail)])
                            }
                        }

                        Spacer().frame(height: 24)

                        Text(copy.footer)
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: 0x4B4B50))
                            .tracking(2)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 12)
                    .padding(.bottom, 96)
                }
                .transition(NativeMotion.contentRevealTransition)
            } else if viewModel.isLoading {
                billingDetailSkeleton
                    .transition(NativeMotion.stateSwapTransition)
            } else {
                NativeStateCard {
                    Text(copy.empty)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                }
                .transition(NativeMotion.stateSwapTransition)
            }

            if let message = viewModel.message {
                NativeStateCard {
                    Text(message)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 12)
            }

            if viewModel.isLoading && viewModel.detail != nil {
                NativeSectionRefreshOverlay(lineWidths: [88, 60], cornerRadius: 18)
                    .padding(.top, 12)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(.horizontal, 24)
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading)
        .animation(NativeMotion.overlayTransition, value: viewModel.detail != nil)
        .onAppear {
            viewModel.load(id: record.id, fallback: record)
        }
        .overlay(alignment: .bottom) {
            NativeToastOverlay(message: $toastMessage)
        }
    }

    private var copy: BillingDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    private var billingDetailSkeleton: some View {
        VStack(spacing: 24) {
            NativeSkeletonBlock(height: 220, cornerRadius: 24)
            NativeSkeletonBlock(height: 260, cornerRadius: 24)
        }
        .padding(.horizontal, 24)
        .padding(.top, 12)
    }

    private func detailHeader(_ detail: BillingDetail) -> some View {
        VStack(spacing: 8) {
            Circle()
                .fill(Color(hex: 0x1C1C1F))
                .frame(width: 72, height: 72)
                .overlay(
                    Text(headerIcon(detail))
                        .font(.system(size: 32))
                )
                .overlay(
                    Circle()
                        .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                )

            Text(copy.amountTitle)
                .font(.system(size: 15))
                .foregroundStyle(Color(hex: 0xA0A0A0))

            Text(amountText(detail.amount))
                .font(.system(size: 40, weight: .black, design: .monospaced))
                .foregroundStyle(detail.amount < 0 ? .white : Color(hex: 0x36C282))

            Text(copy.successStatus)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color(hex: 0x36C282))
                .padding(.horizontal, 12)
                .padding(.vertical, 4)
                .background(Color(hex: 0x36C282).opacity(0.12), in: Capsule())
        }
    }

    private func detailReceiptCard(_ detail: BillingDetail) -> some View {
        VStack(spacing: 20) {
            detailRow(copy.typeLabel, detail.type)
            detailRow(copy.nameLabel, detail.title)

            Rectangle()
                .fill(Color.clear)
                .frame(height: 1)
                .overlay(
                    Rectangle()
                        .stroke(style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
                        .foregroundStyle(Color(hex: 0x2D2D31))
                )

            detailRow(copy.timeLabel, detail.createdAtText)
            detailRow(copy.orderLabel, orderNumber(detail))
            detailRow(copy.receiptLabel, detail.receiptURL.isEmpty ? "-" : detail.receiptURL)
        }
        .padding(24)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private func detailRow(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(label)
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: 0xA0A0A0))
            Spacer()
            Text(value.isEmpty ? "-" : value)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.white)
                .multilineTextAlignment(.trailing)
        }
    }

    private func secondaryActionButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.clear, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private func primaryActionButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func orderNumber(_ detail: BillingDetail) -> String {
        "NX\(detail.id)"
    }

    private func amountText(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value).doubleValue
        let prefix = number >= 0 ? "+" : "-"
        return "\(prefix)\(String(format: "%.0f", abs(number)))"
    }

    private func headerIcon(_ detail: BillingDetail) -> String {
        let value = "\(detail.title) \(detail.type)".lowercased()
        if value.contains("邀请") || value.contains("invite") {
            return "🤝"
        }
        if value.contains("购买") || value.contains("buy") {
            return "🎮"
        }
        return detail.amount >= 0 ? "🪙" : "🧾"
    }

    private func reportText(_ detail: BillingDetail) -> String {
        [
            copy.reportTemplateTitle,
            "\(copy.orderLabel): \(orderNumber(detail))",
            "\(copy.nameLabel): \(detail.title)",
            "\(copy.amountTitle): \(amountText(detail.amount))",
            "\(copy.timeLabel): \(detail.createdAtText)"
        ].joined(separator: "\n")
    }

    private func presentActivitySheet(items: [Any]) {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController?
            .present(controller, animated: true)
    }
}

private struct BillingDetailCopy {
    let title: String
    let loading: String
    let empty: String
    let refreshing: String
    let amountTitle: String
    let successStatus: String
    let typeLabel: String
    let nameLabel: String
    let timeLabel: String
    let orderLabel: String
    let receiptLabel: String
    let copyID: String
    let reportAction: String
    let copied: String
    let reportTemplateTitle: String
    let footer: String

    static func forLanguage(_ language: AppLanguage) -> BillingDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "交易详情",
                loading: "加载详情中...",
                empty: "暂无交易详情",
                refreshing: "正在刷新",
                amountTitle: "交易金额",
                successStatus: "交易成功",
                typeLabel: "交易类型",
                nameLabel: "商品名称",
                timeLabel: "交易时间",
                orderLabel: "交易单号",
                receiptLabel: "回执地址",
                copyID: "复制单号",
                reportAction: "对此交易有疑问?",
                copied: "交易单号已复制",
                reportTemplateTitle: "交易投诉与反馈",
                footer: "NEXUS RUNTIME SECURED"
            )
        case .traditionalChinese:
            return .init(
                title: "交易詳情",
                loading: "載入詳情中...",
                empty: "暫無交易詳情",
                refreshing: "正在刷新",
                amountTitle: "交易金額",
                successStatus: "交易成功",
                typeLabel: "交易類型",
                nameLabel: "商品名稱",
                timeLabel: "交易時間",
                orderLabel: "交易單號",
                receiptLabel: "回執地址",
                copyID: "複製單號",
                reportAction: "對此交易有疑問?",
                copied: "交易單號已複製",
                reportTemplateTitle: "交易投訴與回饋",
                footer: "NEXUS RUNTIME SECURED"
            )
        case .english:
            return .init(
                title: "Transaction Detail",
                loading: "Loading detail...",
                empty: "No transaction detail",
                refreshing: "Refreshing",
                amountTitle: "Transaction Amount",
                successStatus: "Completed",
                typeLabel: "Type",
                nameLabel: "Item",
                timeLabel: "Time",
                orderLabel: "Order No.",
                receiptLabel: "Receipt URL",
                copyID: "Copy ID",
                reportAction: "Question About This Transaction?",
                copied: "Order number copied",
                reportTemplateTitle: "Transaction Support Request",
                footer: "NEXUS RUNTIME SECURED"
            )
        }
    }
}
