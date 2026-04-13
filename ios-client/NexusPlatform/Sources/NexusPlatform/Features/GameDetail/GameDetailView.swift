import SwiftUI

struct GameDetailView: View {
    let game: Game
    @StateObject private var viewModel = GameDetailViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                hero
                VStack(alignment: .leading, spacing: 8) {
                    Text(viewModel.runtimeProfile?.gameName.isEmpty == false ? (viewModel.runtimeProfile?.gameName ?? game.name) : game.name)
                        .font(.title.bold())
                    Text(viewModel.runtimeProfile?.shareSubtitle.isEmpty == false ? (viewModel.runtimeProfile?.shareSubtitle ?? game.description) : game.description)
                        .font(.body)
                        .foregroundStyle(AppTheme.ColorToken.textSecondary)
                }
                .padding(.horizontal, AppTheme.Layout.pagePadding)

                Text("版本 v\(game.version)")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    .padding(.horizontal, AppTheme.Layout.pagePadding)

                if let profile = viewModel.runtimeProfile {
                    VStack(alignment: .leading, spacing: 6) {
                        if profile.studioName.isEmpty == false {
                            Text("厂牌：\(profile.studioName)")
                                .font(.footnote)
                        }
                        if profile.playerCountText.isEmpty == false {
                            Text("玩家：\(profile.playerCountText)")
                                .font(.footnote)
                        }
                    }
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
                    .padding(.horizontal, AppTheme.Layout.pagePadding)
                }

                Spacer(minLength: 80)
            }
        }
        .safeAreaInset(edge: .bottom) {
            NavigationLink(destination: GameView(game: game)) {
                Text("立即秒开")
                    .font(.headline)
                    .nexusPrimaryCTA()
                    .padding(.horizontal, AppTheme.Layout.pagePadding)
                    .padding(.top, 8)
                    .padding(.bottom, 12)
                    .background(.ultraThinMaterial)
            }
            .buttonStyle(.plain)
        }
        .nexusPageBackground()
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { viewModel.load(appID: game.id) }
    }

    private var hero: some View {
        ZStack(alignment: .bottomLeading) {
            if let banner = viewModel.runtimeProfile?.runtimeBannerURL,
               let url = URL(string: banner),
               banner.isEmpty == false {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: 0, style: .continuous).fill(AppTheme.GradientToken.hero)
                }
            } else {
                RoundedRectangle(cornerRadius: 0, style: .continuous).fill(AppTheme.GradientToken.hero)
            }
        }
        .frame(height: 260)
        .clipped()
        .overlay(alignment: .bottomLeading) {
            Text(game.category ?? "精选")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
                .padding(14)
                .background(Color.black.opacity(0.2), in: Capsule())
                .padding(16)
        }
    }
}
