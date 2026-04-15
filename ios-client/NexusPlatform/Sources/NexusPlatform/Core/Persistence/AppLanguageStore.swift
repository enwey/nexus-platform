import Foundation

enum AppLanguage: String, CaseIterable, Identifiable, Sendable {
    case simplifiedChinese = "zh-CN"
    case traditionalChinese = "zh-TW"
    case english = "en"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .simplifiedChinese: return "简体中文"
        case .traditionalChinese: return "繁體中文"
        case .english: return "English"
        }
    }
}

actor AppLanguageStore {
    static let shared = AppLanguageStore()

    private let defaults = UserDefaults.standard
    private let key = "nexus.app.language"

    func current() -> AppLanguage {
        guard let raw = defaults.string(forKey: key),
              let language = AppLanguage(rawValue: raw) else {
            return .simplifiedChinese
        }
        return language
    }

    func set(_ language: AppLanguage) {
        defaults.set(language.rawValue, forKey: key)
    }
}
