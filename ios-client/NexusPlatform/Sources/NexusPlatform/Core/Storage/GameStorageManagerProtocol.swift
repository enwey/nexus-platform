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

struct HostedMiniAppManifestLocale: Codable, Hashable, Sendable {
    let name: String?
    let description: String?
}

struct HostedMiniAppManifestMetadata: Codable, Hashable, Sendable {
    let icon: String?
    let kind: String?
    let locales: [String: HostedMiniAppManifestLocale]?
}

struct HostedMiniAppManifest: Codable, Hashable, Sendable {
    let appId: String?
    let version: String?
    let name: String?
    let description: String?
    let entry: String?
    let icon: String?
    let kind: String?
    let locales: [String: HostedMiniAppManifestLocale]?
    let metadata: HostedMiniAppManifestMetadata?

    var resolvedEntry: String {
        let raw = entry?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return raw.isEmpty ? "index.html" : raw
    }

    var resolvedIconPath: String? {
        let candidates = [
            metadata?.icon?.trimmingCharacters(in: .whitespacesAndNewlines),
            icon?.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
        return candidates.first(where: { ($0?.isEmpty == false) }) ?? nil
    }

    var resolvedKind: String {
        let raw = metadata?.kind?.trimmingCharacters(in: .whitespacesAndNewlines)
            ?? kind?.trimmingCharacters(in: .whitespacesAndNewlines)
            ?? "html5-mini-game"
        return raw.isEmpty ? "html5-mini-game" : raw
    }

    var resolvedLocales: [String: HostedMiniAppManifestLocale] {
        let topLevel = locales ?? [:]
        let nested = metadata?.locales ?? [:]
        return topLevel.merging(nested) { current, _ in current }
    }
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
