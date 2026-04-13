import SwiftUI

struct DiscoverView: View {
    @StateObject private var viewModel = DiscoverViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                fallbackBanner
                heroSection
                rankingSection
                categorySection
                feedSection
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("发现")
        .onAppear { viewModel.load() }
    }

    @ViewBuilder
    private var fallbackBanner: some View {
        if let message = viewModel.heroFallbackMessage {
            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.yellow)
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                Spacer(minLength: 0)
            }
            .padding(10)
            .background(AppTheme.ColorToken.surfaceSecondary, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    @ViewBuilder
    private var heroSection: some View {
        if let hero = viewModel.hero {
            NavigationLink(destination: GameDetailView(game: heroTargetGame)) {
                ZStack(alignment: .bottomLeading) {
                    RoundedRectangle(cornerRadius: AppTheme.Layout.cardRadius, style: .continuous)
                        .fill(AppTheme.GradientToken.hero)
                        .frame(height: 180)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(hero.badgeText.isEmpty ? "精选推荐" : hero.badgeText)
                            .font(.caption.bold())
                            .foregroundStyle(.white.opacity(0.9))
                        Text(hero.title)
                            .font(.title3.bold())
                            .foregroundStyle(.white)
                        Text(hero.subtitle)
                            .font(.footnote)
                            .foregroundStyle(.white.opacity(0.88))
                            .lineLimit(2)
                    }
                    .padding(14)
                }
            }
            .buttonStyle(.plain)
        }
    }

    private var rankingSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("排行榜")
                .font(.headline)
            ForEach(Array(viewModel.topRanked.enumerated()), id: \.element.id) { idx, game in
                NavigationLink(destination: GameDetailView(game: game)) {
                    HStack {
                        Text("#\(idx + 1)")
                            .font(.headline.monospacedDigit())
                            .foregroundStyle(AppTheme.ColorToken.auroraPink)
                            .frame(width: 44)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(game.name).font(.subheadline.bold())
                            Text(game.description)
                                .font(.caption)
                                .foregroundStyle(AppTheme.ColorToken.textSecondary)
                                .lineLimit(1)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 4)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(14)
        .nexusGlassCard()
    }

    private var categorySection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.categories, id: \.self) { category in
                    let selected = category == viewModel.selectedCategory
                    Button(category) {
                        viewModel.selectCategory(category)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(selected ? AnyShapeStyle(AppTheme.GradientToken.hero) : AnyShapeStyle(AppTheme.ColorToken.surfaceSecondary))
                    .foregroundStyle(selected ? Color.white : AppTheme.ColorToken.textPrimary)
                    .clipShape(Capsule())
                }
            }
        }
    }

    private var feedSection: some View {
        LazyVStack(spacing: 12) {
            ForEach(viewModel.filteredGames) { game in
                NavigationLink(destination: GameDetailView(game: game)) {
                    VStack(alignment: .leading, spacing: 8) {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(AppTheme.GradientToken.hero)
                            .frame(height: 132)
                            .overlay(alignment: .bottomLeading) {
                                Text(game.category ?? "推荐")
                                    .font(.caption.bold())
                                    .foregroundStyle(.white)
                                    .padding(8)
                                    .background(Color.black.opacity(0.2), in: Capsule())
                                    .padding(10)
                            }
                        Text(game.name).font(.headline)
                        Text(game.description)
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.ColorToken.textSecondary)
                            .lineLimit(2)
                    }
                    .padding(12)
                    .nexusGlassCard()
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var heroTargetGame: Game {
        if let heroGame = viewModel.heroGame {
            return heroGame
        }

        if let hero = viewModel.hero {
            return Game(
                id: hero.appID.isEmpty ? UUID().uuidString : hero.appID,
                name: hero.title.isEmpty ? "推荐游戏" : hero.title,
                description: hero.subtitle,
                iconUrl: "",
                downloadUrl: "",
                version: "1.0.0",
                md5: "",
                category: "精选"
            )
        }

        return Game(
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
}
