import SwiftUI

struct GameListView: View {
    var body: some View {
        LibraryView()
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
