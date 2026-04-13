import Foundation

struct GameEngagementSnapshot: Codable, Sendable {
    var lastPlayedGameID: String?
    var lastPlayedAt: TimeInterval?
    var playedCounts: [String: Int]

    static let empty = GameEngagementSnapshot(lastPlayedGameID: nil, lastPlayedAt: nil, playedCounts: [:])
}

actor GameEngagementStore {
    static let shared = GameEngagementStore()

    private let key = "nexus.engagement.snapshot"
    private let defaults = UserDefaults.standard

    func read() -> GameEngagementSnapshot {
        guard let data = defaults.data(forKey: key),
              let snapshot = try? JSONDecoder().decode(GameEngagementSnapshot.self, from: data) else {
            return .empty
        }
        return snapshot
    }

    func markPlayed(gameID: String) {
        var snapshot = read()
        snapshot.lastPlayedGameID = gameID
        snapshot.lastPlayedAt = Date().timeIntervalSince1970
        snapshot.playedCounts[gameID, default: 0] += 1

        if let data = try? JSONEncoder().encode(snapshot) {
            defaults.set(data, forKey: key)
        }
    }

    func clear() {
        defaults.removeObject(forKey: key)
    }
}
