import Foundation

struct GamePackageDescriptor: Sendable {
    let gameID: String
    let version: String
    let remoteZIPURL: URL
    let md5: String?
}

enum GameUpdateMode: Sendable {
    case silent
    case force
}

struct GameInstallProgress: Sendable {
    let gameID: String
    let version: String
    let completedUnitCount: Int64
    let totalUnitCount: Int64

    var fractionCompleted: Double {
        guard totalUnitCount > 0 else { return 0 }
        return Double(completedUnitCount) / Double(totalUnitCount)
    }
}

struct LocalGameVersion: Sendable {
    let gameID: String
    let version: String
    let rootDirectory: URL
    let entryFile: URL
    let installedAt: Date
}

protocol GameStorageManagerProtocol: Sendable {
    func bootstrapStorageIfNeeded() throws

    func rootDirectory(for gameID: String) -> URL
    func versionDirectory(gameID: String, version: String) -> URL

    func currentVersion(gameID: String) async throws -> LocalGameVersion?
    func allInstalledVersions(gameID: String) async throws -> [LocalGameVersion]

    /// Download + verify + unzip to `gameID/version` folder (version-isolated).
    func install(
        package: GamePackageDescriptor,
        mode: GameUpdateMode,
        onProgress: (@Sendable (GameInstallProgress) -> Void)?
    ) async throws -> LocalGameVersion

    /// Atomically switch startup pointer after install passes integrity checks.
    func activate(gameID: String, version: String) async throws

    /// Cleanup stale versions after new version is activated.
    func pruneObsoleteVersions(gameID: String, keeping versions: Set<String>) async throws

    func cacheSizeInBytes() async -> Int64
    func clearAllLocalCaches() async throws
}
