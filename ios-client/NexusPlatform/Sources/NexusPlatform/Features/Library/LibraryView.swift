import SwiftUI

struct LibraryView: View {
    @StateObject private var viewModel = LibraryViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header
                content
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("我的库")
        .onAppear { viewModel.load() }
    }

    @ViewBuilder
    private var header: some View {
        Text("Nexus")
            .font(.largeTitle.bold())
            .foregroundStyle(AppTheme.GradientToken.hero)
        Text("秒开即玩 · 冷启动智能分流")
            .font(.subheadline)
            .foregroundStyle(AppTheme.ColorToken.textSecondary)
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            ProgressView("正在准备你的游戏库...")
                .frame(maxWidth: .infinity)
                .padding(24)
                .nexusGlassCard()
        case .returning(let lastPlayed):
            continueCard(lastPlayed)
            gameGrid(viewModel.games)
        case .newUser(let recommended):
            newbieCard(recommended)
            emptyState
        case .empty:
            emptyState
        }
    }

    private func continueCard(_ game: Game) -> some View {
        NavigationLink(destination: GameView(game: game)) {
            VStack(alignment: .leading, spacing: 8) {
                Text("继续游玩")
                    .font(.headline)
                Text(game.name)
                    .font(.title3.bold())
                Text(game.description)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                Text("立即回到上次进度")
                    .font(.footnote.weight(.semibold))
                    .padding(.top, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(AppTheme.GradientToken.hero, in: RoundedRectangle(cornerRadius: AppTheme.Layout.cardRadius, style: .continuous))
            .foregroundStyle(.white)
        }
        .buttonStyle(.plain)
    }

    private func newbieCard(_ list: [Game]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("新手必玩")
                .font(.headline)
            ForEach(list) { game in
                NavigationLink(destination: GameView(game: game)) {
                    Text(game.name)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
                if game.id != list.last?.id {
                    Divider()
                }
            }
        }
        .padding(16)
        .nexusGlassCard()
    }

    private var emptyState: some View {
        VStack(spacing: 10) {
            Image(systemName: "sparkles.tv")
                .font(.system(size: 26))
                .foregroundStyle(AppTheme.ColorToken.auroraBlue)
            Text("你的游戏库还是空的")
                .font(.headline)
            Text("去发现页挑一款，3 秒内就能开始。")
                .font(.subheadline)
                .foregroundStyle(AppTheme.ColorToken.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(22)
        .nexusGlassCard()
    }

    private func gameGrid(_ games: [Game]) -> some View {
        LazyVStack(spacing: 10) {
            ForEach(games) { game in
                NavigationLink(destination: GameView(game: game)) {
                    GameRow(game: game)
                }
                .buttonStyle(.plain)
            }
        }
    }
}
