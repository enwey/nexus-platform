import Foundation
import CryptoKit
import CryptoSwift
import WebKit
import ZIPFoundation

actor VersionedGameStorageManager: @preconcurrency GameStorageManagerProtocol {
    private let securePackageV1 = "NEXUS_SECURE_ZIP_V1"
    private let supportedPackageFormatsHeader = "NEXUS_SECURE_ZIP_V1"
    private let cryptoChunkSize = 64 * 1024
    private let fileManager: FileManager
    private let gamesRoot: URL
    private let legacyGamesRoot: URL
    private let session: URLSession

    init(fileManager: FileManager = .default, session: URLSession = BackendPinnedSession.shared) {
        self.fileManager = fileManager
        self.gamesRoot = GameStoragePaths.gamesRoot(fileManager: fileManager)
        self.legacyGamesRoot = GameStoragePaths.legacyGamesRoot(fileManager: fileManager)
        self.session = session
    }

    func bootstrapStorageIfNeeded() throws {
        try migrateLegacyStorageIfNeeded()
        if fileManager.fileExists(atPath: gamesRoot.path) == false {
            try fileManager.createDirectory(at: gamesRoot, withIntermediateDirectories: true)
        }
        try markGamesRootExcludedFromBackup()
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

        if let securePackage = try extractSecurePackage(from: tempURL) {
            let decryptedZIP = try await decryptSecurePackage(securePackage, package: package)
            defer {
                try? fileManager.removeItem(at: securePackage.workingDirectory)
                try? fileManager.removeItem(at: decryptedZIP)
            }
            try fileManager.unzipItem(at: decryptedZIP, to: staging)
        } else {
            try fileManager.unzipItem(at: tempURL, to: staging)
        }
        try normalizeExtractedStructure(in: staging)

        guard let entry = resolveEntryFile(in: staging) else {
            throw StorageError.entryNotFound
        }
        try ensureHostedManifest(in: staging, package: package, entryFile: entry)
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
        URLCache.shared.removeAllCachedResponses()

        if fileManager.fileExists(atPath: gamesRoot.path) {
            try fileManager.removeItem(at: gamesRoot)
        }
        if legacyGamesRoot.path != gamesRoot.path, fileManager.fileExists(atPath: legacyGamesRoot.path) {
            try fileManager.removeItem(at: legacyGamesRoot)
        }

        try clearDirectoryContents(at: fileManager.temporaryDirectory)
        try await clearWebKitCache()
        try bootstrapStorageIfNeeded()
    }

    func cacheSizeInBytes() async -> Int64 {
        let urls = measurableCacheDirectories()
        let fileBytes = urls.reduce(into: Int64.zero) { total, url in
            total += directorySize(at: url)
        }
        return fileBytes
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

    private func ensureHostedManifest(in root: URL, package: GamePackageDescriptor, entryFile: URL) throws {
        let manifestURL = root.appendingPathComponent("manifest.json")
        let relativeEntry = entryFile.path.replacingOccurrences(of: root.path + "/", with: "")

        if fileManager.fileExists(atPath: manifestURL.path) {
            let data = try Data(contentsOf: manifestURL)
            let manifest = try JSONDecoder().decode(HostedMiniAppManifest.self, from: data)
            let normalized = try normalizedHostedManifest(manifest, package: package, relativeEntry: relativeEntry)
            if normalized != manifest {
                let normalizedData = try JSONEncoder().encode(normalized)
                try normalizedData.write(to: manifestURL, options: .atomic)
            }
            return
        }

        let generated = HostedMiniAppManifest(
            appId: package.gameID,
            version: package.version,
            name: package.gameID,
            description: nil,
            entry: relativeEntry,
            icon: nil,
            kind: "html5-mini-game",
            locales: nil,
            metadata: HostedMiniAppManifestMetadata(icon: nil, kind: "html5-mini-game", locales: nil)
        )
        let data = try JSONEncoder().encode(generated)
        try data.write(to: manifestURL, options: .atomic)
    }

    private func normalizedHostedManifest(
        _ manifest: HostedMiniAppManifest,
        package: GamePackageDescriptor,
        relativeEntry: String
    ) throws -> HostedMiniAppManifest {
        let entry = manifest.resolvedEntry.trimmingCharacters(in: .whitespacesAndNewlines)
        if entry.isEmpty || entry != relativeEntry {
            throw StorageError.invalidHostedManifest
        }

        let normalizedAppID: String?
        if let appID = manifest.appId?.trimmingCharacters(in: .whitespacesAndNewlines),
           appID.isEmpty == false {
            if appID == package.gameID || appID == "replace-with-generated-app-id" {
                normalizedAppID = package.gameID
            } else {
                throw StorageError.invalidHostedManifest
            }
        } else {
            normalizedAppID = package.gameID
        }

        let normalizedVersion: String?
        if let version = manifest.version?.trimmingCharacters(in: .whitespacesAndNewlines),
           version.isEmpty == false {
            if sanitizeVersion(version) == sanitizeVersion(package.version) {
                normalizedVersion = package.version
            } else {
                throw StorageError.invalidHostedManifest
            }
        } else {
            normalizedVersion = package.version
        }

        let allowedKinds = ["html5-mini-game", "html5-mini-app", "html5"]
        let resolvedKind = manifest.resolvedKind.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if allowedKinds.contains(resolvedKind) == false {
            throw StorageError.invalidHostedManifest
        }

        let normalizedKind = resolvedKind.isEmpty ? "html5-mini-game" : resolvedKind
        let normalizedMetadata = HostedMiniAppManifestMetadata(
            icon: manifest.metadata?.icon,
            kind: normalizedKind,
            locales: manifest.metadata?.locales
        )

        return HostedMiniAppManifest(
            appId: normalizedAppID,
            version: normalizedVersion,
            name: manifest.name,
            description: manifest.description,
            entry: relativeEntry,
            icon: manifest.icon,
            kind: normalizedKind,
            locales: manifest.locales,
            metadata: normalizedMetadata
        )
    }

    private func calculateMD5(fileURL: URL) throws -> String {
        let handle = try FileHandle(forReadingFrom: fileURL)
        defer { try? handle.close() }

        var hasher = Insecure.MD5()
        while let chunk = try handle.read(upToCount: cryptoChunkSize), chunk.isEmpty == false {
            hasher.update(data: chunk)
        }

        let digest = hasher.finalize()
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    private func extractSecurePackage(from packageURL: URL) throws -> SecurePackageContainer? {
        let workingDirectory = fileManager.temporaryDirectory
            .appendingPathComponent("secure_pkg_\(UUID().uuidString)", isDirectory: true)
        try fileManager.createDirectory(at: workingDirectory, withIntermediateDirectories: true)
        try fileManager.unzipItem(at: packageURL, to: workingDirectory)

        let manifestURL = workingDirectory.appendingPathComponent("nexus-package.json")
        guard fileManager.fileExists(atPath: manifestURL.path) else {
            try? fileManager.removeItem(at: workingDirectory)
            return nil
        }

        let manifestData = try Data(contentsOf: manifestURL)
        let manifest = try JSONDecoder().decode(SecurePackageManifest.self, from: manifestData)

        let payloadURL = workingDirectory.appendingPathComponent(manifest.payloadFile)
        guard fileManager.fileExists(atPath: payloadURL.path) else {
            throw StorageError.securePayloadMissing
        }

        return SecurePackageContainer(manifest: manifest, payloadURL: payloadURL, workingDirectory: workingDirectory)
    }

    private func decryptSecurePackage(_ securePackage: SecurePackageContainer, package: GamePackageDescriptor) async throws -> URL {
        switch securePackage.manifest.format {
        case securePackageV1:
            return try await decryptSecurePackageV1(securePackage, package: package)
        default:
            throw StorageError.unsupportedSecurePackage
        }
    }

    private func decryptSecurePackageV1(_ securePackage: SecurePackageContainer, package: GamePackageDescriptor) async throws -> URL {
        let contentKey = try await fetchRuntimePackageKey(gameID: package.gameID, version: package.version)
        let nonceData = Data(base64Encoded: securePackage.manifest.encryption.nonce) ?? Data()
        guard nonceData.count == 12 else {
            throw StorageError.invalidSecurePackage
        }
        let outputURL = fileManager.temporaryDirectory.appendingPathComponent("secure_plain_\(UUID().uuidString).zip")
        try decryptSecurePayload(
            inputURL: securePackage.payloadURL,
            outputURL: outputURL,
            key: contentKey,
            nonce: nonceData
        )
        return outputURL
    }

    private func fetchRuntimePackageKey(gameID: String, version: String) async throws -> Data {
        guard let authSession = await AuthSessionStore.shared.current(),
              authSession.accessToken.isEmpty == false else {
            throw StorageError.runtimeKeyUnavailable
        }
        let deviceID = runtimeDeviceID()

        let environment = try BackendEnvironment.current()

        var ticketComponents = URLComponents(
            url: environment
                .apiBaseURL
                .appendingPathComponent("game")
                .appendingPathComponent(gameID)
                .appendingPathComponent("runtime-ticket"),
            resolvingAgainstBaseURL: false
        )
        ticketComponents?.queryItems = [URLQueryItem(name: "version", value: version)]
        guard let ticketURL = ticketComponents?.url else {
            throw StorageError.invalidSecurePackage
        }

        let client = BackendAPIClient(session: session, baseURL: environment.apiBaseURL)
        guard let ticketPayload = try await client.request(
            url: ticketURL,
            authMode: .required,
            extraHeaders: [
                "X-Nexus-Device-Id": deviceID,
                "X-Nexus-Supported-Package-Formats": supportedPackageFormatsHeader
            ]
        ) as? [String: Any],
              let ticket = ticketPayload["ticket"] as? String,
              let ticketVersion = ticketPayload["version"] as? String else {
            throw StorageError.runtimeKeyUnavailable
        }

        let redeemURL = environment
            .apiBaseURL
            .appendingPathComponent("game")
            .appendingPathComponent(gameID)
            .appendingPathComponent("runtime-key")
            .appendingPathComponent("redeem")

        guard let payload = try await client.request(
            url: redeemURL,
            method: "POST",
            body: [
                "ticket": ticket,
                "version": ticketVersion
            ],
            authMode: .required,
            extraHeaders: [
                "X-Nexus-Device-Id": deviceID,
                "X-Nexus-Supported-Package-Formats": supportedPackageFormatsHeader
            ]
        ) as? [String: Any],
              let key = payload["key"] as? String,
              let keyData = Data(base64Encoded: key),
              keyData.isEmpty == false else {
            throw StorageError.runtimeKeyUnavailable
        }
        return keyData
    }

    private func directorySize(at root: URL) -> Int64 {
        guard fileManager.fileExists(atPath: root.path) else {
            return 0
        }
        guard let enumerator = fileManager.enumerator(
            at: root,
            includingPropertiesForKeys: [.isRegularFileKey, .fileSizeKey],
            options: [.skipsHiddenFiles]
        ) else {
            return 0
        }

        var total: Int64 = 0
        for case let fileURL as URL in enumerator {
            guard
                let values = try? fileURL.resourceValues(forKeys: [.isRegularFileKey, .fileSizeKey]),
                values.isRegularFile == true
            else {
                continue
            }
            total += Int64(values.fileSize ?? 0)
        }
        return total
    }

    private func clearDirectoryContents(at root: URL) throws {
        guard fileManager.fileExists(atPath: root.path) else {
            return
        }
        let entries = try fileManager.contentsOfDirectory(at: root, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles])
        for entry in entries {
            try? fileManager.removeItem(at: entry)
        }
    }

    private func measurableCacheDirectories() -> [URL] {
        return Array(
            Set([
                gamesRoot,
                legacyGamesRoot,
                fileManager.temporaryDirectory
            ])
        )
    }

    private func clearWebKitCache() async {
        let store = WKWebsiteDataStore.default()
        let dataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
        await withCheckedContinuation { continuation in
            store.fetchDataRecords(ofTypes: dataTypes) { records in
                store.removeData(ofTypes: dataTypes, for: records) {
                    continuation.resume()
                }
            }
        }
    }

    private func markGamesRootExcludedFromBackup() throws {
        if fileManager.fileExists(atPath: gamesRoot.path) {
            try GameStoragePaths.excludeFromBackup(at: gamesRoot)
        }
    }

    private func migrateLegacyStorageIfNeeded() throws {
        let legacyRoot = legacyGamesRoot.standardizedFileURL
        let currentRoot = gamesRoot.standardizedFileURL
        guard legacyRoot.path != currentRoot.path else { return }
        guard fileManager.fileExists(atPath: legacyRoot.path) else { return }

        try fileManager.createDirectory(at: currentRoot.deletingLastPathComponent(), withIntermediateDirectories: true)
        if fileManager.fileExists(atPath: currentRoot.path) == false {
            try fileManager.moveItem(at: legacyRoot, to: currentRoot)
            return
        }

        let entries = try fileManager.contentsOfDirectory(at: legacyRoot, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles])
        for entry in entries {
            let target = currentRoot.appendingPathComponent(entry.lastPathComponent, isDirectory: true)
            if fileManager.fileExists(atPath: target.path) {
                try? fileManager.removeItem(at: entry)
                continue
            }
            try fileManager.moveItem(at: entry, to: target)
        }

        let remainingEntries = try fileManager.contentsOfDirectory(at: legacyRoot, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles])
        if remainingEntries.isEmpty {
            try? fileManager.removeItem(at: legacyRoot)
        }
    }

    private func decryptSecurePayload(
        inputURL: URL,
        outputURL: URL,
        key: Data,
        nonce: Data
    ) throws {
        let inputHandle = try FileHandle(forReadingFrom: inputURL)
        defer { try? inputHandle.close() }

        let totalSize = try inputHandle.seekToEnd()
        let tagLength = 16
        guard totalSize > UInt64(tagLength) else {
            throw StorageError.invalidSecurePackage
        }

        try inputHandle.seek(toOffset: totalSize - UInt64(tagLength))
        let tagData = try inputHandle.read(upToCount: tagLength) ?? Data()
        guard tagData.count == tagLength else {
            throw StorageError.invalidSecurePackage
        }

        if fileManager.fileExists(atPath: outputURL.path) {
            try fileManager.removeItem(at: outputURL)
        }
        fileManager.createFile(atPath: outputURL.path, contents: nil)
        let outputHandle = try FileHandle(forWritingTo: outputURL)
        defer { try? outputHandle.close() }

        do {
            try inputHandle.seek(toOffset: 0)
            let gcm = CryptoSwift.GCM(iv: Array(nonce), authenticationTag: Array(tagData), mode: .detached)
            let aes = try CryptoSwift.AES(key: Array(key), blockMode: gcm, padding: .noPadding)
            var decryptor = try aes.makeDecryptor()
            var remainingCiphertextBytes = Int(totalSize) - tagLength

            while remainingCiphertextBytes > 0 {
                let chunkSize = min(cryptoChunkSize, remainingCiphertextBytes)
                let chunk = try inputHandle.read(upToCount: chunkSize) ?? Data()
                guard chunk.isEmpty == false else {
                    throw StorageError.invalidSecurePackage
                }

                remainingCiphertextBytes -= chunk.count
                let plainChunk = try decryptor.update(withBytes: Array(chunk))
                if plainChunk.isEmpty == false {
                    try outputHandle.write(contentsOf: Data(plainChunk))
                }
            }

            let finalChunk = try decryptor.finish()
            if finalChunk.isEmpty == false {
                try outputHandle.write(contentsOf: Data(finalChunk))
            }
        } catch {
            try? fileManager.removeItem(at: outputURL)
            throw error
        }
    }
}

