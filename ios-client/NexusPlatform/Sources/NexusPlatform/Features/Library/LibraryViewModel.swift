import Foundation

@MainActor
final class LibraryViewModel: ObservableObject {
    enum ColdStartState: Equatable {
        case loading
        case returning(lastPlayed: Game)
        case newUser(recommended: [Game])
        case empty
    }

    @Published private(set) var state: ColdStartState = .loading
    @Published private(set) var games: [Game] = []

    private let catalogService: GameCatalogServiceProtocol
    private let engagementStore: GameEngagementStore

    init(
        catalogService: GameCatalogServiceProtocol = GameCatalogService(),
        engagementStore: GameEngagementStore = .shared
    ) {
        self.catalogService = catalogService
        self.engagementStore = engagementStore
    }

    func load() {
        Task {
            state = .loading
            do {
                let loaded = try await catalogService.fetchGames()
                games = loaded
                let snapshot = await engagementStore.read()

                if let lastID = snapshot.lastPlayedGameID,
                   let last = loaded.first(where: { $0.id == lastID }) {
                    state = .returning(lastPlayed: last)
                    return
                }

                if loaded.isEmpty {
                    state = .empty
                    return
                }

                let newbie = Array(loaded.prefix(4))
                state = .newUser(recommended: newbie)
            } catch {
                games = []
                state = .empty
            }
        }
    }

    func markPlayed(_ game: Game) {
        Task { await engagementStore.markPlayed(gameID: game.id) }
    }
}
