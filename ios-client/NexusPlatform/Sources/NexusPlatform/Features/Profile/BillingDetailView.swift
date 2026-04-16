import SwiftUI

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

    func amountText(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value).doubleValue
        return String(format: "%.2f", number)
    }
}

struct BillingDetailView: View {
    let record: BillingRecord

    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = BillingDetailViewModel()

    var body: some View {
        VStack(spacing: 0) {
            header

            VStack(alignment: .leading, spacing: 0) {
                if let detail = viewModel.detail {
                    Text(detail.title)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(.white)

                    Spacer()
                        .frame(height: 8)

                    Text(detail.subtitle)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))

                    Spacer()
                        .frame(height: 8)

                    Text(String(format: copy.amountFormat, viewModel.amountText(detail.amount)))
                        .font(.system(size: 15))
                        .foregroundStyle(.white)
                    Text(String(format: copy.typeFormat, detail.type))
                        .font(.system(size: 15))
                        .foregroundStyle(.white)
                        .padding(.top, 4)
                    Text(String(format: copy.timeFormat, detail.createdAtText))
                        .font(.system(size: 15))
                        .foregroundStyle(.white)
                        .padding(.top, 4)
                    Text(String(format: copy.receiptFormat, detail.receiptURL.isEmpty ? "-" : detail.receiptURL))
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .padding(.top, 4)
                } else if viewModel.isLoading {
                    Text(copy.empty)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                } else {
                    Text(copy.empty)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                }

                if let message = viewModel.message {
                    Text(message)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .padding(.top, 12)
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            viewModel.load(id: record.id, fallback: record)
        }
    }

    private var header: some View {
        VStack(spacing: 0) {
            Spacer()
                .frame(height: 20)

            HStack(spacing: 0) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                }
                .buttonStyle(.plain)

                Text(copy.title)
                    .font(.system(size: 28, weight: .heavy))
                    .foregroundStyle(.white)

                Spacer()
            }
            .padding(.horizontal, 24)

            Spacer()
                .frame(height: 20)
        }
    }

    private var copy: BillingDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct BillingDetailCopy {
    let title: String
    let empty: String
    let amountFormat: String
    let typeFormat: String
    let timeFormat: String
    let receiptFormat: String

    static func forLanguage(_ language: AppLanguage) -> BillingDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "交易详情",
                empty: "暂无交易详情",
                amountFormat: "金额：%@",
                typeFormat: "类型：%@",
                timeFormat: "时间：%@",
                receiptFormat: "回执：%@"
            )
        case .traditionalChinese:
            return .init(
                title: "交易詳情",
                empty: "暫無交易詳情",
                amountFormat: "金額：%@",
                typeFormat: "類型：%@",
                timeFormat: "時間：%@",
                receiptFormat: "回執：%@"
            )
        case .english:
            return .init(
                title: "Transaction Detail",
                empty: "No transaction detail",
                amountFormat: "Amount: %@",
                typeFormat: "Type: %@",
                timeFormat: "Time: %@",
                receiptFormat: "Receipt: %@"
            )
        }
    }
}
