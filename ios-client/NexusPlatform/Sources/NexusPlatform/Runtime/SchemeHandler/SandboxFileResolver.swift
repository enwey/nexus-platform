import Foundation

struct SandboxFileResolver {
    private let fileManager: FileManager
    private let gamesRoot: URL

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
        let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        self.gamesRoot = documents.appendingPathComponent("games", isDirectory: true)
    }

    func resolveFileURL(for requestURL: URL) -> URL? {
        guard let gameID = requestURL.host, !gameID.isEmpty else {
            return nil
        }

        let root = resolveGameRoot(gameID: gameID)
        guard let root else {
            return nil
        }

        let relativePath = normalizedRelativePath(from: requestURL)
        let candidate = root.appendingPathComponent(relativePath, isDirectory: false).standardizedFileURL
        let rootStandard = root.standardizedFileURL

        guard candidate.path.hasPrefix(rootStandard.path) else {
            return nil
        }
        guard fileManager.fileExists(atPath: candidate.path) else {
            return nil
        }
        return candidate
    }

    private func resolveGameRoot(gameID: String) -> URL? {
        let gameRoot = gamesRoot.appendingPathComponent(gameID, isDirectory: true)
        let flatIndex = gameRoot.appendingPathComponent("index.html")
        if fileManager.fileExists(atPath: flatIndex.path) {
            return gameRoot
        }

        let versionsRoot = gameRoot.appendingPathComponent("versions", isDirectory: true)
        let activeVersionFile = gameRoot.appendingPathComponent("active_version.txt")
        if let activeVersion = try? String(contentsOf: activeVersionFile).trimmingCharacters(in: .whitespacesAndNewlines),
           !activeVersion.isEmpty {
            let versionRoot = versionsRoot.appendingPathComponent(activeVersion, isDirectory: true)
            let versionIndex = versionRoot.appendingPathComponent("index.html")
            if fileManager.fileExists(atPath: versionIndex.path) {
                return versionRoot
            }
        }

        guard let candidates = try? fileManager.contentsOfDirectory(
            at: versionsRoot,
            includingPropertiesForKeys: [.isDirectoryKey],
            options: [.skipsHiddenFiles]
        ) else {
            return nil
        }

        let versionDirs = candidates.filter { url in
            var isDir: ObjCBool = false
            guard fileManager.fileExists(atPath: url.path, isDirectory: &isDir), isDir.boolValue else {
                return false
            }
            return fileManager.fileExists(atPath: url.appendingPathComponent("index.html").path)
        }

        return versionDirs.sorted(by: { $0.lastPathComponent > $1.lastPathComponent }).first
    }

    private func normalizedRelativePath(from requestURL: URL) -> String {
        let rawPath = requestURL.path
        if rawPath.isEmpty || rawPath == "/" {
            return "index.html"
        }

        let trimmed = rawPath.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return trimmed.isEmpty ? "index.html" : trimmed
    }
}
