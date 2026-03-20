import SwiftUI

@main
struct NexusPlatformApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        NavigationView {
            GameListView()
        }
    }
}
