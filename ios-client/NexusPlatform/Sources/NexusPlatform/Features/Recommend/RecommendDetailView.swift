import SwiftUI

struct RecommendDetailView: View {
    let item: RecommendTodayItem
    let game: Game

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                AsyncImage(url: URL(string: item.coverURL)) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Rectangle().fill(AppTheme.GradientToken.hero)
                }
                .frame(height: 320)
                .clipped()

                VStack(alignment: .leading, spacing: 12) {
                    Text(item.articleTag.isEmpty ? "专题" : item.articleTag)
                        .font(.caption.bold())
                        .foregroundStyle(AppTheme.ColorToken.auroraPink)

                    Text(item.articleTitle.isEmpty ? (item.cardTitle.isEmpty ? item.gameName : item.cardTitle) : item.articleTitle)
                        .font(.title2.bold())

                    Text(item.articleBody.isEmpty ? game.description : item.articleBody)
                        .font(.body)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)

                    VStack(alignment: .leading, spacing: 8) {
                        Text(game.name)
                            .font(.headline)
                        Text(game.description.isEmpty ? (game.category ?? "精选游戏") : game.description)
                            .font(.footnote)
                            .foregroundStyle(AppTheme.ColorToken.textSecondary)
                        AuthGateLaunchLink(game: game) {
                            Text(item.actionText.isEmpty ? "立即秒开" : item.actionText)
                                .font(.headline)
                                .nexusPrimaryCTA()
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(14)
                    .nexusGlassCard()
                }
                .padding(.horizontal, AppTheme.Layout.pagePadding)
                .padding(.bottom, 24)
            }
        }
        .nexusPageBackground()
        .navigationBarTitleDisplayMode(.inline)
    }
}
