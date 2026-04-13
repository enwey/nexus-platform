import Foundation

struct RuntimeUpdateSnapshot: Sendable {
    let gameID: String
    let hasUpdate: Bool
    let ready: Bool
    let force: Bool
    let latestVersion: String?
    let errMsg: String
}

actor RuntimeUpdateStateStore {
    static let shared = RuntimeUpdateStateStore()

    private struct PendingUpdate: Sendable {
        let gameID: String
        let version: String
        let force: Bool
        var ready: Bool
        var failed: Bool
        var message: String
    }

    private var currentGameID: String?
    private var pending: [String: PendingUpdate] = [:]

    func setCurrentGameID(_ gameID: String) {
        currentGameID = gameID
    }

    func currentGame() -> String? {
        currentGameID
    }

    func markNoUpdate(gameID: String) {
        pending.removeValue(forKey: gameID)
    }

    func markPending(gameID: String, version: String, force: Bool) {
        pending[gameID] = PendingUpdate(
            gameID: gameID,
            version: version,
            force: force,
            ready: false,
            failed: false,
            message: "update.check:ok"
        )
    }

    func markReady(gameID: String) {
        guard var state = pending[gameID] else { return }
        state.ready = true
        state.failed = false
        state.message = "update.check:ok"
        pending[gameID] = state
    }

    func markFailed(gameID: String, message: String) {
        guard var state = pending[gameID] else { return }
        state.failed = true
        state.ready = false
        state.message = message.isEmpty ? "update.check:fail" : message
        pending[gameID] = state
    }

    func snapshot(gameID: String) -> RuntimeUpdateSnapshot {
        if let state = pending[gameID] {
            return RuntimeUpdateSnapshot(
                gameID: gameID,
                hasUpdate: true,
                ready: state.ready,
                force: state.force,
                latestVersion: state.version,
                errMsg: state.message
            )
        }
        return RuntimeUpdateSnapshot(
            gameID: gameID,
            hasUpdate: false,
            ready: false,
            force: false,
            latestVersion: nil,
            errMsg: "update.check:ok"
        )
    }

    func applyPending(gameID: String, storage: GameStorageManagerProtocol) async throws {
        guard let state = pending[gameID] else { return }
        guard state.ready else {
            throw GameLaunchError.packageInstallFailed("更新包尚未下载完成")
        }
        try await storage.activate(gameID: gameID, version: state.version)
        try await storage.pruneObsoleteVersions(gameID: gameID, keeping: [state.version])
        pending.removeValue(forKey: gameID)
    }
}
