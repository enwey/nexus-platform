import SwiftUI
import UIKit

@MainActor
final class BillingDetailViewModel: ObservableObject {
    @Published private(set) var detail: BillingDetail?
    @Published private(set) var isLoading = false
    @Published private(set) var message: String?

    private let service: BillingServiceProtocol

    init(service: BillingServiceProtocol = BillingService()) {
        self.service = service
    }

    func load(id: Int64, fallback: BillingRecord) {
        Task {
            isLoading = true
            defer { isLoading = false }
            do {
                detail = try await service.fetchBillingDetail(id: id)
            } catch {
                detail = BillingDetail(
                    id: fallback.id,
                    type: fallback.type,
                    title: fallback.title,
                    subtitle: fallback.subtitle,
                    amount: fallback.amount,
                    createdAtText: fallback.createdAtText,
                    receiptURL: ""
                )
                message = error.localizedDescription
            }
        }
    }

    func reload(id: Int64, fallback: BillingRecord) {
        message = nil
        load(id: id, fallback: fallback)
    }

    func amountText(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value).doubleValue
        let text = String(format: "%.2f", number)
        return number >= 0 ? "+\(text)" : text
    }
}

struct BillingDetailView: View {
    let record: BillingRecord
    @StateObject private var viewModel = BillingDetailViewModel()
    @State private var linkTip: String?
    @State private var showReceiptPage = false
    @State private var receiptPageURL: URL?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if viewModel.isLoading {
                    ProgressView("加载中...")
                }

                if let detail = viewModel.detail {
                    Text(detail.title)
                        .font(.title3.bold())

                    Text("¥\(viewModel.amountText(detail.amount))")
                        .font(.system(size: 30, weight: .heavy, design: .rounded))
                        .foregroundStyle(
                            viewModel.amountText(detail.amount).hasPrefix("-")
                            ? AppTheme.ColorToken.danger
                            : AppTheme.ColorToken.success
                        )

                    Divider()
                    line("账单ID", "\(detail.id)")
                    line("类型", detail.type.isEmpty ? "-" : detail.type)
                    line("时间", detail.createdAtText.isEmpty ? "-" : detail.createdAtText)
                    line("说明", detail.subtitle.isEmpty ? "-" : detail.subtitle)
                    line("回执", detail.receiptURL.isEmpty ? "暂无" : resolvedReceiptURL(detail.receiptURL))
                    if detail.receiptURL.isEmpty == false {
                        HStack(spacing: 10) {
                            Button("打开回执") {
                                openReceipt(detail.receiptURL)
                            }
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(AppTheme.ColorToken.auroraBlue)

                            Button("复制链接") {
                                UIPasteboard.general.string = resolvedReceiptURL(detail.receiptURL)
                                linkTip = "回执链接已复制"
                            }
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(AppTheme.ColorToken.textSecondary)
                        }
                    }
                }

                if let message = viewModel.message {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    Button("重试") {
                        viewModel.reload(id: record.id, fallback: record)
                    }
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(AppTheme.ColorToken.auroraBlue)
                }

                if let linkTip {
                    Text(linkTip)
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("账单详情")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            viewModel.load(id: record.id, fallback: record)
        }
        .background(
            NavigationLink(
                isActive: $showReceiptPage,
                destination: {
                    if let receiptPageURL {
                        ReceiptPageView(url: receiptPageURL)
                    } else {
                        EmptyView()
                    }
                },
                label: { EmptyView() }
            )
            .hidden()
        )
    }

    private func line(_ key: String, _ value: String) -> some View {
        HStack(alignment: .top) {
            Text(key)
                .font(.footnote)
                .foregroundStyle(AppTheme.ColorToken.textSecondary)
                .frame(width: 56, alignment: .leading)
            Text(value)
                .font(.footnote)
                .foregroundStyle(AppTheme.ColorToken.textPrimary)
            Spacer(minLength: 0)
        }
    }

    private func resolvedReceiptURL(_ raw: String) -> String {
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            return raw
        }
        if raw.hasPrefix("/") {
            let base = BackendEnvironment.current().apiBaseURL
            return URL(string: raw, relativeTo: base)?.absoluteURL.absoluteString ?? raw
        }
        return raw
    }

    private func openReceipt(_ raw: String) {
        guard let url = URL(string: resolvedReceiptURL(raw)) else {
            linkTip = "回执链接无效"
            return
        }
        receiptPageURL = url
        showReceiptPage = true
    }
}
