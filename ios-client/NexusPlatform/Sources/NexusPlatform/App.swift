import SwiftUI

@main
struct NexusPlatformApp: App {
    var body: some Scene {
        WindowGroup {
            LaunchExperienceView()
        }
    }
}

struct RootTabView: View {
    var body: some View {
        TabView {
            NavigationStack {
                LibraryView()
            }
            .tabItem {
                Label(copy.library, systemImage: "house.fill")
            }

            NavigationStack {
                DiscoverView()
            }
            .tabItem {
                Label(copy.discover, systemImage: "sparkles")
            }

            NavigationStack {
                RecommendView()
            }
            .tabItem {
                Label(copy.recommend, systemImage: "play.square.stack.fill")
            }

            NavigationStack {
                ProfileView()
            }
            .tabItem {
                Label(copy.profile, systemImage: "person.crop.circle")
            }
        }
        .tint(Color(hex: 0x6B4EFF))
    }

    private var copy: RootTabCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }
}

private struct RootTabCopy {
    let library: String
    let discover: String
    let recommend: String
    let profile: String

    static func forLanguage(_ language: AppLanguage) -> RootTabCopy {
        switch language {
        case .simplifiedChinese:
            return .init(library: "我的库", discover: "发现", recommend: "推荐", profile: "我的")
        case .traditionalChinese:
            return .init(library: "我的庫", discover: "發現", recommend: "推薦", profile: "我的")
        case .english:
            return .init(library: "Library", discover: "Discover", recommend: "Recommend", profile: "Profile")
        }
    }
}
