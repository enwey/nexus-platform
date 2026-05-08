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
                            .frame(height: 28)

                        statsRow

                        Spacer()
                            .frame(height: 26)

                        Text(copy.introTitle)
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(.white)

                        Spacer()
                            .frame(height: 12)

                        Text(displayDescription)
                            .font(.system(size: 14))
                            .foregroundStyle(Color(hex: 0x8B8D99))
                            .lineSpacing(6)
                            .fixedSize(horizontal: false, vertical: true)

                        Spacer()
                            .frame(height: 120)
                    }
                    .padding(.top, 30)
                    .padding(.horizontal, 24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .background(
                        Color(hex: 0x090A0F)
                            .clipShape(
                                RoundedRectangle(cornerRadius: 32, style: .continuous)
                            )
                    )
                    .offset(y: -40)
                    .padding(.bottom, -40)
                }
            }
            .ignoresSafeArea(edges: .top)

            bottomPlayButton
        }
        .background(
            Color(hex: 0x090A0F)
                .ignoresSafeArea()
        )
        .toolbar(.hidden, for: .tabBar)
        .navigationBarTitleDisplayMode(.inline)
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
                    heroPlaceholder
                }
            } else {
                heroPlaceholder
            }
        }
        .overlay(
            LinearGradient(
                colors: [
                    Color.black.opacity(0.16),
                    Color.black.opacity(0.06),
                    Color(hex: 0x090A0F).opacity(0.96)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .overlay(alignment: .top) {
            LinearGradient(
                colors: [Color.black.opacity(0.22), .clear],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 110)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 336)
        .clipped()
        .ignoresSafeArea(edges: .top)
    }

    private var headerRow: some View {
        HStack(alignment: .bottom, spacing: 20) {
            gameIcon

            VStack(alignment: .leading, spacing: 6) {
                Text(resolvedGame.localizedName(for: language))
                    .font(.system(size: 24, weight: .heavy))
                    .foregroundStyle(.white)
                    .lineLimit(2)

                if developerLine.isEmpty == false {
                    Text(developerLine)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color(hex: 0x5C67FF))
                        .lineLimit(1)
                }

                if categoryLine.isEmpty == false {
                    Text(categoryLine)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color.white.opacity(0.62))
                        .lineLimit(1)
                }
            }
            .padding(.bottom, 6)

            Spacer(minLength: 0)
        }
    }

    private var statsRow: some View {
        HStack(spacing: 0) {
            statItem(value: packageSizeText, label: copy.sizeMeta)
            statDivider
            statItem(value: rankText, label: copy.rankMeta)
            statDivider
            statItem(value: versionText, label: copy.versionMeta)
        }
        .padding(.vertical, 16)
        .background(Color.clear)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.white.opacity(0.08))
                .frame(height: 1)
        }
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color.white.opacity(0.08))
                .frame(height: 1)
        }
    }

    private func statItem(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 18, weight: .heavy))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
            Text(label)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: 0x8B8D99))
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .frame(maxWidth: .infinity)
    }

    private var statDivider: some View {
        Rectangle()
            .fill(Color.white.opacity(0.08))
            .frame(width: 1, height: 38)
    }

    private var bottomPlayButton: some View {
        VStack(spacing: 0) {
            AuthGateLaunchLink(game: resolvedGame) {
                Text(copy.playNow)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 64)
                    .background(
                        LinearGradient(
                            colors: [Color(hex: 0x5C67FF), Color(hex: 0xCB63FF)],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        in: RoundedRectangle(cornerRadius: 20, style: .continuous)
                    )
                    .shadow(color: Color(hex: 0x5C67FF).opacity(0.32), radius: 18, y: 8)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 24)
        .padding(.top, 16)
        .padding(.bottom, 30)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x090A0F).opacity(0), Color(hex: 0x090A0F), Color(hex: 0x090A0F)],
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

    private var developerLine: String {
        developerName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var categoryLine: String {
        resolvedGame.category?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }

    private var versionText: String {
        let value = resolvedGame.version.trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? "--" : value
    }

    private var rankText: String {
        guard let rank = viewModel.runtimeProfile?.categoryRank else { return "--" }
        return "#\(rank)"
    }

    private var displayDescription: String {
        let value = resolvedGame.localizedDescription(for: language).trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? copy.introFallback : value
    }

    private var packageSizeText: String {
        guard let bytes = viewModel.runtimeProfile?.packageSizeBytes else { return "--" }
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useKB, .useMB, .useGB]
        formatter.countStyle = .file
        formatter.includesUnit = true
        formatter.isAdaptive = true
        return formatter.string(fromByteCount: bytes)
    }

    private var gameIcon: some View {
        AsyncImage(url: URL(string: resolvedGame.iconUrl)) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color(hex: 0x232326))
                .overlay(
                    Text(String(resolvedGame.localizedName(for: language).prefix(1)))
                        .font(.system(size: 30, weight: .bold))
                        .foregroundStyle(Color.white.opacity(0.92))
                )
        }
        .frame(width: 90, height: 90)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(Color(hex: 0x090A0F), lineWidth: 4)
        )
        .shadow(color: Color.black.opacity(0.38), radius: 16, y: 8)
        .offset(y: -50)
        .padding(.bottom, -50)
    }

    private var heroPlaceholder: some View {
        LinearGradient(
            colors: [Color(hex: 0x202331), Color(hex: 0x14151B), Color(hex: 0x090A0F)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .overlay(
            RadialGradient(
                colors: [Color(hex: 0x5C67FF).opacity(0.35), .clear],
                center: .topTrailing,
                startRadius: 12,
                endRadius: 220
            )
        )
    }

    private var copy: GameDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct GameDetailCopy {
    let introTitle: String
    let introFallback: String
    let playNow: String
    let sizeMeta: String
    let rankMeta: String
    let versionMeta: String

    static func forLanguage(_ language: AppLanguage) -> GameDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(
                introTitle: "游戏简介",
                introFallback: "暂无简介",
                playNow: "立即秒开",
                sizeMeta: "大小",
                rankMeta: "排行",
                versionMeta: "版本"
            )
        case .traditionalChinese:
            return .init(
                introTitle: "遊戲簡介",
                introFallback: "暫無簡介",
                playNow: "立即秒開",
                sizeMeta: "大小",
                rankMeta: "排行",
                versionMeta: "版本"
            )
        case .english:
            return .init(
                introTitle: "Introduction",
                introFallback: "No description yet",
                playNow: "Play Instantly",
                sizeMeta: "Size",
                rankMeta: "Rank",
                versionMeta: "Version"
            )
        }
    }
}
