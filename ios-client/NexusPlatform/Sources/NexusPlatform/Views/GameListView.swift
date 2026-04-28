import SwiftUI

struct GameListView: View {
    var body: some View {
        LibraryView()
    }
}

struct GameRow: View {
    let game: Game
    var subtitle: String? = nil
    var leadingTitle: String? = nil
    @State private var language: AppLanguage = AppLanguageStore.currentSync()

    var body: some View {
        HStack(spacing: 12) {
            if let leadingTitle, leadingTitle.isEmpty == false {
                Text(leadingTitle)
                    .font(.system(size: 22, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)
            }

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

            VStack(alignment: .leading, spacing: 6) {
                Text(game.localizedName(for: language))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)

                if let subtitle = resolvedSubtitle {
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0xA0A0A0))
                        .lineLimit(1)
                }
            }

            Spacer()
        }
        .padding(12)
        .background(Color(hex: 0x1C1C1F), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color(hex: 0x2D2D31), lineWidth: 1)
        )
        .onReceive(NotificationCenter.default.publisher(for: AppLanguageStore.didChangeNotification)) { notification in
            if let language = notification.object as? AppLanguage {
                self.language = language
            } else {
                language = AppLanguageStore.currentSync()
            }
        }
    }

    private var resolvedSubtitle: String? {
        if let subtitle {
            return subtitle.isEmpty ? nil : subtitle
        }
        let localizedDescription = game.localizedDescription(for: language)
        let fallback = localizedDescription.isEmpty ? "v\(game.version)" : localizedDescription
        return fallback.isEmpty ? nil : fallback
    }
}
