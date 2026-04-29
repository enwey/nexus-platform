import SwiftUI

struct GameDetailView: View {
    let game: Game
    @StateObject private var viewModel = GameDetailViewModel()
    @State private var language: AppLanguage = AppLanguageStore.currentSync()

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    hero

                    VStack(alignment: .leading, spacing: 0) {
                        headerRow

                        Spacer()
                            .frame(height: 20)

                        statsRow

                        Spacer()
                            .frame(height: 20)

                        Text(copy.introTitle)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(.white)

                        Spacer()
                            .frame(height: 8)

                        Text(displayDescription)
                            .font(.system(size: 14))
                            .foregroundStyle(Color(hex: 0xA0A0A0))
                            .fixedSize(horizontal: false, vertical: true)

                        Spacer()
                            .frame(height: 110)
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .background(Color(hex: 0x121212))
                }
            }

            bottomPlayButton
        }
        .background(
            Color(hex: 0x121212)
                .ignoresSafeArea()
        )
        .toolbar(.hidden, for: .tabBar)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { viewModel.load(appID: game.id) }
        .onReceive(NotificationCenter.default.publisher(for: AppLanguageStore.didChangeNotification)) { notification in
            if let language = notification.object as? AppLanguage {
                self.language = language
            } else {
                language = AppLanguageStore.currentSync()
            }
        }
    }

    private var hero: some View {
        Group {
            if
                let banner = viewModel.runtimeProfile?.runtimeBannerURL,
                let url = URL(string: banner),
                banner.isEmpty == false
            {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Rectangle().fill(Color(hex: 0x232326))
                }
            } else {
                Rectangle()
                    .fill(Color(hex: 0x232326))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 280)
        .clipped()
        .ignoresSafeArea(edges: .top)
    }

    private var headerRow: some View {
        HStack(alignment: .center, spacing: 14) {
            AsyncImage(url: URL(string: resolvedGame.iconUrl)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(Color(hex: 0x232326))
            }
            .frame(width: 84, height: 84)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(resolvedGame.localizedName(for: language))
                    .font(.system(size: 30, weight: .heavy))
                    .foregroundStyle(.white)

                if developerName.isEmpty == false {
                    Text(developerName)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(Color(hex: 0x6B4EFF))
                }
            }
        }
    }

    private var statsRow: some View {
        HStack {
            statItem(value: rankText, label: copy.rank)
            Spacer()
            statItem(value: versionText, label: copy.version)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 20)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func statItem(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
            Text(label)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0xA0A0A0))
        }
        .frame(maxWidth: .infinity)
    }

    private var bottomPlayButton: some View {
        VStack(spacing: 0) {
            AuthGateLaunchLink(game: resolvedGame) {
                Text(copy.playNow)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 58)
                    .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 20)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x121212).opacity(0), Color(hex: 0x121212), Color(hex: 0x121212)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var resolvedGame: Game {
        viewModel.gameDetail ?? game
    }

    private var developerName: String {
        viewModel.runtimeProfile?.studioName ?? ""
    }

    private var versionText: String {
        let value = resolvedGame.version.trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? "--" : value
    }

    private var rankText: String {
        guard let rank = viewModel.discoverRank else { return "--" }
        return "#\(rank)"
    }

    private var displayDescription: String {
        let value = resolvedGame.localizedDescription(for: language).trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? copy.introFallback : value
    }

    private var copy: GameDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct GameDetailCopy {
    let introTitle: String
    let introFallback: String
    let playNow: String
    let rank: String
    let version: String

    static func forLanguage(_ language: AppLanguage) -> GameDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(introTitle: "游戏简介", introFallback: "暂无简介", playNow: "秒开", rank: "排行", version: "版本")
        case .traditionalChinese:
            return .init(introTitle: "遊戲簡介", introFallback: "暫無簡介", playNow: "秒開", rank: "排行", version: "版本")
        case .english:
            return .init(introTitle: "Introduction", introFallback: "No description yet", playNow: "Play", rank: "Rank", version: "Version")
        }
    }
}
