import Foundation
import CryptoKit
import ZIPFoundation

actor VersionedGameStorageManager: GameStorageManagerProtocol {
    private let fileManager: FileManager
    private let gamesRoot: URL
    private let session: URLSession

    init(fileManager: FileManager = .default, session: URLSession = .shared) {
        self.fileManager = fileManager
        let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        self.gamesRoot = documents.appendingPathComponent("games", isDirectory: true)
        self.session = session
    }

    func bootstrapStorageIfNeeded() throws {
        if fileManager.fileExists(atPath: gamesRoot.path) == false {
            try fileManager.createDirectory(at: gamesRoot, withIntermediateDirectories: true)
        }
    }

    func rootDirectory(for gameID: String) -> URL {
        gamesRoot.appendingPathComponent(gameID, isDirectory: true)
    }

    func versionDirectory(gameID: String, version: String) -> URL {
        let safeVersion = sanitizeVersion(version)
        return rootDirectory(for: gameID)
            .appendingPathComponent("versions", isDirectory: true)
            .appendingPathComponent(safeVersion, isDirectory: true)
    }

    func currentVersion(gameID: String) async throws -> LocalGameVersion? {
        try bootstrapStorageIfNeeded()
        let root = rootDirectory(for: gameID)
        let activeFile = root.appendingPathComponent("active_version.txt")
        guard fileManager.fileExists(atPath: activeFile.path) else {
            return nil
        }
        let active = try String(contentsOf: activeFile).trimmingCharacters(in: .whitespacesAndNewlines)
        if active.isEmpty {
            return nil
        }
        let versionRoot = versionDirectory(gameID: gameID, version: active)
        guard let entry = resolveEntryFile(in: versionRoot) else {
            return nil
        }
        return LocalGameVersion(gameID: gameID, version: active, rootDirectory: versionRoot, entryFile: entry, installedAt: installDate(versionRoot))
    }

    func allInstalledVersions(gameID: String) async throws -> [LocalGameVersion] {
        try bootstrapStorageIfNeeded()
        let versionsRoot = rootDirectory(for: gameID).appendingPathComponent("versions", isDirectory: true)
        guard fileManager.fileExists(atPath: versionsRoot.path) else {
            return []
        }
        let urls = try fileManager.contentsOfDirectory(
            at: versionsRoot,
            includingPropertiesForKeys: [.isDirectoryKey, .contentModificationDateKey],
            options: [.skipsHiddenFiles]
        )

        var output: [LocalGameVersion] = []
        for url in urls {
            var isDir: ObjCBool = false
            if fileManager.fileExists(atPath: url.path, isDirectory: &isDir), isDir.boolValue {
                if let entry = resolveEntryFile(in: url) {
                    output.append(
                        LocalGameVersion(
                            gameID: gameID,
                            version: url.lastPathComponent,
                            rootDirectory: url,
                            entryFile: entry,
                            installedAt: installDate(url)
                        )
                    )
                }
            }
        }

        return output.sorted { $0.installedAt > $1.installedAt }
    }

    func install(
        package: GamePackageDescriptor,
        mode: GameUpdateMode,
        onProgress: (@Sendable (GameInstallProgress) -> Void)?
    ) async throws -> LocalGameVersion {
        _ = mode
        try bootstrapStorageIfNeeded()

        let root = rootDirectory(for: package.gameID)
        try fileManager.createDirectory(at: root, withIntermediateDirectories: true)

        let staging = root.appendingPathComponent("staging", isDirectory: true).appendingPathComponent(sanitizeVersion(package.version), isDirectory: true)
        if fileManager.fileExists(atPath: staging.path) {
            try fileManager.removeItem(at: staging)
        }
        try fileManager.createDirectory(at: staging, withIntermediateDirectories: true)

        onProgress?(GameInstallProgress(gameID: package.gameID, version: package.version, completedUnitCount: 0, totalUnitCount: 100))
        let (tempURL, _) = try await session.download(from: package.remoteZIPURL)

        if let md5 = package.md5, md5.isEmpty == false {
            let actual = try calculateMD5(fileURL: tempURL)
            if actual.caseInsensitiveCompare(md5) != .orderedSame {
                throw StorageError.checksumMismatch
            }
        }

        try fileManager.unzipItem(at: tempURL, to: staging)
        try normalizeExtractedStructure(in: staging)

        guard let entry = resolveEntryFile(in: staging) else {
            throw StorageError.entryNotFound
        }
        onProgress?(GameInstallProgress(gameID: package.gameID, version: package.version, completedUnitCount: 100, totalUnitCount: 100))
        return LocalGameVersion(gameID: package.gameID, version: package.version, rootDirectory: staging, entryFile: entry, installedAt: Date())
    }

    func activate(gameID: String, version: String) async throws {
        try bootstrapStorageIfNeeded()
        let root = rootDirectory(for: gameID)
        let versionRoot = versionDirectory(gameID: gameID, version: version)
        let staging = root.appendingPathComponent("staging", isDirectory: true).appendingPathComponent(sanitizeVersion(version), isDirectory: true)
        let source = fileManager.fileExists(atPath: versionRoot.path) ? versionRoot : staging
        if fileManager.fileExists(atPath: source.path) == false {
            throw StorageError.versionNotFound
        }

        let versionsRoot = root.appendingPathComponent("versions", isDirectory: true)
        try fileManager.createDirectory(at: versionsRoot, withIntermediateDirectories: true)

        if source.path != versionRoot.path {
            if fileManager.fileExists(atPath: versionRoot.path) {
                try fileManager.removeItem(at: versionRoot)
            }
            try fileManager.moveItem(at: source, to: versionRoot)
        }

        let active = root.appendingPathComponent("active_version.txt")
        try sanitizeVersion(version).write(to: active, atomically: true, encoding: .utf8)
    }

    func pruneObsoleteVersions(gameID: String, keeping versions: Set<String>) async throws {
        let keep = Set(versions.map { sanitizeVersion($0) })
        let versionsRoot = rootDirectory(for: gameID).appendingPathComponent("versions", isDirectory: true)
        guard fileManager.fileExists(atPath: versionsRoot.path) else {
            return
        }
        let urls = try fileManager.contentsOfDirectory(at: versionsRoot, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles])
        for url in urls where keep.contains(url.lastPathComponent) == false {
            try fileManager.removeItem(at: url)
        }
    }

    func clearAllLocalCaches() async throws {
        if fileManager.fileExists(atPath: gamesRoot.path) {
            try fileManager.removeItem(at: gamesRoot)
        }
        try bootstrapStorageIfNeeded()
    }

    private func sanitizeVersion(_ version: String) -> String {
        let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-")
        let mapped = version.unicodeScalars.map { allowed.contains($0) ? Character($0) : "_" }
        let safe = String(mapped)
        return safe.isEmpty ? "0.0.0" : safe
    }

    private func resolveEntryFile(in root: URL) -> URL? {
        let direct = root.appendingPathComponent("index.html")
        if fileManager.fileExists(atPath: direct.path) {
            return direct
        }

        guard let enumerator = fileManager.enumerator(at: root, includingPropertiesForKeys: [.isDirectoryKey], options: [.skipsHiddenFiles]) else {
            return nil
        }
        for case let fileURL as URL in enumerator {
            if fileURL.lastPathComponent.lowercased() == "index.html" {
                return fileURL
            }
        }
        return nil
    }

    private func normalizeExtractedStructure(in root: URL) throws {
        let directIndex = root.appendingPathComponent("index.html")
        if fileManager.fileExists(atPath: directIndex.path) {
            return
        }
        let children = try fileManager.contentsOfDirectory(at: root, includingPropertiesForKeys: [.isDirectoryKey], options: [.skipsHiddenFiles])
        if children.count != 1 {
            return
        }

        let child = children[0]
        var isDir: ObjCBool = false
        if fileManager.fileExists(atPath: child.path, isDirectory: &isDir), isDir.boolValue {
            let nested = try fileManager.contentsOfDirectory(at: child, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles])
            for item in nested {
                let target = root.appendingPathComponent(item.lastPathComponent)
                if fileManager.fileExists(atPath: target.path) {
                    try fileManager.removeItem(at: target)
                }
                try fileManager.moveItem(at: item, to: target)
            }
            try fileManager.removeItem(at: child)
        }
    }

    private func installDate(_ url: URL) -> Date {
        let values = try? url.resourceValues(forKeys: [.contentModificationDateKey])
        return values?.contentModificationDate ?? Date()
    }

    private func calculateMD5(fileURL: URL) throws -> String {
        let data = try Data(contentsOf: fileURL)
        let digest = Insecure.MD5.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}

enum StorageError: LocalizedError {
    case checksumMismatch
    case entryNotFound
    case versionNotFound

    var errorDescription: String? {
        switch self {
        case .checksumMismatch:
            return "Game package checksum mismatch"
        case .entryNotFound:
            return "Game entry file not found"
        case .versionNotFound:
            return "Game version does not exist"
        }
    }
}
