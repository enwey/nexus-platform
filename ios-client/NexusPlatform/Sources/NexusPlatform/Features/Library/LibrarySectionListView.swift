import SwiftUI

struct LibrarySectionListView: View {
    let title: String
    let games: [Game]

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(games) { game in
                    AuthGateLaunchLink(game: game) {
                        GameRow(game: game)
                    }
                    .simultaneousGesture(TapGesture().onEnded {
                        Task { await GameEngagementStore.shared.markPlayed(gameID: game.id) }
                    })
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 96)
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .nexusTabBarHidden()
    }
}
