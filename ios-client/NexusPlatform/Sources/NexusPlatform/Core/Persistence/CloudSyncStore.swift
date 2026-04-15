import Foundation

actor CloudSyncStore {
    static let shared = CloudSyncStore()

    private let defaults = UserDefaults.standard
    private let key = "nexus.cloud.sync.enabled"

    func isEnabled() -> Bool {
        defaults.object(forKey: key) as? Bool ?? false
    }

    func setEnabled(_ enabled: Bool) {
        defaults.set(enabled, forKey: key)
    }
}
