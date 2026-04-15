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
                Label("我的库", systemImage: "house.fill")
            }

            NavigationStack {
                DiscoverView()
            }
            .tabItem {
                Label("发现", systemImage: "sparkles")
            }

            NavigationStack {
                RecommendView()
            }
            .tabItem {
                Label("推荐", systemImage: "play.square.stack.fill")
            }

            NavigationStack {
                ProfileView()
            }
            .tabItem {
                Label("我的", systemImage: "person.crop.circle")
            }
        }
    }
}
