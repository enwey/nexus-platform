import SwiftUI

struct DiscoverRankingView: View {
    let games: [Game]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                ForEach(Array(games.enumerated()), id: \.element.id) { index, game in
                    NavigationLink(destination: GameDetailView(game: game)) {
                        HStack(spacing: 14) {
                            Text("#\(index + 1)")
                                .font(.headline.monospacedDigit())
                                .foregroundStyle(AppTheme.ColorToken.auroraPink)
                                .frame(width: 44)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(game.name)
                                    .font(.subheadline.bold())
                                    .foregroundStyle(AppTheme.ColorToken.textPrimary)
                                Text(game.description)
                                    .font(.caption)
                                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                                    .lineLimit(2)
                            }
                            Spacer()
                        }
                    }
                    .buttonStyle(.plain)
                    if game.id != games.last?.id {
                        Divider()
                    }
                }
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("排行榜")
        .navigationBarTitleDisplayMode(.inline)
    }
}
