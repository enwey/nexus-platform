import Foundation

struct Game: Identifiable, Codable, Hashable, Sendable {
    let id: String
    let name: String
    let description: String
    let iconUrl: String
    let downloadUrl: String
    let version: String
    let md5: String
    let category: String?

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
    }

    init(
        id: String,
        name: String,
        description: String,
        iconUrl: String,
        downloadUrl: String,
        version: String,
        md5: String,
        category: String? = nil
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.iconUrl = iconUrl
        self.downloadUrl = downloadUrl
        self.version = version
        self.md5 = md5
        self.category = category
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
    }
}
