import Foundation

@MainActor
final class GameDetailViewModel: ObservableObject {
    @Published private(set) var runtimeProfile: RuntimeProfile?
    @Published private(set) var gameDetail: Game?
    @Published private(set) var discoverRank: Int?

    private let service: GameRuntimeProfileServiceProtocol
    private let catalogService: GameCatalogServiceProtocol
    private let discoverHomeService: DiscoverHomeServiceProtocol

    init(
        service: GameRuntimeProfileServiceProtocol = GameRuntimeProfileService(),
        catalogService: GameCatalogServiceProtocol = GameCatalogService(),
        discoverHomeService: DiscoverHomeServiceProtocol = DiscoverHomeService()
    ) {
        self.service = service
        self.catalogService = catalogService
        self.discoverHomeService = discoverHomeService
    }

    func load(appID: String) {
        Task {
            async let runtimeProfileTask = service.fetchRuntimeProfile(appID: appID)
            async let gameDetailTask = catalogService.fetchGame(appID: appID)
            async let discoverHomeTask = discoverHomeService.fetchHome(limit: 20)

            runtimeProfile = try? await runtimeProfileTask
            gameDetail = try? await gameDetailTask

            if let discoverHome = try? await discoverHomeTask,
               let index = discoverHome.rankedGames.firstIndex(where: { $0.id == appID }) {
                discoverRank = index + 1
            } else {
                discoverRank = nil
            }
        }
    }
}
