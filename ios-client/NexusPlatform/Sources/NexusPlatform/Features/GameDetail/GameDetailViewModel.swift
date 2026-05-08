import Foundation

@MainActor
final class GameDetailViewModel: ObservableObject {
    @Published private(set) var runtimeProfile: RuntimeProfile?
    @Published private(set) var gameDetail: Game?

    private let service: GameRuntimeProfileServiceProtocol
    private let catalogService: GameCatalogServiceProtocol

    init(
        service: GameRuntimeProfileServiceProtocol = GameRuntimeProfileService(),
        catalogService: GameCatalogServiceProtocol = GameCatalogService()
    ) {
        self.service = service
        self.catalogService = catalogService
    }

    func load(appID: String) {
        Task {
            async let runtimeProfileTask = service.fetchRuntimeProfile(appID: appID)
            async let gameDetailTask = catalogService.fetchGame(appID: appID)

            runtimeProfile = try? await runtimeProfileTask
            gameDetail = try? await gameDetailTask
        }
    }
}