private func runtimeDeviceID() -> String {
    let defaults = UserDefaults.standard
    let key = "nexus.runtime.device.id"
    if let existing = defaults.string(forKey: key)?.trimmingCharacters(in: .whitespacesAndNewlines),
       existing.isEmpty == false {
        return existing
    }
    let generated = "rtdev_" + UUID().uuidString.replacingOccurrences(of: "-", with: "")
    defaults.set(generated, forKey: key)
    return generated
}

enum StorageError: LocalizedError {
    case checksumMismatch
    case entryNotFound
    case versionNotFound
    case runtimeKeyUnavailable
    case invalidHostedManifest
    case invalidSecurePackage
    case securePayloadMissing
    case unsupportedSecurePackage

    var errorDescription: String? {
        switch self {
        case .checksumMismatch:
            return AppText.checksumMismatch()
        case .entryNotFound:
            return AppText.entryFileMissing()
        case .versionNotFound:
            return AppText.versionMissing()
        case .runtimeKeyUnavailable:
            return AppText.runtimeKeyUnavailable()
        case .invalidHostedManifest:
            return AppText.invalidHostedManifest()
        case .invalidSecurePackage:
            return AppText.invalidSecurePackage()
        case .securePayloadMissing:
            return AppText.securePayloadMissing()
        case .unsupportedSecurePackage:
            return AppText.unsupportedSecurePackage()
        }
    }
}

private struct SecurePackageManifest: Decodable {
    let format: String
    let entryFile: String
    let payloadFile: String
    let sourceMd5: String?
    let sourceSize: Int?
    let encryption: SecurePackageEncryption
}

private struct SecurePackageEncryption: Decodable {
    let algorithm: String
    let nonce: String
}

private struct SecurePackageContainer {
    let manifest: SecurePackageManifest
    let payloadURL: URL
    let workingDirectory: URL
}
