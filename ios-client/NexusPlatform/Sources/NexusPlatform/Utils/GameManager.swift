import Foundation
import Alamofire
import CryptoKit
import ZipFoundation

class GameManager {
    static let shared = GameManager()
    private let fileManager = FileManager.default
    
    private init() {}
    
    func getGameDirectory(gameId: String) -> URL {
        let documentsPath = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let gamesDirectory = documentsPath.appendingPathComponent("games")
        
        if !fileManager.fileExists(atPath: gamesDirectory.path) {
            try? fileManager.createDirectory(at: gamesDirectory, withIntermediateDirectories: true)
        }
        
        return gamesDirectory.appendingPathComponent(gameId)
    }
    
    func isGameDownloaded(gameId: String) -> Bool {
        let gameDir = getGameDirectory(gameId: gameId)
        let indexPath = gameDir.appendingPathComponent("index.html")
        return fileManager.fileExists(atPath: indexPath.path)
    }
    
    func loadGame(_ game: Game, in webView: WKWebView, completion: @escaping (Bool) -> Void) {
        Task {
            do {
                let gameDir = getGameDirectory(gameId: game.id)
                
                if !isGameDownloaded(gameId: game.id) {
                    try await downloadGame(game, to: gameDir)
                }
                
                let gameUrl = URL(string: "nexus://game/index.html")!
                let request = URLRequest(url: gameUrl)
                webView.load(request)
                
                completion(true)
            } catch {
                print("Failed to load game: \(error.localizedDescription)")
                completion(false)
            }
        }
    }
    
    private func downloadGame(_ game: Game, to directory: URL) async throws {
        guard let url = URL(string: game.downloadUrl) else {
            throw GameError.invalidURL
        }
        
        let destination: DownloadRequest.Destination = { _, _ in
            let tempDir = fileManager.temporaryDirectory
            return (tempDir.appendingPathComponent("\(game.id).zip"), [.removePreviousFile, .createIntermediateDirectories])
        }
        
        let (downloadUrl, response) = try await AF.download(url, to: destination)
            .serializingDownloadedFileURL()
            .value
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw GameError.downloadFailed
        }
        
        let downloadedFile = downloadUrl
        
        if !game.md5.isEmpty {
            let actualMD5 = calculateMD5(file: downloadedFile)
            if actualMD5 != game.md5 {
                try? fileManager.removeItem(at: downloadedFile)
                throw GameError.md5Mismatch
            }
        }
        
        try unzipFile(downloadedFile, to: directory)
        try? fileManager.removeItem(at: downloadedFile)
    }
    
    private func unzipFile(_ zipFile: URL, to destination: URL) throws {
        try fileManager.unzipItem(at: zipFile, to: destination)
    }
    
    private func calculateMD5(file: URL) -> String {
        guard let data = try? Data(contentsOf: file) else {
            return ""
        }
        
        let hash = Insecure.MD5.hash(data: data)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }
    
    func getSDKContent() -> String {
        let documentsPath = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let sdkPath = documentsPath.appendingPathComponent("wx-mock-sdk.js")
        
        guard let content = try? String(contentsOf: sdkPath) else {
            return ""
        }
        
        return content
    }
}

enum GameError: Error, LocalizedError {
    case invalidURL
    case downloadFailed
    case md5Mismatch
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "无效的 URL"
        case .downloadFailed:
            return "下载失败"
        case .md5Mismatch:
            return "MD5 校验失败"
        }
    }
}
