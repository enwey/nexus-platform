import Foundation
import CryptoKit
import WebKit
import ZIPFoundation

actor VersionedGameStorageManager: @preconcurrency GameStorageManagerProtocol {
    private let securePackageV1 = "NEXUS_SECURE_ZIP_V1"
    private let supportedPackageFormatsHeader = "NEXUS_SECURE_ZIP_V1"
    private let fileManager: FileManager
    private let gamesRoot: URL
    private let session: URLSession

    init(fileManager: FileManager = .default, session: URLSession = BackendPinnedSession.shared) {
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

        try clearDirectoryContents(at: fileManager.temporaryDirectory)
        try await clearWebKitCache()
        try bootstrapStorageIfNeeded()
    }

    func cacheSizeInBytes() async -> Int64 {
        let urls = cacheDirectories()
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

    private func calculateMD5(fileURL: URL) throws -> String {
        let data = try Data(contentsOf: fileURL)
        let digest = Insecure.MD5.hash(data: data)
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
        let encryptedPayload = try Data(contentsOf: securePackage.payloadURL)
        let nonceData = Data(base64Encoded: securePackage.manifest.encryption.nonce) ?? Data()
        guard nonceData.count == 12 else {
            throw StorageError.invalidSecurePackage
        }
        guard encryptedPayload.count > 16 else {
            throw StorageError.invalidSecurePackage
        }

        let cipherText = encryptedPayload.dropLast(16)
        let tag = encryptedPayload.suffix(16)
        let sealedBox = try AES.GCM.SealedBox(
            nonce: AES.GCM.Nonce(data: nonceData),
            ciphertext: Data(cipherText),
            tag: Data(tag)
        )
        let plainZIP = try AES.GCM.open(sealedBox, using: SymmetricKey(data: contentKey))
        let outputURL = fileManager.temporaryDirectory.appendingPathComponent("secure_plain_\(UUID().uuidString).zip")
        try Data(plainZIP).write(to: outputURL, options: .atomic)
        return outputURL
    }

    private func fetchRuntimePackageKey(gameID: String, version: String) async throws -> Data {
        guard let authSession = await AuthSessionStore.shared.current(),
              authSession.accessToken.isEmpty == false else {
            throw StorageError.runtimeKeyUnavailable
        }
        let deviceID = runtimeDeviceID()

        var ticketComponents = URLComponents(
            url: BackendEnvironment.current()
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

        let client = BackendAPIClient(session: session, baseURL: BackendEnvironment.current().apiBaseURL)
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

        let redeemURL = BackendEnvironment.current()
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

    private func cacheDirectories() -> [URL] {
        let libraryRoot = fileManager.urls(for: .libraryDirectory, in: .userDomainMask).first
        let cachesRoot = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first

        return [
            gamesRoot,
            fileManager.temporaryDirectory,
            libraryRoot?.appendingPathComponent("WebKit", isDirectory: true),
            cachesRoot?.appendingPathComponent("WebKit", isDirectory: true),
            cachesRoot?.appendingPathComponent("com.apple.WebKit.Networking", isDirectory: true)
        ].compactMap { $0 }
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
