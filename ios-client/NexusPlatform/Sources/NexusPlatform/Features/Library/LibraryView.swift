import SwiftUI

struct LibraryView: View {
    @StateObject private var viewModel = LibraryViewModel()

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 16), count: 4)

    var body: some View {
        ScrollView(showsIndicators: false) {
            if viewModel.isLoading {
                VStack(spacing: 14) {
                    ProgressView()
                    Text(copy.loading)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 120)
            } else {
                VStack(alignment: .leading, spacing: 24) {
                    if hasLibraryContent {
                        resumeCard
                        recentSection

                        if viewModel.myGames.isEmpty {
                            trendingSection
                        } else {
                            myGamesSection
                        }
                    } else {
                        coldStartHero
                        myGamesEmptyCard
                        trendingSection
                    }

                    footer
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)
                .padding(.bottom, 96)
            }
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { viewModel.load() }
    }

    private var hasLibraryContent: Bool {
        viewModel.currentPlaying != nil || !viewModel.recentGames.isEmpty || !viewModel.myGames.isEmpty
    }

    private var resumeCard: some View {
        AuthGateLaunchLink(game: viewModel.currentPlaying ?? fallbackGame) {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: 32, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [Color(hex: 0x5C8CFF), Color(hex: 0x9258FF)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )

                Circle()
                    .fill(Color.white.opacity(0.14))
                    .frame(width: 130, height: 130)
                    .offset(x: -18, y: -60)

                LinearGradient(
                    colors: [.clear, Color.black.opacity(0.30)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))

                VStack(alignment: .leading, spacing: 0) {
                    Text(copy.runningTag)
                        .font(.system(size: 11, weight: .black))
                        .foregroundStyle(Color(hex: 0x36C282))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color(hex: 0x36C282).opacity(0.2), in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                    Spacer().frame(height: 8)

                    Text((viewModel.currentPlaying ?? fallbackGame).name)
                        .font(.system(size: 28, weight: .black))
                        .foregroundStyle(.white)

                    Text((viewModel.currentPlaying ?? fallbackGame).description.isEmpty ? copy.resumeSubtitle : (viewModel.currentPlaying ?? fallbackGame).description)
                        .font(.system(size: 13))
                        .foregroundStyle(Color.white.opacity(0.82))

                    Spacer().frame(height: 16)

                    Text(copy.resumeAction)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(
                            LinearGradient(
                                colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                                startPoint: .leading,
                                endPoint: .trailing
                            ),
                            in: RoundedRectangle(cornerRadius: 24, style: .continuous)
                        )
                }
                .padding(24)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
            }
            .frame(height: 260)
            .overlay(
                RoundedRectangle(cornerRadius: 32, style: .continuous)
                    .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
            )
        }
        .simultaneousGesture(TapGesture().onEnded {
            viewModel.markPlayed(viewModel.currentPlaying ?? fallbackGame)
        })
        .buttonStyle(.plain)
    }

    private var recentSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader(
                title: copy.recentTitle,
                action: viewModel.recentGames.count > 8 ? copy.more : nil,
                destination: LibrarySectionListView(title: copy.recentTitle, games: viewModel.recentGames)
            )

            gameGrid(games: Array(viewModel.recentGames.prefix(8)))
        }
    }

    private var trendingSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader(title: hasLibraryContent ? copy.trendingTitle : copy.trendingTitle, action: nil, destination: EmptyView())
            gameGrid(games: Array((viewModel.everyonePlaying.isEmpty ? viewModel.newbieGames : viewModel.everyonePlaying).prefix(8)), canToggle: true)
        }
    }

    private var myGamesSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader(title: copy.myGamesTitle, action: nil, destination: EmptyView())
            gameGrid(games: viewModel.myGames, canToggle: true)
        }
    }

    private var coldStartHero: some View {
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x5C8CFF), Color(hex: 0x9258FF)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )

            VStack(alignment: .leading, spacing: 0) {
                Text(copy.newPlayerTag)
                    .font(.system(size: 11, weight: .black))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.2), in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                Spacer().frame(height: 10)

                Text((viewModel.newbieGames.first ?? fallbackGame).name)
                    .font(.system(size: 28, weight: .black))
                    .foregroundStyle(.white)

                Spacer().frame(height: 4)

                Text((viewModel.newbieGames.first ?? fallbackGame).description.isEmpty ? copy.coldStartSubtitle : (viewModel.newbieGames.first ?? fallbackGame).description)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.white.opacity(0.82))

                Spacer().frame(height: 16)

                AuthGateLaunchLink(game: viewModel.newbieGames.first ?? fallbackGame) {
                    Text(copy.coldStartAction)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color(hex: 0x1A1A1A))
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        }
        .frame(height: 260)
        .overlay(
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private var myGamesEmptyCard: some View {
        VStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color.white.opacity(0.06))
                .frame(width: 64, height: 64)
                .overlay(
                    Text(copy.emptyIcon)
                        .font(.system(size: 28))
                )

            Spacer().frame(height: 14)

            Text(copy.emptyTitle)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(.white)

            Spacer().frame(height: 8)

            Text(copy.emptyDescription)
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: 0xA0A0A0))
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 24)
        .frame(maxWidth: .infinity)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private func sectionHeader<Destination: View>(title: String, action: String?, destination: Destination) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 24, weight: .black))
                .foregroundStyle(.white)

            Spacer()

            if let action {
                NavigationLink(destination: destination) {
                    HStack(spacing: 2) {
                        Text(action)
                            .font(.system(size: 14, weight: .semibold))
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundStyle(Color(hex: 0x6B4EFF))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func gameGrid(games: [Game], canToggle: Bool = false) -> some View {
        Group {
            if games.isEmpty {
                Text(copy.emptySection)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
            } else {
                LazyVGrid(columns: columns, alignment: .leading, spacing: 16) {
                    ForEach(games) { game in
                        VStack(spacing: 8) {
                            AuthGateLaunchLink(game: game) {
                                AsyncImage(url: URL(string: game.iconUrl)) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .fill(Color(hex: 0x1C1C1F))
                                }
                                .frame(maxWidth: .infinity)
                                .aspectRatio(1, contentMode: .fit)
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                                )
                            }
                            .simultaneousGesture(TapGesture().onEnded {
                                viewModel.markPlayed(game)
                            })
                            .buttonStyle(.plain)

                            Text(game.name)
                                .font(.system(size: 12))
                                .foregroundStyle(Color(hex: 0xA0A0A0))
                                .lineLimit(1)
                                .frame(maxWidth: .infinity)

                            if canToggle {
                                Button(viewModel.myGames.contains(where: { $0.id == game.id }) ? copy.remove : copy.add) {
                                    viewModel.toggleMyGame(game)
                                }
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 6)
                                .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                        }
                    }
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

    private var fallbackGame: Game {
        viewModel.allGames.first ?? Game(
            id: UUID().uuidString,
            name: copy.fallbackName,
            description: copy.resumeSubtitle,
            iconUrl: "",
            downloadUrl: "",
            version: "1.0.0",
            md5: "",
            category: copy.trendingTitle
        )
    }

    private var copy: LibraryCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct LibraryCopy {
    let title: String
    let loading: String
    let runningTag: String
    let resumeSubtitle: String
    let resumeAction: String
    let recentTitle: String
    let trendingTitle: String
    let myGamesTitle: String
    let more: String
    let newPlayerTag: String
    let coldStartSubtitle: String
    let coldStartAction: String
    let emptyIcon: String
    let emptyTitle: String
    let emptyDescription: String
    let emptySection: String
    let loadEnd: String
    let add: String
    let remove: String
    let fallbackName: String

    static func forLanguage(_ language: AppLanguage) -> LibraryCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                title: "我的库",
                loading: "正在准备你的游戏库...",
                runningTag: "进行中",
                resumeSubtitle: "继续上次的进度，马上回到游戏里。",
                resumeAction: "继续游玩",
                recentTitle: "最近玩过",
                trendingTitle: "大家都在玩",
                myGamesTitle: "我的游戏",
                more: "查看更多",
                newPlayerTag: "新玩家推荐",
                coldStartSubtitle: "先挑一款热门游戏，马上开始你的第一局。",
                coldStartAction: "去玩看看",
                emptyIcon: "🎮",
                emptyTitle: "还没有收藏的游戏",
                emptyDescription: "先去发现页挑几款喜欢的，之后这里会更热闹。",
                emptySection: "暂无内容",
                loadEnd: "已经到底啦",
                add: "加入",
                remove: "移除",
                fallbackName: "推荐游戏"
            )
        case .traditionalChinese:
            return .init(
                title: "我的庫",
                loading: "正在準備你的遊戲庫...",
                runningTag: "進行中",
                resumeSubtitle: "繼續上次的進度，馬上回到遊戲裡。",
                resumeAction: "繼續遊玩",
                recentTitle: "最近玩過",
                trendingTitle: "大家都在玩",
                myGamesTitle: "我的遊戲",
                more: "查看更多",
                newPlayerTag: "新玩家推薦",
                coldStartSubtitle: "先挑一款熱門遊戲，馬上開始你的第一局。",
                coldStartAction: "去玩看看",
                emptyIcon: "🎮",
                emptyTitle: "還沒有收藏的遊戲",
                emptyDescription: "先去發現頁挑幾款喜歡的，之後這裡會更熱鬧。",
                emptySection: "暫無內容",
                loadEnd: "已經到底了",
                add: "加入",
                remove: "移除",
                fallbackName: "推薦遊戲"
            )
        case .english:
            return .init(
                title: "Library",
                loading: "Preparing your library...",
                runningTag: "Running",
                resumeSubtitle: "Jump back into your last session right away.",
                resumeAction: "Continue",
                recentTitle: "Recently Played",
                trendingTitle: "Trending",
                myGamesTitle: "My Games",
                more: "View More",
                newPlayerTag: "New Player Pick",
                coldStartSubtitle: "Pick a hot game and start your first round now.",
                coldStartAction: "Play Now",
                emptyIcon: "🎮",
                emptyTitle: "No games collected yet",
                emptyDescription: "Pick a few favorites from Discover and this space will fill up fast.",
                emptySection: "No content yet",
                loadEnd: "You've reached the end",
                add: "Add",
                remove: "Remove",
                fallbackName: "Featured Game"
            )
        }
    }
}
