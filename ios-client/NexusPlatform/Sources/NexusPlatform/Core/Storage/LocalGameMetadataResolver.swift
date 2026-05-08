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
              let manifest = try? JSONDecoder().decode(HostedMiniAppManifest.self, from: data) else {
            return localizedRemote
        }

        let mergedLocalizedNames = localizedRemote.localizedNames.merging(extract(field: \.name, from: manifest.resolvedLocales)) { _, new in new }
        let mergedLocalizedDescriptions = localizedRemote.localizedDescriptions.merging(extract(field: \.description, from: manifest.resolvedLocales)) { _, new in new }
        let iconURL: String? = {
            guard let iconPath = manifest.resolvedIconPath?.trimmingCharacters(in: .whitespacesAndNewlines),
                  iconPath.isEmpty == false else {
                return localizedRemote.iconUrl
            }
            let fileURL = local.rootDirectory.appendingPathComponent(iconPath)
            return FileManager.default.fileExists(atPath: fileURL.path) ? fileURL.absoluteString : localizedRemote.iconUrl
        }()

        let localizedName = localizedValue(
            values: extract(field: \.name, from: manifest.resolvedLocales),
            language: language,
            fallback: manifest.name ?? ""
        )
        let localizedDescription = localizedValue(
            values: extract(field: \.description, from: manifest.resolvedLocales),
            language: language,
            fallback: manifest.description ?? ""
        )

        return localizedRemote.applyingPresentation(
            name: localizedName.ifEmpty(localizedRemote.name),
            description: localizedDescription.ifEmpty(localizedRemote.description),
            iconUrl: iconURL,
            localizedNames: mergedLocalizedNames,
            localizedDescriptions: mergedLocalizedDescriptions
        )
    }

    private func localizedValue(values: [String: String], language: AppLanguage, fallback: String) -> String {
        guard values.isEmpty == false else { return fallback }
        let target = language.rawValue.lowercased()
        let languageOnly = target.split(separator: "-").first.map(String.init) ?? target
        return values.first(where: { $0.key.lowercased() == target })?.value
            ?? values.first(where: { $0.key.lowercased() == languageOnly })?.value
            ?? values.first(where: { $0.key.lowercased().hasPrefix("\(languageOnly)-") })?.value
            ?? values["default"]
            ?? fallback
    }

    private func extract(
        field: KeyPath<HostedMiniAppManifestLocale, String?>,
        from locales: [String: HostedMiniAppManifestLocale]
    ) -> [String: String] {
        locales.compactMapValues { locale in
            guard let text = locale[keyPath: field]?.trimmingCharacters(in: .whitespacesAndNewlines),
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
