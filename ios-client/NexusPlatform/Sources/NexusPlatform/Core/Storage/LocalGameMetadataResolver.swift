import Foundation

actor LocalGameMetadataResolver {
    static let shared = LocalGameMetadataResolver()

    private let storageManager: VersionedGameStorageManager

    init(storageManager: VersionedGameStorageManager = VersionedGameStorageManager()) {
        self.storageManager = storageManager
    }

    func merge(_ games: [Game]) async -> [Game] {
        var merged: [Game] = []
        merged.reserveCapacity(games.count)
        for game in games {
            merged.append(await merge(game))
        }
        return merged
    }

    func merge(_ game: Game) async -> Game {
        let language = AppLanguageStore.currentSync()
        let localizedRemote = game.applyingPresentation(
            name: game.localizedName(for: language),
            description: game.localizedDescription(for: language)
        )

        guard let local = try? await storageManager.currentVersion(gameID: game.id) else {
            return localizedRemote
        }

        let manifestURL = local.rootDirectory.appendingPathComponent("manifest.json")
        guard let data = try? Data(contentsOf: manifestURL),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return localizedRemote
        }

        let manifest = ParsedManifest(payload: json)
        let iconURL: String? = {
            guard let iconPath = manifest.iconPath?.trimmingCharacters(in: .whitespacesAndNewlines),
                  iconPath.isEmpty == false else {
                return localizedRemote.iconUrl
            }
            let fileURL = local.rootDirectory.appendingPathComponent(iconPath)
            return FileManager.default.fileExists(atPath: fileURL.path) ? fileURL.absoluteString : localizedRemote.iconUrl
        }()

        return localizedRemote.applyingPresentation(
            name: manifest.localizedName(for: language).ifEmpty(localizedRemote.name),
            description: manifest.localizedDescription(for: language).ifEmpty(localizedRemote.description),
            iconUrl: iconURL
        )
    }
}

private struct ParsedManifest {
    let name: String
    let description: String
    let iconPath: String?
    let localizedNames: [String: String]
    let localizedDescriptions: [String: String]

    init(payload: [String: Any]) {
        let metadata = payload["metadata"] as? [String: Any]
        let locales = (metadata?["locales"] as? [String: Any]) ?? (payload["locales"] as? [String: Any]) ?? [:]
        self.name = payload["name"] as? String ?? ""
        self.description = payload["description"] as? String ?? ""
        self.iconPath = (metadata?["icon"] as? String) ?? (payload["icon"] as? String)
        self.localizedNames = Self.extract(field: "name", from: locales)
        self.localizedDescriptions = Self.extract(field: "description", from: locales)
    }

    func localizedName(for language: AppLanguage) -> String {
        resolve(values: localizedNames, language: language, fallback: name)
    }

    func localizedDescription(for language: AppLanguage) -> String {
        resolve(values: localizedDescriptions, language: language, fallback: description)
    }

    private func resolve(values: [String: String], language: AppLanguage, fallback: String) -> String {
        guard values.isEmpty == false else { return fallback }
        let target = language.rawValue.lowercased()
        let languageOnly = target.split(separator: "-").first.map(String.init) ?? target
        return values.first(where: { $0.key.lowercased() == target })?.value
            ?? values.first(where: { $0.key.lowercased() == languageOnly })?.value
            ?? values.first(where: { $0.key.lowercased().hasPrefix("\(languageOnly)-") })?.value
            ?? values["default"]
            ?? fallback
    }

    private static func extract(field: String, from locales: [String: Any]) -> [String: String] {
        locales.compactMapValues { value in
            guard let dict = value as? [String: Any],
                  let text = dict[field] as? String,
                  text.isEmpty == false else {
                return nil
            }
            return text
        }
    }
}

private extension String {
    func ifEmpty(_ fallback: String) -> String {
        isEmpty ? fallback : self
    }
}
