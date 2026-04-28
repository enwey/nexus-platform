import SwiftUI

struct DiscoverView: View {
    @StateObject private var viewModel = DiscoverViewModel()
    @State private var hasLoaded = false
    @State private var language: AppLanguage = AppLanguageStore.currentSync()

    var body: some View {
        ScrollView(showsIndicators: false) {
            if viewModel.isLoading && viewModel.games.isEmpty {
                discoverSkeleton
            } else {
                VStack(alignment: .leading, spacing: 20) {
                    Spacer()
                        .frame(height: 24)

                    bannerSection
                        .padding(.horizontal, 24)

                    categorySection
                        .padding(.horizontal, 24)

                    if let error = viewModel.errorMessage, error.isEmpty == false {
                        errorBanner(error)
                            .padding(.horizontal, 24)
                    }

                    feedSection
                        .padding(.horizontal, 24)
                        .padding(.bottom, 96)
                }
            }
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
        .animation(NativeMotion.overlayTransition, value: isRefreshingContent)
    }

    private var isRefreshingContent: Bool {
        viewModel.isLoading && (viewModel.games.isEmpty == false || viewModel.topRanked.isEmpty == false)
    }

    private var discoverSkeleton: some View {
        VStack(alignment: .leading, spacing: 20) {
            Spacer().frame(height: 14)

            heroSkeleton
                .padding(.horizontal, 24)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<5, id: \.self) { index in
                        NativeSkeletonBlock(width: index == 0 ? 56 : 72, height: 34, cornerRadius: 17)
                    }
                }
                .padding(.horizontal, 24)
            }

            skeletonFeedSection
                .padding(.horizontal, 24)
                .padding(.bottom, 96)
        }
    }

    private var heroSkeleton: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )

            Rectangle()
                .fill(Color.black.opacity(0.28))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))

            VStack(alignment: .leading, spacing: 10) {
                NativeSkeletonBlock(width: 54, height: 19, cornerRadius: 8)
                Spacer()
                NativeSkeletonBlock(width: 188, height: 34, cornerRadius: 10)
                NativeSkeletonText(widths: [196, 168], lineHeight: 14, spacing: 6, cornerRadius: 7)
            }
            .padding(20)
        }
        .frame(height: 180)
    }

    private var skeletonFeedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            NativeSkeletonBlock(width: 84, height: 28, cornerRadius: 8)
            ForEach(0..<4, id: \.self) { _ in
                discoverRowSkeleton
            }
        }
    }

    private var discoverRowSkeleton: some View {
        HStack(spacing: 12) {
            NativeSkeletonBlock(width: 52, height: 52, cornerRadius: 12)

            VStack(alignment: .leading, spacing: 6) {
                NativeSkeletonBlock(width: 136, height: 16, cornerRadius: 7)
                NativeSkeletonBlock(width: 144, height: 12, cornerRadius: 6)
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
            ZStack(alignment: .topLeading) {
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

                    Spacer()

                    VStack(alignment: .leading, spacing: 10) {
                        Text(viewModel.hero?.title ?? heroTargetGame.localizedName(for: language))
                            .font(.system(size: 30, weight: .black))
                            .foregroundStyle(.white)

                        Text(heroSubtitle)
                            .font(.system(size: 14))
                            .foregroundStyle(.white.opacity(0.9))
                            .lineLimit(2)
                    }
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

    private func discoverListItem(game: Game) -> some View {
        HStack(spacing: 12) {
            NavigationLink(destination: GameDetailView(game: game)) {
                HStack(spacing: 12) {
                    AsyncImage(url: URL(string: game.iconUrl)) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(Color(hex: 0x232326))
                    }
                    .frame(width: 56, height: 56)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                    )

                    VStack(alignment: .leading, spacing: 6) {
                        Text(game.localizedName(for: language))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(.white)
                            .lineLimit(1)

                        Text(localizedGameDescription(for: game))
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: 0xA0A0A0))
                            .lineLimit(1)
                    }

                    Spacer(minLength: 0)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            AuthGateLaunchLink(game: game) {
                Text(copy.quickPlay)
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
        }
        .padding(16)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private var feedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            if viewModel.filteredGames.isEmpty {
                Text(copy.empty)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
            } else {
                ForEach(viewModel.filteredGames) { game in
                    discoverListItem(game: game)
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

    private var heroSubtitle: String {
        if let subtitle = viewModel.hero?.subtitle, subtitle.isEmpty == false {
            return subtitle
        }
        return localizedGameDescription(for: heroTargetGame)
    }

    private func localizedGameDescription(for game: Game) -> String {
        let localizedDescription = game.localizedDescription(for: language)
        return localizedDescription.isEmpty ? "v\(game.version)" : localizedDescription
    }
}

private struct DiscoverCopy {
    let title: String
    let empty: String
    let hot: String
    let refreshing: String
    let quickPlay: String

    static func forLanguage(_ language: AppLanguage) -> DiscoverCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "发现", empty: "暂无内容", hot: "热门", refreshing: "正在刷新", quickPlay: "秒开")
        case .traditionalChinese:
            return .init(title: "發現", empty: "暫無內容", hot: "熱門", refreshing: "正在刷新", quickPlay: "秒開")
        case .english:
            return .init(title: "Discover", empty: "No content yet", hot: "Hot", refreshing: "Refreshing", quickPlay: "Play")
        }
    }
}
