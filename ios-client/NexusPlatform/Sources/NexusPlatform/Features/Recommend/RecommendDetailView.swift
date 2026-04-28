import SwiftUI

struct RecommendDetailView: View {
    let item: RecommendTodayItem
    let game: Game
    @State private var language: AppLanguage = AppLanguageStore.currentSync()

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                hero

                VStack(alignment: .leading, spacing: 14) {
                    if resolvedTag.isEmpty == false {
                        Text(resolvedTag)
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(Color(hex: 0x6B4EFF))
                    }

                    Text(item.articleTitle.isEmpty ? resolvedTitle : item.articleTitle)
                        .font(.system(size: 30, weight: .black))
                        .foregroundStyle(.white)

                    Text(item.articleBody.isEmpty ? localizedGameDescription : item.articleBody)
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
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
        .onReceive(NotificationCenter.default.publisher(for: AppLanguageStore.didChangeNotification)) { notification in
            if let language = notification.object as? AppLanguage {
                self.language = language
            } else {
                language = AppLanguageStore.currentSync()
            }
        }
    }

    private var hero: some View {
        ZStack(alignment: .bottom) {
            AsyncImage(
                url: URL(string: item.coverURL),
                transaction: Transaction(animation: NativeMotion.overlayTransition)
            ) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                        .transition(NativeMotion.contentRevealTransition)
                default:
                    Rectangle()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: 0x24253A), Color(hex: 0x1A1A1D)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .overlay(alignment: .bottomLeading) {
                            VStack(alignment: .leading, spacing: 12) {
                                NativeSkeletonBlock(width: 76, height: 14, cornerRadius: 7)
                                NativeSkeletonText(widths: [188, 236], lineHeight: 16, spacing: 10, cornerRadius: 8)
                            }
                            .padding(24)
                        }
                        .transition(NativeMotion.stateSwapTransition)
                }
            }

            LinearGradient(
                colors: [Color.clear, Color.black.opacity(0.58)],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .frame(height: 420)
        .clipped()
        .ignoresSafeArea(edges: .top)
    }

    private var gameCard: some View {
        HStack(spacing: 12) {
            AsyncImage(
                url: URL(string: resolvedGameIconURL),
                transaction: Transaction(animation: NativeMotion.overlayTransition)
            ) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                        .transition(NativeMotion.contentRevealTransition)
                default:
                    NativeSkeletonIcon(size: 56, cornerRadius: 12)
                        .transition(NativeMotion.stateSwapTransition)
                }
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(game.localizedName(for: language))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)

                Text(localizedGameDescription.isEmpty ? (game.category ?? copy.defaultGameCategory) : localizedGameDescription)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .lineLimit(1)
            }

            Spacer()

            AuthGateLaunchLink(game: game) {
                Text(item.actionText.isEmpty ? copy.play : item.actionText)
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
        .padding(14)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
        .animation(NativeMotion.overlayTransition, value: item.coverURL)
        .animation(NativeMotion.overlayTransition, value: resolvedGameIconURL)
    }

    private var resolvedTitle: String {
        item.cardTitle.isEmpty ? game.localizedName(for: language) : item.cardTitle
    }

    private var resolvedGameIconURL: String {
        if game.iconUrl.isEmpty == false {
            return game.iconUrl
        }
        return item.gameIconURL
    }

    private var resolvedTag: String {
        let candidates = [item.articleTag, item.cardCategory, game.category ?? ""]
        return candidates.first(where: { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false }) ?? ""
    }

    private var copy: RecommendDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    private var localizedGameDescription: String {
        game.localizedDescription(for: language)
    }
}

private struct RecommendDetailCopy {
    let title: String
    let defaultTag: String
    let defaultGameCategory: String
    let play: String

    static func forLanguage(_ language: AppLanguage) -> RecommendDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "推荐详情", defaultTag: "专题", defaultGameCategory: "精选游戏", play: "秒开")
        case .traditionalChinese:
            return .init(title: "推薦詳情", defaultTag: "專題", defaultGameCategory: "精選遊戲", play: "秒開")
        case .english:
            return .init(title: "Recommendation", defaultTag: "Feature", defaultGameCategory: "Featured", play: "Play")
        }
    }
}
