import Foundation

struct GameEngagementSnapshot: Codable, Sendable {
    var lastPlayedGameID: String?
    var lastPlayedAt: TimeInterval?
    var playedCounts: [String: Int]
    var recentGameIDs: [String]
    var favoriteGameIDs: [String]

    enum CodingKeys: String, CodingKey {
        case lastPlayedGameID
        case lastPlayedAt
        case playedCounts
        case recentGameIDs
        case favoriteGameIDs
    }

    static let empty = GameEngagementSnapshot(
        lastPlayedGameID: nil,
        lastPlayedAt: nil,
        playedCounts: [:],
        recentGameIDs: [],
        favoriteGameIDs: []
    )

    init(
        lastPlayedGameID: String?,
        lastPlayedAt: TimeInterval?,
        playedCounts: [String: Int],
        recentGameIDs: [String],
        favoriteGameIDs: [String]
    ) {
        self.lastPlayedGameID = lastPlayedGameID
        self.lastPlayedAt = lastPlayedAt
        self.playedCounts = playedCounts
        self.recentGameIDs = recentGameIDs
        self.favoriteGameIDs = favoriteGameIDs
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.lastPlayedGameID = try container.decodeIfPresent(String.self, forKey: .lastPlayedGameID)
        self.lastPlayedAt = try container.decodeIfPresent(TimeInterval.self, forKey: .lastPlayedAt)
        self.playedCounts = try container.decodeIfPresent([String: Int].self, forKey: .playedCounts) ?? [:]
        self.recentGameIDs = try container.decodeIfPresent([String].self, forKey: .recentGameIDs) ?? []
        self.favoriteGameIDs = try container.decodeIfPresent([String].self, forKey: .favoriteGameIDs) ?? []
    }
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
        snapshot.recentGameIDs.removeAll { $0 == gameID }
        snapshot.recentGameIDs.insert(gameID, at: 0)
        if snapshot.recentGameIDs.count > 12 {
            snapshot.recentGameIDs = Array(snapshot.recentGameIDs.prefix(12))
        }

        save(snapshot)
    }

    func toggleFavorite(gameID: String) -> Bool {
        var snapshot = read()
        let updated: Bool
        if snapshot.favoriteGameIDs.contains(gameID) {
            snapshot.favoriteGameIDs.removeAll { $0 == gameID }
            updated = false
        } else {
            snapshot.favoriteGameIDs.insert(gameID, at: 0)
            updated = true
        }
        save(snapshot)
        return updated
    }

    func isFavorite(gameID: String) -> Bool {
        read().favoriteGameIDs.contains(gameID)
    }

    func applyCloudState(
        currentPlayingGameID: String?,
        recentGameIDs: [String],
        favoriteGameIDs: [String]
    ) {
        var snapshot = read()
        snapshot.lastPlayedGameID = currentPlayingGameID
        snapshot.recentGameIDs = Array(NSOrderedSet(array: recentGameIDs).array as? [String] ?? recentGameIDs)
        snapshot.favoriteGameIDs = Array(NSOrderedSet(array: favoriteGameIDs).array as? [String] ?? favoriteGameIDs)
        save(snapshot)
    }

    private func save(_ snapshot: GameEngagementSnapshot) {

        if let data = try? JSONEncoder().encode(snapshot) {
            defaults.set(data, forKey: key)
        }
    }

    func clear() {
        defaults.removeObject(forKey: key)
    }
}
