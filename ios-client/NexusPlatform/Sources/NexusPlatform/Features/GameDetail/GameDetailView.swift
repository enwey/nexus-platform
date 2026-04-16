import SwiftUI

struct GameDetailView: View {
    let game: Game
    @StateObject private var viewModel = GameDetailViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
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

                Text(game.description.isEmpty ? copy.introFallback : game.description)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0xA0A0A0))

                Spacer()
                    .frame(height: 24)

                AuthGateLaunchLink(game: game) {
                    Text(copy.playNow)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 62)
                        .background(Color(hex: 0x6B4EFF), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(20)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(Color(hex: 0x121212))
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
            .offset(y: -18)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { viewModel.load(appID: game.id) }
    }

    private var hero: some View {
        ZStack(alignment: .topLeading) {
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: [Color(hex: 0x6B4EFF, alpha: 0.65), Color(hex: 0x121212)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )

            if
                let banner = viewModel.runtimeProfile?.runtimeBannerURL,
                let url = URL(string: banner),
                banner.isEmpty == false
            {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Rectangle().fill(Color.clear)
                }
                .overlay(
                    LinearGradient(
                        colors: [Color.black.opacity(0.12), Color.black.opacity(0.35)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }

            Button(copy.back) {
                dismiss()
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: 88, height: 48)
            .background(Color.white.opacity(0.14), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .padding(16)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 280)
        .clipped()
    }

    private var headerRow: some View {
        HStack(alignment: .center, spacing: 14) {
            AsyncImage(url: URL(string: game.iconUrl)) { image in
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
                Text(game.name)
                    .font(.system(size: 30, weight: .heavy))
                    .foregroundStyle(.white)
                Text(copy.editorPick)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0x6B4EFF))
            }
        }
    }

    private var statsRow: some View {
        HStack {
            statItem(value: "4.9", label: copy.score)
            Spacer()
            statItem(value: "#1", label: copy.rank)
            Spacer()
            statItem(value: game.version, label: copy.version)
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

    private var copy: GameDetailCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct GameDetailCopy {
    let back: String
    let editorPick: String
    let introTitle: String
    let introFallback: String
    let playNow: String
    let score: String
    let rank: String
    let version: String

    static func forLanguage(_ language: AppLanguage) -> GameDetailCopy {
        switch language {
        case .simplifiedChinese:
            return .init(back: "返回", editorPick: "编辑精选", introTitle: "游戏简介", introFallback: "暂无简介", playNow: "立即秒开", score: "评分", rank: "排行", version: "版本")
        case .traditionalChinese:
            return .init(back: "返回", editorPick: "編輯精選", introTitle: "遊戲簡介", introFallback: "暫無簡介", playNow: "立即秒開", score: "評分", rank: "排行", version: "版本")
        case .english:
            return .init(back: "Back", editorPick: "Editor's Pick", introTitle: "Introduction", introFallback: "No description yet", playNow: "Play Now", score: "Score", rank: "Rank", version: "Version")
        }
    }
}
