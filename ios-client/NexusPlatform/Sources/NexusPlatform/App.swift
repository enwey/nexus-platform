import SwiftUI

@main
struct NexusPlatformApp: App {
    var body: some Scene {
        WindowGroup {
            RootTabView()
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
                ProfileView()
            }
            .tabItem {
                Label("我的", systemImage: "person.crop.circle")
            }
        }
    }
}
