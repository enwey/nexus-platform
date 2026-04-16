import SwiftUI

struct RecommendDetailView: View {
    let item: RecommendTodayItem
    let game: Game
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                hero

                VStack(alignment: .leading, spacing: 14) {
                    Text(item.articleTag.isEmpty ? copy.defaultTag : item.articleTag)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(Color(hex: 0x6B4EFF))

                    Text(item.articleTitle.isEmpty ? resolvedTitle : item.articleTitle)
                        .font(.system(size: 30, weight: .black))
                        .foregroundStyle(.white)

                    Text(item.articleBody.isEmpty ? game.description : item.articleBody)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xA0A0A0))

                    gameCard
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)
                .padding(.bottom, 96)
            }
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    private var hero: some View {
        ZStack(alignment: .topLeading) {
            AsyncImage(url: URL(string: item.coverURL)) { image in
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

            LinearGradient(
                colors: [.clear, Color.black.opacity(0.58)],
                startPoint: .top,
                endPoint: .bottom
            )

            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.black.opacity(0.45), in: Circle())
            }
            .padding(.top, 52)
            .padding(.leading, 20)
        }
        .frame(height: 420)
        .clipped()
    }

    private var gameCard: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: game.iconUrl)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color(hex: 0x232326))
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(game.name)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)

                Text(game.description.isEmpty ? (game.category ?? copy.defaultGameCategory) : game.description)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .lineLimit(1)
            }

            Spacer()

            AuthGateLaunchLink(game: game) {
                Text(item.actionText.isEmpty ? copy.play : item.actionText)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(.black)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(
                        LinearGradient(
                            colors: [Color(hex: 0x6B4EFF), Color(hex: 0xA04CFF)],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        in: RoundedRectangle(cornerRadius: 18, style: .continuous)
                    )
                    .foregroundStyle(.white)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
    }

    private var resolvedTitle: String {
        item.cardTitle.isEmpty ? item.gameName : item.cardTitle
    }

    private var copy: RecommendDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct RecommendDetailCopy {
    let defaultTag: String
    let defaultGameCategory: String
    let play: String

    static func forLanguage(_ language: AppLanguage) -> RecommendDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(defaultTag: "专题", defaultGameCategory: "精选游戏", play: "立即秒开")
        case .traditionalChinese:
            return .init(defaultTag: "專題", defaultGameCategory: "精選遊戲", play: "立即秒開")
        case .english:
            return .init(defaultTag: "Feature", defaultGameCategory: "Featured", play: "Play Now")
        }
    }
}
