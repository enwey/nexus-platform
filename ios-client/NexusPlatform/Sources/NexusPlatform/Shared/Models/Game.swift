import Foundation

struct Game: Identifiable, Decodable, Hashable, Sendable {
    let id: String
    let name: String
    let description: String
    let iconUrl: String
    let downloadUrl: String
    let version: String
    let md5: String
    let category: String?
    let localizedNames: [String: String]
    let localizedDescriptions: [String: String]

    enum CodingKeys: String, CodingKey {
        case id
        case appId
        case name
        case description
        case iconUrl
        case downloadUrl
        case version
        case md5
        case category
        case locales
        case metadata
    }

    struct LocalizedEntry: Codable, Hashable, Sendable {
        let name: String?
        let description: String?
    }

    struct Metadata: Codable, Hashable, Sendable {
        let locales: [String: LocalizedEntry]?
    }

    init(
        id: String,
        name: String,
        description: String,
        iconUrl: String,
        downloadUrl: String,
        version: String,
        md5: String,
        category: String? = nil,
        localizedNames: [String: String] = [:],
        localizedDescriptions: [String: String] = [:]
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.iconUrl = iconUrl
        self.downloadUrl = downloadUrl
        self.version = version
        self.md5 = md5
        self.category = category
        self.localizedNames = localizedNames
        self.localizedDescriptions = localizedDescriptions
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        let appID = try container.decodeIfPresent(String.self, forKey: .appId)
        let fallbackID = (try? container.decode(Int.self, forKey: .id)).map { String($0) }
        self.id = appID ?? fallbackID ?? UUID().uuidString

        self.name = try container.decodeIfPresent(String.self, forKey: .name) ?? "Untitled"
        self.description = try container.decodeIfPresent(String.self, forKey: .description) ?? ""
        self.iconUrl = try container.decodeIfPresent(String.self, forKey: .iconUrl) ?? ""
        self.downloadUrl = try container.decodeIfPresent(String.self, forKey: .downloadUrl) ?? ""
        self.version = try container.decodeIfPresent(String.self, forKey: .version) ?? "0.0.0"
        self.md5 = try container.decodeIfPresent(String.self, forKey: .md5) ?? ""
        self.category = try container.decodeIfPresent(String.self, forKey: .category)
        let topLevelLocales = try container.decodeIfPresent([String: LocalizedEntry].self, forKey: .locales) ?? [:]
        let nestedLocales = try container.decodeIfPresent(Metadata.self, forKey: .metadata)?.locales ?? [:]
        let mergedLocales = topLevelLocales.merging(nestedLocales) { current, _ in current }
        self.localizedNames = mergedLocales.compactMapValues(\.name)
        self.localizedDescriptions = mergedLocales.compactMapValues(\.description)
    }

    func localizedName(for language: AppLanguage) -> String {
        resolveLocalizedValue(values: localizedNames, language: language, fallback: name)
    }

    func localizedDescription(for language: AppLanguage) -> String {
        resolveLocalizedValue(values: localizedDescriptions, language: language, fallback: description)
    }

    func applyingPresentation(
        name: String? = nil,
        description: String? = nil,
        iconUrl: String? = nil,
        localizedNames: [String: String]? = nil,
        localizedDescriptions: [String: String]? = nil
    ) -> Game {
        Game(
            id: id,
            name: name ?? self.name,
            description: description ?? self.description,
            iconUrl: iconUrl ?? self.iconUrl,
            downloadUrl: downloadUrl,
            version: version,
            md5: md5,
            category: category,
            localizedNames: localizedNames ?? self.localizedNames,
            localizedDescriptions: localizedDescriptions ?? self.localizedDescriptions
        )
    }

    private func resolveLocalizedValue(values: [String: String], language: AppLanguage, fallback: String) -> String {
        guard values.isEmpty == false else { return fallback }
        let target = language.rawValue.lowercased()
        let languageOnly = target.split(separator: "-").first.map(String.init) ?? target
        return values.first(where: { $0.key.lowercased() == target })?.value
            ?? values.first(where: { $0.key.lowercased() == languageOnly })?.value
            ?? values.first(where: { $0.key.lowercased().hasPrefix("\(languageOnly)-") })?.value
            ?? values["default"]
            ?? fallback
    }
}
