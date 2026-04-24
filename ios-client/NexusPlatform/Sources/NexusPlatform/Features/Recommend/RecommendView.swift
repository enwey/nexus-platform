import SwiftUI

struct RecommendView: View {
    @StateObject private var viewModel = RecommendViewModel()

    var body: some View {
        ScrollView(showsIndicators: false) {
            LazyVStack(spacing: 20) {
                if let error = viewModel.errorMessage, error.isEmpty == false {
                    errorBanner(error)
                }

                if viewModel.isLoading && viewModel.items.isEmpty {
                    ForEach(0..<3, id: \.self) { _ in
                        RecommendSkeletonCard()
                    }
                } else if viewModel.items.isEmpty {
                    NativeStateCard {
                        Text(copy.empty)
                            .font(.system(size: 14))
                            .foregroundStyle(Color(hex: 0xA0A0A0))
                    }
                } else {
                    ForEach(viewModel.items) { item in
                        NavigationLink(
                            destination: RecommendDetailView(
                                item: item,
                                game: resolvedGame(for: item)
                            )
                        ) {
                            RecommendCard(item: item, copy: copy, isRefreshing: viewModel.isLoading)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 96)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { viewModel.load() }
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading && viewModel.items.isEmpty == false)
        .overlay(alignment: .top) {
            if viewModel.isLoading && viewModel.items.isEmpty == false {
                NativeRefreshPill(text: copy.refreshing)
                    .padding(.top, 10)
            }
        }
    }

    private func errorBanner(_ text: String) -> some View {
        NativeStateCard {
            Text(text)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Color(hex: 0xFFB3B3))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func resolvedGame(for item: RecommendTodayItem) -> Game {
        if let game = viewModel.game(for: item) {
            return game
        }
        return Game(
            id: item.appID.isEmpty ? UUID().uuidString : item.appID,
            name: item.gameName,
            description: item.articleBody.isEmpty ? item.cardTitle : item.articleBody,
            iconUrl: item.gameIconURL,
            downloadUrl: "",
            version: "1.0.0",
            md5: "",
            category: item.gameCategory
        )
    }

    private var copy: RecommendCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct RecommendSkeletonCard: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 32, style: .continuous)
            .fill(
                LinearGradient(
                    colors: [Color(hex: 0x24253A), Color(hex: 0x1A1A1D)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(height: 420)
            .overlay {
                VStack(alignment: .leading, spacing: 0) {
                    NativeSkeletonBlock(width: 74, height: 12, cornerRadius: 6)

                    Spacer()

                    VStack(alignment: .leading, spacing: 16) {
                        NativeSkeletonText(widths: [210, 168], lineHeight: 22, spacing: 10, cornerRadius: 8)

                        HStack(spacing: 10) {
                            NativeSkeletonBlock(width: 44, height: 44, cornerRadius: 10)

                            VStack(alignment: .leading, spacing: 8) {
                                NativeSkeletonBlock(width: 96, height: 12, cornerRadius: 6)
                                NativeSkeletonBlock(width: 54, height: 10, cornerRadius: 5)
                            }

                            Spacer()

                            NativeSkeletonBlock(width: 84, height: 32, cornerRadius: 16)
                        }
                        .padding(10)
                        .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                }
                .padding(22)
            }
            .overlay(
                RoundedRectangle(cornerRadius: 32, style: .continuous)
                    .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
            )
    }
}

private struct RecommendCard: View {
    let item: RecommendTodayItem
    let copy: RecommendCopy
    var isRefreshing: Bool = false

    var body: some View {
        ZStack {
            AsyncImage(url: URL(string: item.coverURL)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Rectangle().fill(
                    LinearGradient(
                        colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
            }

            LinearGradient(
                colors: [Color.black.opacity(0.28), .clear, Color.black.opacity(0.75)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 0) {
                Text(item.cardCategory.isEmpty ? copy.defaultCategory : item.cardCategory)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(.white.opacity(0.8))

                Spacer()

                VStack(alignment: .leading, spacing: 16) {
                    Text(item.cardTitle.isEmpty ? item.gameName : item.cardTitle)
                        .font(.system(size: 30, weight: .black))
                        .foregroundStyle(.white)

                    HStack(spacing: 10) {
                        AsyncImage(url: URL(string: item.gameIconURL)) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(Color(hex: 0x1C1C1F))
                        }
                        .frame(width: 44, height: 44)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.gameName)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                            Text(item.gameCategory.isEmpty ? copy.defaultGameCategory : item.gameCategory)
                                .font(.system(size: 11))
                                .foregroundStyle(.white.opacity(0.7))
                        }

                        Spacer()

                        Text(item.actionText.isEmpty ? copy.play : item.actionText)
                            .font(.system(size: 13, weight: .heavy))
                            .foregroundStyle(.black)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    }
                    .padding(10)
                    .background(Color.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
            }
            .padding(22)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 420)
        .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
        .overlay {
            if isRefreshing {
                NativeSectionRefreshOverlay(lineWidths: [90, 60], cornerRadius: 32)
            }
        }
        .animation(NativeMotion.overlayTransition, value: isRefreshing)
    }
}

private struct RecommendCopy {
    let title: String
    let loading: String
    let refreshing: String
    let empty: String
    let defaultCategory: String
    let defaultGameCategory: String
    let play: String

    static func forLanguage(_ language: AppLanguage) -> RecommendCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "推荐", loading: "加载中...", refreshing: "正在刷新", empty: "今天还没有新的推荐", defaultCategory: "今日推荐", defaultGameCategory: "全部", play: "立即秒开")
        case .traditionalChinese:
            return .init(title: "推薦", loading: "載入中...", refreshing: "正在刷新", empty: "今天還沒有新的推薦", defaultCategory: "今日推薦", defaultGameCategory: "全部", play: "立即秒開")
        case .english:
            return .init(title: "Recommend", loading: "Loading...", refreshing: "Refreshing", empty: "No new recommendations today", defaultCategory: "Today's Pick", defaultGameCategory: "All", play: "Play Now")
        }
    }
}
