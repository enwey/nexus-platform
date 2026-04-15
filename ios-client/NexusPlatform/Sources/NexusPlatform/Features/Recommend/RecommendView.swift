import SwiftUI

struct RecommendView: View {
    @StateObject private var viewModel = RecommendViewModel()

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                if viewModel.isLoading {
                    ProgressView("加载中...")
                        .frame(maxWidth: .infinity)
                        .padding(.top, 24)
                } else if viewModel.items.isEmpty {
                    Text("今天还没有新的推荐")
                        .font(.footnote)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 24)
                } else {
                    ForEach(viewModel.items) { item in
                        NavigationLink(
                            destination: RecommendDetailView(
                                item: item,
                                game: resolvedGame(for: item)
                            )
                        ) {
                            RecommendCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle("推荐")
        .onAppear { viewModel.load() }
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
}

private struct RecommendCard: View {
    let item: RecommendTodayItem

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: URL(string: item.coverURL)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Rectangle().fill(AppTheme.GradientToken.hero)
            }
            .frame(height: 360)
            .clipped()

            LinearGradient(
                colors: [.black.opacity(0.1), .black.opacity(0.72)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 12) {
                Text(item.cardCategory.isEmpty ? "今日推荐" : item.cardCategory)
                    .font(.caption.bold())
                    .foregroundStyle(.white.opacity(0.85))

                Text(item.cardTitle.isEmpty ? item.gameName : item.cardTitle)
                    .font(.title3.bold())
                    .foregroundStyle(.white)

                HStack(spacing: 10) {
                    AsyncImage(url: URL(string: item.gameIconURL)) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(AppTheme.ColorToken.surfaceSecondary)
                    }
                    .frame(width: 42, height: 42)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.gameName)
                            .font(.subheadline.bold())
                            .foregroundStyle(.white)
                        Text(item.gameCategory.isEmpty ? "精选" : item.gameCategory)
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.75))
                    }

                    Spacer()

                    Text(item.actionText.isEmpty ? "立即秒开" : item.actionText)
                        .font(.caption.bold())
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.white, in: Capsule())
                        .foregroundStyle(Color.black)
                }
                .padding(10)
                .background(Color.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            .padding(18)
        }
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(AppTheme.ColorToken.border.opacity(0.35), lineWidth: 1)
        )
    }
}
