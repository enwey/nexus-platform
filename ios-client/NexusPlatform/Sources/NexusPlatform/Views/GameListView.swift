import SwiftUI

struct GameListView: View {
    var body: some View {
        LibraryView()
    }
}

struct GameRow: View {
    let game: Game
    
    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: game.iconUrl)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color(hex: 0x232326))
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
            )
            
            VStack(alignment: .leading, spacing: 4) {
                Text(game.name)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                Text(game.description.isEmpty ? "v\(game.version)" : game.description)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: 0xA0A0A0))
                    .lineLimit(1)
                Text("v\(game.version)")
                    .font(.system(size: 11, weight: .semibold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color(hex: 0x6B4EFF).opacity(0.14), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
                    .foregroundStyle(Color(hex: 0x6B4EFF))
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
}
