import SwiftUI

struct RecommendView: View {
    @StateObject private var viewModel = RecommendViewModel()
    @State private var hasLoaded = false
    @State private var language: AppLanguage = AppLanguageStore.currentSync()

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
                        let game = resolvedGame(for: item)
                        ZStack(alignment: .bottomTrailing) {
                            NavigationLink(
                                destination: RecommendDetailView(
                                    item: item,
                                    game: game
                                )
                            ) {
                                RecommendCard(
                                    item: item,
                                    game: game,
                                    copy: copy,
                                    language: language,
                                    isRefreshing: viewModel.isLoading
                                )
                            }
                            .buttonStyle(.plain)

                            AuthGateLaunchLink(game: game) {
                                Text(copy.play)
                                    .font(.system(size: 12, weight: .black))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(
                                        LinearGradient(
                                            colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        ),
                                        in: Capsule()
                                    )
                            }
                            .buttonStyle(.plain)
                            .padding(.trailing, 32)
                            .padding(.bottom, 32)
                        }
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 24)
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
        .onReceive(NotificationCenter.default.publisher(for: AppLanguageStore.didChangeNotification)) { notification in
            if let language = notification.object as? AppLanguage {
                self.language = language
            } else {
                language = AppLanguageStore.currentSync()
            }
        }
        .animation(NativeMotion.overlayTransition, value: viewModel.isLoading && viewModel.items.isEmpty == false)
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
        ZStack {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )

            LinearGradient(
                colors: [Color.black.opacity(0.28), .clear, Color.black.opacity(0.75)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 0) {
                NativeSkeletonBlock(width: 82, height: 13, cornerRadius: 6)

                Spacer()

                VStack(alignment: .leading, spacing: 16) {
                    NativeSkeletonBlock(width: 236, height: 36, cornerRadius: 10)

                    HStack(spacing: 10) {
                        NativeSkeletonBlock(width: 44, height: 44, cornerRadius: 10)

                        VStack(alignment: .leading, spacing: 4) {
                            NativeSkeletonBlock(width: 102, height: 15, cornerRadius: 6)
                            NativeSkeletonBlock(width: 54, height: 11, cornerRadius: 5)
                        }

                        Spacer()

                        NativeSkeletonBlock(width: 84, height: 32, cornerRadius: 18)
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
    }
}

private struct RecommendCard: View {
    let item: RecommendTodayItem
    let game: Game
    let copy: RecommendCopy
    let language: AppLanguage
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
                            Text(localizedGameName)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                            Text(item.gameCategory.isEmpty ? copy.defaultGameCategory : item.gameCategory)
                                .font(.system(size: 11))
                                .foregroundStyle(.white.opacity(0.7))
                        }

                        Spacer()
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

    private var localizedGameName: String {
        game.localizedName(for: language)
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
            return .init(title: "推荐", loading: "加载中...", refreshing: "正在刷新", empty: "今天还没有新的推荐", defaultCategory: "今日推荐", defaultGameCategory: "全部", play: "秒开")
        case .traditionalChinese:
            return .init(title: "推薦", loading: "載入中...", refreshing: "正在刷新", empty: "今天還沒有新的推薦", defaultCategory: "今日推薦", defaultGameCategory: "全部", play: "秒開")
        case .english:
            return .init(title: "Recommend", loading: "Loading...", refreshing: "Refreshing", empty: "No new recommendations today", defaultCategory: "Today's Pick", defaultGameCategory: "All", play: "Play")
        }
    }
}
