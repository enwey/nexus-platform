import Foundation

@MainActor
final class GameDetailViewModel: ObservableObject {
    @Published private(set) var runtimeProfile: RuntimeProfile?

    private let service: GameRuntimeProfileServiceProtocol

    init(service: GameRuntimeProfileServiceProtocol = GameRuntimeProfileService()) {
        self.service = service
    }

    func load(appID: String) {
        Task {
            runtimeProfile = try? await service.fetchRuntimeProfile(appID: appID)
        }
    }
}
