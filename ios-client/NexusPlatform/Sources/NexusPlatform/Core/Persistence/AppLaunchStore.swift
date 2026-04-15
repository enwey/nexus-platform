import Foundation

actor AppLaunchStore {
    static let shared = AppLaunchStore()

    private let defaults = UserDefaults.standard
    private let onboardingKey = "nexus.launch.onboarding.completed"

    func hasCompletedOnboarding() -> Bool {
        defaults.bool(forKey: onboardingKey)
    }

    func markOnboardingCompleted() {
        defaults.set(true, forKey: onboardingKey)
    }
}
