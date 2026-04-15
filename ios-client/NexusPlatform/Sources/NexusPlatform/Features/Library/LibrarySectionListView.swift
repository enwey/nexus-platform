import SwiftUI

struct LibrarySectionListView: View {
    let title: String
    let games: [Game]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                ForEach(games) { game in
                    AuthGateLaunchLink(game: game) {
                        GameRow(game: game)
                    }
                    .buttonStyle(.plain)
                    if game.id != games.last?.id {
                        Divider()
                    }
                }
            }
            .padding(AppTheme.Layout.pagePadding)
        }
        .nexusPageBackground()
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}
