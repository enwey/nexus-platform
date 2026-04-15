import Foundation

@MainActor
final class RecommendViewModel: ObservableObject {
    @Published private(set) var items: [RecommendTodayItem] = []
    @Published private(set) var gamesByID: [String: Game] = [:]
    @Published private(set) var isLoading = false

    private let recommendService: RecommendServiceProtocol
    private let gameService: GameCatalogServiceProtocol

    init(
        recommendService: RecommendServiceProtocol = RecommendService(),
        gameService: GameCatalogServiceProtocol = GameCatalogService()
    ) {
        self.recommendService = recommendService
        self.gameService = gameService
    }

    func load() {
        Task {
            isLoading = true
            defer { isLoading = false }

            async let recommendsTask = recommendService.fetchToday(limit: 10)
            async let gamesTask = gameService.fetchGames()

            do {
                let (recommends, games) = try await (recommendsTask, gamesTask)
                items = recommends
                gamesByID = Dictionary(uniqueKeysWithValues: games.map { ($0.id, $0) })
            } catch {
                items = []
                gamesByID = [:]
            }
        }
    }

    func game(for item: RecommendTodayItem) -> Game? {
        gamesByID[item.appID]
    }
}
