import SwiftUI

struct AuthGateLaunchLink<Label: View>: View {
    let game: Game
    @ViewBuilder let label: () -> Label

    @State private var shouldNavigate = false
    @State private var showAuthFlow = false
    @State private var isChecking = false

    var body: some View {
        Button {
            Task { await handleTap() }
        } label: {
            label()
        }
        .disabled(isChecking)
        .background(
            NavigationLink(
                destination: GameView(game: game),
                isActive: $shouldNavigate,
                label: { EmptyView() }
            )
            .hidden()
        )
        .sheet(isPresented: $showAuthFlow) {
            NavigationStack {
                AuthFlowView { session in
                    Task {
                        await AuthSessionStore.shared.save(session)
                        await MainActor.run {
                            showAuthFlow = false
                            shouldNavigate = true
                        }
                    }
                }
            }
        }
    }

    private func handleTap() async {
        guard isChecking == false else { return }
        isChecking = true
        defer { isChecking = false }

        let session = await AuthSessionStore.shared.current()
        if session == nil {
            await MainActor.run {
                showAuthFlow = true
            }
            return
        }
        await MainActor.run {
            shouldNavigate = true
        }
    }
}
