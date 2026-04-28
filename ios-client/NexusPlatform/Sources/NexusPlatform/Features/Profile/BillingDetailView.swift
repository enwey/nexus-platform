import SwiftUI

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

    var body: some View {
        ZStack(alignment: .topLeading) {
            if let detail = viewModel.detail {
                VStack(alignment: .leading, spacing: 0) {
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
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading)
        .animation(NativeMotion.overlayTransition, value: viewModel.detail != nil)
        .onAppear {
            viewModel.load(id: record.id, fallback: record)
        }
    }

    private var copy: BillingDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    private var billingDetailSkeleton: some View {
        VStack(alignment: .leading, spacing: 0) {
            NativeSkeletonBlock(width: 196, height: 30, cornerRadius: 10)
            Spacer().frame(height: 8)
            NativeSkeletonBlock(width: 228, height: 14, cornerRadius: 7)
            Spacer().frame(height: 8)
            NativeSkeletonBlock(width: 172, height: 14, cornerRadius: 7)
            Spacer().frame(height: 8)
            NativeSkeletonBlock(width: 116, height: 16, cornerRadius: 7)
            Spacer().frame(height: 4)
            NativeSkeletonBlock(width: 104, height: 16, cornerRadius: 7)
            Spacer().frame(height: 4)
            NativeSkeletonBlock(width: 160, height: 16, cornerRadius: 7)
            Spacer().frame(height: 4)
            NativeSkeletonBlock(width: 236, height: 15, cornerRadius: 7)
        }
    }
}

private struct BillingDetailCopy {
    let title: String
    let loading: String
    let empty: String
    let refreshing: String
    let amountFormat: String
    let typeFormat: String
    let timeFormat: String
    let receiptFormat: String

    static func forLanguage(_ language: AppLanguage) -> BillingDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "交易详情",
                loading: "加载详情中...",
                empty: "暂无交易详情",
                refreshing: "正在刷新",
                amountFormat: "金额：%@",
                typeFormat: "类型：%@",
                timeFormat: "时间：%@",
                receiptFormat: "回执：%@"
            )
        case .traditionalChinese:
            return .init(
                title: "交易詳情",
                loading: "載入詳情中...",
                empty: "暫無交易詳情",
                refreshing: "正在刷新",
                amountFormat: "金額：%@",
                typeFormat: "類型：%@",
                timeFormat: "時間：%@",
                receiptFormat: "回執：%@"
            )
        case .english:
            return .init(
                title: "Transaction Detail",
                loading: "Loading detail...",
                empty: "No transaction detail",
                refreshing: "Refreshing",
                amountFormat: "Amount: %@",
                typeFormat: "Type: %@",
                timeFormat: "Time: %@",
                receiptFormat: "Receipt: %@"
            )
        }
    }
}
