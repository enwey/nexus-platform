import SwiftUI

struct Game: Identifiable, Codable {
    let id: String
    let name: String
    let description: String
    let iconUrl: String
    let downloadUrl: String
    let version: String
    let md5: String
}

struct GameListView: View {
    @State private var games: [Game] = []
    
    var body: some View {
        List(games) { game in
            NavigationLink(destination: GameView(game: game)) {
                GameRow(game: game)
            }
        }
        .navigationTitle("游戏列表")
        .onAppear {
            loadGames()
        }
    }
    
    private func loadGames() {
        games = [
            Game(
                id: "1",
                name: "示例游戏",
                description: "这是一个示例游戏",
                iconUrl: "",
                downloadUrl: "https://example.com/game1.zip",
                version: "1.0.0",
                md5: ""
            )
        ]
    }
}

struct GameRow: View {
    let game: Game
    
    var body: some View {
        HStack {
            AsyncImage(url: URL(string: game.iconUrl)) { image in
                image.resizable()
            } placeholder: {
                Rectangle()
                    .fill(Color.gray.opacity(0.3))
            }
            .frame(width: 60, height: 60)
            .cornerRadius(8)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(game.name)
                    .font(.headline)
                Text(game.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("v\(game.version)")
                    .font(.caption2)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color.blue.opacity(0.1))
                    .foregroundColor(.blue)
                    .cornerRadius(4)
            }
            
            Spacer()
        }
        .padding(.vertical, 8)
    }
}
