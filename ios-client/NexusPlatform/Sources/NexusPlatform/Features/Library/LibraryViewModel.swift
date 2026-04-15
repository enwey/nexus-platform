import Foundation

@MainActor
final class LibraryViewModel: ObservableObject {
    @Published private(set) var isLoading = false
    @Published private(set) var currentPlaying: Game?
    @Published private(set) var recentGames: [Game] = []
    @Published private(set) var myGames: [Game] = []
    @Published private(set) var everyonePlaying: [Game] = []
    @Published private(set) var newbieGames: [Game] = []
    @Published private(set) var allGames: [Game] = []

    private let catalogService: GameCatalogServiceProtocol
    private let engagementStore: GameEngagementStore
    private let homeService: LibraryHomeServiceProtocol

    init(
        catalogService: GameCatalogServiceProtocol = GameCatalogService(),
        engagementStore: GameEngagementStore = .shared,
        homeService: LibraryHomeServiceProtocol = LibraryHomeService()
    ) {
        self.catalogService = catalogService
        self.engagementStore = engagementStore
        self.homeService = homeService
    }

    func load() {
        Task {
            isLoading = true
            defer { isLoading = false }

            let snapshot = await engagementStore.read()
            let catalog = (try? await catalogService.fetchGames()) ?? []
            allGames = catalog

            if let remoteHome = try? await homeService.fetchHome() {
                currentPlaying = remoteHome.currentPlayingGame
                recentGames = remoteHome.recentGames
                myGames = remoteHome.myGames
                everyonePlaying = remoteHome.everyonePlaying
                newbieGames = remoteHome.newbieMustPlay
                await engagementStore.applyCloudState(
                    currentPlayingGameID: remoteHome.currentPlayingGame?.id,
                    recentGameIDs: remoteHome.recentGames.map(\.id),
                    favoriteGameIDs: remoteHome.myGames.map(\.id)
                )
                return
            }

            currentPlaying = catalog.first { $0.id == snapshot.lastPlayedGameID }
            recentGames = snapshot.recentGameIDs.compactMap { id in
                catalog.first(where: { $0.id == id })
            }
            myGames = snapshot.favoriteGameIDs.compactMap { id in
                catalog.first(where: { $0.id == id })
            }
            everyonePlaying = Array(catalog.filter { game in
                snapshot.favoriteGameIDs.contains(game.id) == false
            }.prefix(8))
            newbieGames = Array(catalog.prefix(4))
        }
    }

    func toggleMyGame(_ game: Game) {
        Task {
            let nowFavorite = await engagementStore.toggleFavorite(gameID: game.id)
            if nowFavorite {
                myGames.removeAll { $0.id == game.id }
                myGames.insert(game, at: 0)
            } else {
                myGames.removeAll { $0.id == game.id }
            }
            if myGames.isEmpty {
                everyonePlaying = Array(allGames.filter { candidate in
                    myGames.contains(where: { $0.id == candidate.id }) == false
                }.prefix(8))
            }
            try? await homeService.setFavorite(appID: game.id, favorite: nowFavorite)
        }
    }

    func markPlayed(_ game: Game) {
        Task {
            await engagementStore.markPlayed(gameID: game.id)
            currentPlaying = game
            recentGames.removeAll { $0.id == game.id }
            recentGames.insert(game, at: 0)
            recentGames = Array(recentGames.prefix(8))
        }
    }
}
