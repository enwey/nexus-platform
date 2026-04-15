import SwiftUI

struct LibraryView: View {
    @StateObject private var viewModel = LibraryViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                if viewModel.isLoading {
                    ProgressView("正在准备你的游戏库...")
                        .frame(maxWidth: .infinity)
                        .padding(.top, 24)
                } else {
                    if let current = viewModel.currentPlaying {
                        continueCard(current)
                    }

                    section(
                        title: "最近玩过",
                        action: viewModel.recentGames.count > 4 ? "查看更多" : nil,
                        games: Array(viewModel.recentGames.prefix(8)),
                        emptyText: "还没有最近记录"
                    )

                    if viewModel.myGames.isEmpty {
                        section(
                            title: "大家都在玩",
                            action: nil,
                            games: Array((viewModel.everyonePlaying.isEmpty ? viewModel.newbieGames : viewModel.everyonePlaying).prefix(8)),
                            emptyText: "去发现页挑一款，马上就能开始。",
                            showFavoriteAction: true
                        )
                    } else {
                        section(
                            title: "我的游戏",
                            action: nil,
                            games: viewModel.myGames,
                            emptyText: "还没有收藏的游戏",
                            showFavoriteAction: true
                        )
                    }
                }
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("我的库")
        .onAppear { viewModel.load() }
    }

    private func continueCard(_ game: Game) -> some View {
        AuthGateLaunchLink(game: game) {
            VStack(alignment: .leading, spacing: 8) {
                Text("继续游玩")
                    .font(.headline)
                Text(game.name)
                    .font(.title3.bold())
                Text(game.description)
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.88))
                Text("立即回到上次进度")
                    .font(.footnote.weight(.semibold))
                    .padding(.top, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(AppTheme.GradientToken.hero, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .foregroundStyle(.white)
        }
        .simultaneousGesture(TapGesture().onEnded {
            viewModel.markPlayed(game)
        })
        .buttonStyle(.plain)
    }

    private func section(
        title: String,
        action: String?,
        games: [Game],
        emptyText: String,
        showFavoriteAction: Bool = false
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.headline)
                Spacer()
                if let action {
                    NavigationLink(destination: LibrarySectionListView(title: title, games: games)) {
                        Text(action)
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(AppTheme.ColorToken.auroraBlue)
                    }
                    .buttonStyle(.plain)
                }
            }

            if games.isEmpty {
                Text(emptyText)
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    .padding(.vertical, 8)
            } else {
                ForEach(games) { game in
                    HStack(spacing: 12) {
                        AuthGateLaunchLink(game: game) {
                            GameRow(game: game)
                        }
                        .simultaneousGesture(TapGesture().onEnded {
                            viewModel.markPlayed(game)
                        })
                        .buttonStyle(.plain)

                        if showFavoriteAction {
                            Button(viewModel.myGames.contains(where: { $0.id == game.id }) ? "移除" : "加入") {
                                viewModel.toggleMyGame(game)
                            }
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(AppTheme.ColorToken.surfaceSecondary, in: Capsule())
                        }
                    }
                    if game.id != games.last?.id {
                        Divider()
                    }
                }
            }
        }
        .padding(16)
        .nexusGlassCard()
    }
}
