import SwiftUI

struct DiscoverRankingView: View {
    let games: [Game]
    @State private var language: AppLanguage = AppLanguageStore.currentSync()

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 12) {
                if games.isEmpty {
                    ForEach(0..<6, id: \.self) { index in
                        rankingSkeletonRow(rank: index + 1)
                    }
                } else {
                    ForEach(Array(games.enumerated()), id: \.element.id) { index, game in
                        NavigationLink(destination: GameDetailView(game: game)) {
                            HStack(spacing: 12) {
                                Text("\(index + 1)")
                                    .font(.system(size: 22, weight: .heavy))
                                    .foregroundStyle(.white)
                                    .frame(width: 28, height: 28)

                                AsyncImage(url: URL(string: game.iconUrl)) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    NativeSkeletonBlock(width: 52, height: 52, cornerRadius: 12)
                                }
                                .frame(width: 52, height: 52)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
                                )

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(game.localizedName(for: language))
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundStyle(.white)
                                    Text(localizedGameDescription(for: game))
                                        .font(.system(size: 12))
                                        .foregroundStyle(Color(hex: 0xA0A0A0))
                                        .lineLimit(1)
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
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 96)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(copy.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
        .onReceive(NotificationCenter.default.publisher(for: AppLanguageStore.didChangeNotification)) { notification in
            if let language = notification.object as? AppLanguage {
                self.language = language
            } else {
                language = AppLanguageStore.currentSync()
            }
        }
        .animation(NativeMotion.overlayTransition, value: games.isEmpty)
    }

    private var copy: DiscoverRankingCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    private func rankingSkeletonRow(rank: Int) -> some View {
        HStack(spacing: 12) {
            Text("\(rank)")
                .font(.system(size: 22, weight: .heavy))
                .foregroundStyle(Color.white.opacity(0.12))
                .frame(width: 28, height: 28)

            NativeSkeletonBlock(width: 52, height: 52, cornerRadius: 12)

            VStack(alignment: .leading, spacing: 6) {
                NativeSkeletonBlock(width: 132, height: 14, cornerRadius: 7)
                NativeSkeletonBlock(width: 166, height: 12, cornerRadius: 6)
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

    private func localizedGameDescription(for game: Game) -> String {
        let localizedDescription = game.localizedDescription(for: language)
        return localizedDescription.isEmpty ? "v\(game.version)" : localizedDescription
    }
}

private struct DiscoverRankingCopy {
    let title: String

    static func forLanguage(_ language: AppLanguage) -> DiscoverRankingCopy {
        switch language {
        case .simplifiedChinese:
            return .init(title: "查看更多")
        case .traditionalChinese:
            return .init(title: "查看更多")
        case .english:
            return .init(title: "View More")
        }
    }
}
