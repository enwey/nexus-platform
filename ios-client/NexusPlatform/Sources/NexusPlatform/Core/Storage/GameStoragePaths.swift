import Foundation

enum GameStoragePaths {
    private static let containerDirectoryName = "NexusPlatform"
    private static let gamesDirectoryName = "games"

    static func gamesRoot(fileManager: FileManager = .default) -> URL {
        let applicationSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        let baseDirectory = applicationSupport
            ?? fileManager.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? fileManager.temporaryDirectory

        return baseDirectory
            .appendingPathComponent(containerDirectoryName, isDirectory: true)
            .appendingPathComponent(gamesDirectoryName, isDirectory: true)
    }

    static func legacyGamesRoot(fileManager: FileManager = .default) -> URL {
        let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? fileManager.temporaryDirectory
        return documents.appendingPathComponent(gamesDirectoryName, isDirectory: true)
    }

    static func excludeFromBackup(at url: URL) throws {
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutableURL = url
        try mutableURL.setResourceValues(values)
    }
}
