import SwiftUI

struct DiscoverView: View {
    @StateObject private var viewModel = DiscoverViewModel()

    var body: some View {
        ScrollView(showsIndicators: false) {
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
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { viewModel.load() }
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

    static func forLanguage(_ language: AppLanguage) -> DiscoverCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "发现", rankTitle: "排行榜", viewMore: "查看更多", empty: "暂无内容", loadEnd: "已经到底啦", hot: "热门")
        case .traditionalChinese:
            return .init(title: "發現", rankTitle: "排行榜", viewMore: "查看更多", empty: "暫無內容", loadEnd: "已經到底了", hot: "熱門")
        case .english:
            return .init(title: "Discover", rankTitle: "Ranking", viewMore: "View More", empty: "No content yet", loadEnd: "You've reached the end", hot: "Hot")
        }
    }
}
