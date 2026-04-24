import SwiftUI

struct DiscoverView: View {
    @StateObject private var viewModel = DiscoverViewModel()

    var body: some View {
        ScrollView(showsIndicators: false) {
            if viewModel.isLoading && viewModel.games.isEmpty && viewModel.topRanked.isEmpty {
                discoverSkeleton
            } else {
                VStack(alignment: .leading, spacing: 20) {
                    Spacer()
                        .frame(height: 12)

                    bannerSection
                        .padding(.horizontal, 24)

                    categorySection
                        .padding(.horizontal, 24)

                    if let error = viewModel.errorMessage, error.isEmpty == false {
                        errorBanner(error)
                            .padding(.horizontal, 24)
                    }

                    if viewModel.selectedCategory == "全部" {
                        rankingSection
                            .padding(.horizontal, 24)
                    }

                    feedSection
                        .padding(.horizontal, 24)

                    footer
                        .padding(.horizontal, 24)
                        .padding(.bottom, 96)
                }
            }
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { viewModel.load() }
        .animation(NativeMotion.overlayTransition, value: isRefreshingContent)
        .overlay(alignment: .top) {
            if viewModel.isLoading && (viewModel.games.isEmpty == false || viewModel.topRanked.isEmpty == false) {
                NativeRefreshPill(text: copy.refreshing)
                    .padding(.top, 10)
            }
        }
    }

    private var isRefreshingContent: Bool {
        viewModel.isLoading && (viewModel.games.isEmpty == false || viewModel.topRanked.isEmpty == false)
    }

    private var discoverSkeleton: some View {
        VStack(alignment: .leading, spacing: 20) {
            Spacer().frame(height: 14)

            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x24253A), Color(hex: 0x1C1C1F)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(height: 180)
                .overlay(alignment: .bottomLeading) {
                    VStack(alignment: .leading, spacing: 10) {
                        NativeSkeletonBlock(width: 52, height: 20, cornerRadius: 8)
                        NativeSkeletonText(widths: [184, 228], lineHeight: 14)
                    }
                    .padding(20)
                }
                .padding(.horizontal, 24)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<5, id: \.self) { index in
                        NativeSkeletonBlock(width: index == 0 ? 56 : 72, height: 34, cornerRadius: 17)
                    }
                }
                .padding(.horizontal, 24)
            }

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    NativeSkeletonBlock(width: 102, height: 24, cornerRadius: 8)
                    Spacer()
                    NativeSkeletonBlock(width: 62, height: 12, cornerRadius: 6)
                }

                ForEach(0..<3, id: \.self) { index in
                    discoverRowSkeleton(rank: index + 1)
                }
            }
            .padding(.horizontal, 24)

            VStack(alignment: .leading, spacing: 12) {
                NativeSkeletonBlock(width: 78, height: 24, cornerRadius: 8)
                ForEach(0..<4, id: \.self) { index in
                    discoverRowSkeleton(rank: index + 1)
                }
            }
            .padding(.horizontal, 24)

            footer
                .padding(.horizontal, 24)
                .padding(.bottom, 96)
        }
    }

    private func discoverRowSkeleton(rank: Int) -> some View {
        HStack(spacing: 12) {
            Text("\(rank)")
                .font(.system(size: 22, weight: .heavy))
                .foregroundStyle(Color.white.opacity(0.12))
                .frame(width: 28, height: 28)

            NativeSkeletonBlock(width: 52, height: 52, cornerRadius: 12)

            VStack(alignment: .leading, spacing: 6) {
                NativeSkeletonBlock(width: 128, height: 14, cornerRadius: 7)
                NativeSkeletonBlock(width: 166, height: 12, cornerRadius: 6)
            }

            Spacer()
        }
        .padding(12)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private var bannerSection: some View {
        NavigationLink(destination: GameDetailView(game: heroTargetGame)) {
            ZStack(alignment: .bottomLeading) {
                if
                    let hero = viewModel.hero,
                    let url = URL(string: hero.coverURL),
                    hero.coverURL.isEmpty == false
                {
                    AsyncImage(url: url) { image in
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
                } else {
                    Rectangle().fill(
                        LinearGradient(
                            colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                }

                Rectangle()
                    .fill(Color.black.opacity(0.28))

                VStack(alignment: .leading, spacing: 10) {
                    Text(viewModel.hero?.badgeText.isEmpty == false ? (viewModel.hero?.badgeText ?? copy.hot) : copy.hot)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.black.opacity(0.5), in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                    Text(viewModel.hero?.title ?? heroTargetGame.name)
                        .font(.system(size: 30, weight: .black))
                        .foregroundStyle(.white)

                    Text(viewModel.hero?.subtitle ?? heroTargetGame.description)
                        .font(.system(size: 14))
                        .foregroundStyle(.white.opacity(0.9))
                        .lineLimit(2)
                }
                .padding(20)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 180)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                if isRefreshingContent {
                    NativeSectionRefreshOverlay(lineWidths: [108, 72], cornerRadius: 20)
                }
            }
            .animation(NativeMotion.overlayTransition, value: isRefreshingContent)
        }
        .buttonStyle(.plain)
    }

    private var categorySection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(viewModel.categories.enumerated()), id: \.offset) { _, category in
                    let selected = category == viewModel.selectedCategory
                    Button(category) {
                        viewModel.selectCategory(category)
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(selected ? .white : Color(hex: 0xF2F2F7))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(
                        selected
                        ? LinearGradient(colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)], startPoint: .leading, endPoint: .trailing)
                        : LinearGradient(colors: [Color(hex: 0x232326), Color(hex: 0x232326)], startPoint: .leading, endPoint: .trailing)
                    )
                    .clipShape(Capsule())
                }
            }
        }
    }

    private var rankingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(copy.rankTitle)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.white)
                Spacer()
                NavigationLink(destination: DiscoverRankingView(games: viewModel.topRanked)) {
                    Text(copy.viewMore)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color(hex: 0x6B4EFF))
                }
                .buttonStyle(.plain)
            }

            if viewModel.topRanked.isEmpty {
                Text(copy.empty)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
            } else {
                ForEach(viewModel.topRanked.prefix(10)) { game in
                    rankedItem(game: game, rank: (viewModel.topRanked.firstIndex(where: { $0.id == game.id }) ?? 0) + 1)
                }
            }
        }
        .overlay {
            if isRefreshingContent {
                NativeSectionRefreshOverlay(lineWidths: [96, 58], cornerRadius: 18)
            }
        }
        .animation(NativeMotion.overlayTransition, value: isRefreshingContent)
    }

    private func rankedItem(game: Game, rank: Int) -> some View {
        NavigationLink(destination: GameDetailView(game: game)) {
            HStack(spacing: 12) {
                Text("\(rank)")
                    .font(.system(size: 22, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)

                AsyncImage(url: URL(string: game.iconUrl)) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color(hex: 0x232326))
                }
                .frame(width: 52, height: 52)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: 4) {
                    Text(game.name)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                    Text(game.description.isEmpty ? "v\(game.version)" : game.description)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .lineLimit(1)
                }

                Spacer()
            }
            .padding(12)
            .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var feedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            if viewModel.filteredGames.isEmpty {
                Text(copy.empty)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
            } else {
                Text(viewModel.selectedCategory)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.white)

                ForEach(viewModel.filteredGames) { game in
                    rankedItem(game: game, rank: (viewModel.filteredGames.firstIndex(where: { $0.id == game.id }) ?? 0) + 1)
                }
            }
        }
        .overlay {
            if isRefreshingContent {
                NativeSectionRefreshOverlay(lineWidths: [84, 54], cornerRadius: 18)
            }
        }
        .animation(NativeMotion.overlayTransition, value: isRefreshingContent)
    }

    private var footer: some View {
        Text(copy.loadEnd)
            .font(.system(size: 12))
            .foregroundStyle(Color(hex: 0xA0A0A0))
            .frame(maxWidth: .infinity, alignment: .center)
    }

    private func errorBanner(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(Color(hex: 0xFFB3B3))
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: 0x3A1616), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color(hex: 0x7A2C2C), lineWidth: 1)
            )
    }

    private var heroTargetGame: Game {
        viewModel.heroGame ?? viewModel.topRanked.first ?? viewModel.games.first ?? Game(
            id: UUID().uuidString,
            name: "推荐游戏",
            description: "",
            iconUrl: "",
            downloadUrl: "",
            version: "1.0.0",
            md5: "",
            category: "精选"
        )
    }

    private var copy: DiscoverCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct DiscoverCopy {
    let title: String
    let rankTitle: String
    let viewMore: String
    let empty: String
    let loadEnd: String
    let hot: String
    let refreshing: String

    static func forLanguage(_ language: AppLanguage) -> DiscoverCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "发现", rankTitle: "排行榜", viewMore: "查看更多", empty: "暂无内容", loadEnd: "已经到底啦", hot: "热门", refreshing: "正在刷新")
        case .traditionalChinese:
            return .init(title: "發現", rankTitle: "排行榜", viewMore: "查看更多", empty: "暫無內容", loadEnd: "已經到底了", hot: "熱門", refreshing: "正在刷新")
        case .english:
            return .init(title: "Discover", rankTitle: "Ranking", viewMore: "View More", empty: "No content yet", loadEnd: "You've reached the end", hot: "Hot", refreshing: "Refreshing")
        }
    }
}
