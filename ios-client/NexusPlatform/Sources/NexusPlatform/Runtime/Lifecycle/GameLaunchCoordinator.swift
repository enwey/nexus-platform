import Foundation

struct GameLaunchResult: Sendable {
    let activeVersion: String
    let forceUpdated: Bool
}

extension Notification.Name {
    static let gameLocalMetadataDidChange = Notification.Name("gameLocalMetadataDidChange")
}

enum GameLaunchError: LocalizedError {
    case updateCheckFailed(String)
    case packageInstallFailed(String)
    case forceUpdateFailed(String)
    case fallbackDownloadFailed(String)

    var errorDescription: String? {
        switch self {
        case .updateCheckFailed(let msg):
            return AppText.launchUpdateCheckFailed(msg)
        case .packageInstallFailed(let msg):
            return AppText.packageInstallFailed(msg)
        case .forceUpdateFailed(let msg):
            return AppText.forceUpdateFailed(msg)
        case .fallbackDownloadFailed(let msg):
            return AppText.fallbackDownloadFailed(msg)
        }
    }
}

actor GameLaunchCoordinator {
    private let updateService: GameUpdateCheckServiceProtocol
    private let storage: GameStorageManagerProtocol
    private let updateState: RuntimeUpdateStateStore

    init(
        updateService: GameUpdateCheckServiceProtocol = BackendGameUpdateService(),
        storage: GameStorageManagerProtocol = VersionedGameStorageManager(),
        updateState: RuntimeUpdateStateStore = .shared
    ) {
        self.updateService = updateService
        self.storage = storage
        self.updateState = updateState
    }

    func prepareLaunch(
        game: Game,
        forceProgress: (@Sendable (GameInstallProgress) -> Void)? = nil
    ) async throws -> GameLaunchResult {
        try storage.bootstrapStorageIfNeeded()
        await updateState.setCurrentGameID(game.id)
        let current = try await storage.currentVersion(gameID: game.id)
        let localVersion = current?.version ?? fallbackVersion(for: game.version)

        let updateInfo: GameUpdateInfo?
        do {
            updateInfo = try await updateService.checkUpdate(appID: game.id, localVersion: localVersion)
        } catch {
            if current == nil {
                throw GameLaunchError.updateCheckFailed(error.localizedDescription)
            }
            updateInfo = nil
        }
        let hasUpdate = updateInfo?.hasUpdate == true
        let force = updateInfo?.forceUpdate == true

        if force, hasUpdate, let latest = updateInfo?.latestVersion, let zip = updateInfo?.downloadURL {
            do {
                await updateState.markPending(gameID: game.id, version: latest, force: true)
                try await forceInstallWithRetry(
                    gameID: game.id,
                    version: latest,
                    zipURL: zip,
                    md5: updateInfo?.md5,
                    forceProgress: forceProgress
                )
                notifyLocalMetadataChanged(gameID: game.id)
                await updateState.markNoUpdate(gameID: game.id)
                return GameLaunchResult(activeVersion: latest, forceUpdated: true)
            } catch {
                await updateState.markFailed(gameID: game.id, message: error.localizedDescription)
                throw GameLaunchError.forceUpdateFailed(error.localizedDescription)
            }
        }

        if current == nil {
            do {
                if hasUpdate, let latest = updateInfo?.latestVersion, let zip = updateInfo?.downloadURL {
                    let descriptor = GamePackageDescriptor(gameID: game.id, version: latest, remoteZIPURL: zip, md5: updateInfo?.md5)
                    await updateState.markPending(gameID: game.id, version: latest, force: false)
                    _ = try await storage.install(package: descriptor, mode: .silent, onProgress: forceProgress)
                    try await storage.activate(gameID: game.id, version: latest)
                    try await storage.pruneObsoleteVersions(gameID: game.id, keeping: [latest])
                    notifyLocalMetadataChanged(gameID: game.id)
                    await updateState.markNoUpdate(gameID: game.id)
                    return GameLaunchResult(activeVersion: latest, forceUpdated: false)
                }

                let fallbackDescriptor = try fallbackDescriptor(game: game)
                _ = try await storage.install(package: fallbackDescriptor, mode: .silent, onProgress: forceProgress)
                try await storage.activate(gameID: game.id, version: fallbackDescriptor.version)
                try await storage.pruneObsoleteVersions(gameID: game.id, keeping: [fallbackDescriptor.version])
                notifyLocalMetadataChanged(gameID: game.id)
                await updateState.markNoUpdate(gameID: game.id)
                return GameLaunchResult(activeVersion: fallbackDescriptor.version, forceUpdated: false)
            } catch {
                await updateState.markFailed(gameID: game.id, message: error.localizedDescription)
                throw GameLaunchError.fallbackDownloadFailed(error.localizedDescription)
            }
        }

        if hasUpdate, let latest = updateInfo?.latestVersion, let zip = updateInfo?.downloadURL {
            let md5 = updateInfo?.md5
            let gameID = game.id
            await updateState.markPending(gameID: gameID, version: latest, force: false)
            Task.detached {
                let descriptor = GamePackageDescriptor(gameID: gameID, version: latest, remoteZIPURL: zip, md5: md5)
                do {
                    _ = try await self.storage.install(package: descriptor, mode: .silent, onProgress: nil)
                    await self.updateState.markReady(gameID: gameID)
                } catch {
                    await self.updateState.markFailed(gameID: gameID, message: error.localizedDescription)
                }
            }
        } else {
            await updateState.markNoUpdate(gameID: game.id)
        }

        return GameLaunchResult(activeVersion: current?.version ?? localVersion, forceUpdated: false)
    }

    private func fallbackDescriptor(game: Game) throws -> GamePackageDescriptor {
        guard let url = URL(string: game.downloadUrl), game.downloadUrl.isEmpty == false else {
            throw URLError(.badURL)
        }
        return GamePackageDescriptor(
            gameID: game.id,
            version: fallbackVersion(for: game.version),
            remoteZIPURL: url,
            md5: game.md5.isEmpty ? nil : game.md5
        )
    }

    private func fallbackVersion(for raw: String) -> String {
        raw.isEmpty ? "0.0.0" : raw
    }

    private func forceInstallWithRetry(
        gameID: String,
        version: String,
        zipURL: URL,
        md5: String?,
        forceProgress: (@Sendable (GameInstallProgress) -> Void)?
    ) async throws {
        let descriptor = GamePackageDescriptor(gameID: gameID, version: version, remoteZIPURL: zipURL, md5: md5)
        var lastError: Error?
        for _ in 0..<2 {
            do {
                _ = try await storage.install(package: descriptor, mode: .force, onProgress: forceProgress)
                try await storage.activate(gameID: gameID, version: version)
                try await storage.pruneObsoleteVersions(gameID: gameID, keeping: [version])
                return
            } catch {
                lastError = error
            }
        }
        throw lastError ?? GameLaunchError.packageInstallFailed("未知错误")
    }

    private func notifyLocalMetadataChanged(gameID: String) {
        NotificationCenter.default.post(name: .gameLocalMetadataDidChange, object: gameID)
    }
}
