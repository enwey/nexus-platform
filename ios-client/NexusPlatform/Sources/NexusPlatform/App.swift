import SwiftUI

@main
struct NexusPlatformApp: App {
    init() {
        let navigationAppearance = UINavigationBarAppearance()
        navigationAppearance.configureWithOpaqueBackground()
        navigationAppearance.backgroundColor = UIColor(
            red: 18 / 255,
            green: 18 / 255,
            blue: 18 / 255,
            alpha: 1
        )
        navigationAppearance.shadowColor = UIColor.white.withAlphaComponent(0.04)
        navigationAppearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationAppearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]

        let navigationBar = UINavigationBar.appearance()
        navigationBar.standardAppearance = navigationAppearance
        navigationBar.scrollEdgeAppearance = navigationAppearance
        navigationBar.compactAppearance = navigationAppearance
        navigationBar.compactScrollEdgeAppearance = navigationAppearance
        navigationBar.tintColor = .white
        navigationBar.prefersLargeTitles = false

    }

    var body: some Scene {
        WindowGroup {
            LaunchExperienceView()
                .preferredColorScheme(.dark)
        }
    }
}

struct RootTabView: View {
    enum Tab: Hashable {
        case library
        case discover
        case recommend
        case profile
    }

    @State private var selection: Tab = .library
    @State private var hidesCustomTabBar = false

    var body: some View {
        ZStack {
            tabScene(.library) {
                NavigationStack {
                    LibraryView()
                }
            }

            tabScene(.discover) {
                NavigationStack {
                    DiscoverView()
                }
            }

            tabScene(.recommend) {
                NavigationStack {
                    RecommendView()
                }
            }

            tabScene(.profile) {
                NavigationStack {
                    ProfileView()
                }
            }
        }
        .background(Color(hex: 0x121212).ignoresSafeArea())
        .tint(Color(hex: 0x6B4EFF))
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if hidesCustomTabBar == false {
                NexusFloatingTabBar(selection: $selection, copy: copy)
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .onPreferenceChange(NexusTabBarHiddenPreferenceKey.self) { hidesCustomTabBar = $0 }
        .animation(NativeMotion.overlayTransition, value: hidesCustomTabBar)
    }

    private var copy: RootTabCopy {
        .forLanguage(AppLanguageStore.currentSync())
    }

    @ViewBuilder
    private func tabScene<Content: View>(_ tab: Tab, @ViewBuilder content: () -> Content) -> some View {
        content()
            .opacity(selection == tab ? 1 : 0)
            .allowsHitTesting(selection == tab)
            .accessibilityHidden(selection != tab)
    }
}

private struct NexusFloatingTabBar: View {
    @Binding var selection: RootTabView.Tab
    let copy: RootTabCopy

    var body: some View {
        HStack(spacing: 8) {
            item(.library, title: copy.library, systemImage: "house.fill")
            item(.discover, title: copy.discover, systemImage: "sparkles")
            item(.recommend, title: copy.recommend, systemImage: "play.square.fill")
            item(.profile, title: copy.profile, systemImage: "person.crop.circle.fill")
        }
        .padding(8)
        .background(Color(hex: 0x161616), in: Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.18), radius: 10, y: 2)
    }

    @ViewBuilder
    private func item(_ tab: RootTabView.Tab, title: String, systemImage: String) -> some View {
        let isSelected = selection == tab

        Button {
            selection = tab
        } label: {
            VStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.system(size: 24, weight: .semibold))
                Text(title)
                    .font(.system(size: 11, weight: .semibold))
            }
            .foregroundStyle(isSelected ? Color(hex: 0x7C5CFF) : .white)
            .frame(maxWidth: .infinity)
            .frame(height: 72)
            .background(
                Capsule()
                    .fill(isSelected ? Color.white.opacity(0.12) : .clear)
            )
        }
        .buttonStyle(.plain)
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

private struct NexusTabBarHiddenPreferenceKey: PreferenceKey {
    static var defaultValue = false

    static func reduce(value: inout Bool, nextValue: () -> Bool) {
        value = value || nextValue()
    }
}

extension View {
    func nexusTabBarHidden(_ hidden: Bool = true) -> some View {
        preference(key: NexusTabBarHiddenPreferenceKey.self, value: hidden)
    }
}

enum NativeMotion {
    static let skeletonCycle: TimeInterval = 1.45
    static let skeletonAngle: Double = 18
    static let skeletonBandWidthRatio: CGFloat = 0.34
    static let skeletonBandMinWidth: CGFloat = 24
    static let skeletonBandHeightMultiplier: CGFloat = 2.4
    static let skeletonBandVerticalOffsetMultiplier: CGFloat = 0.7
    static let skeletonBlurRadius: CGFloat = 8
    static let toastPresent = Animation.interpolatingSpring(duration: 0.34, bounce: 0.05)
    static let toastDismiss = Animation.easeInOut(duration: 0.24)
    static let press = Animation.easeOut(duration: 0.12)
    static let overlayTransition = Animation.easeInOut(duration: 0.24)
    static let stateSwapTransition: AnyTransition = .opacity.combined(with: .scale(scale: 0.985, anchor: .top))
    static let contentRevealTransition: AnyTransition = .opacity.combined(with: .scale(scale: 0.992, anchor: .center))
    static let skeletonBaseOpacity = 0.06
    static let skeletonGlowOpacity = 0.12
    static let pillBackgroundOpacity = 0.72
    static let sectionOverlayOpacity = 0.16
}

struct NativeToastOverlay: View {
    @Binding var message: String?
    var bottomPadding: CGFloat = 26

    @State private var renderedMessage: String?
    @State private var isPresented = false
    @State private var dismissTask: Task<Void, Never>?

    var body: some View {
        ZStack(alignment: .bottom) {
            if let renderedMessage {
                Text(renderedMessage)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.82), in: Capsule())
                    .padding(.bottom, bottomPadding)
                    .opacity(isPresented ? 1 : 0)
                    .offset(y: isPresented ? 0 : 18)
                    .scaleEffect(isPresented ? 1 : 0.98, anchor: .bottom)
                    .animation(NativeMotion.toastPresent, value: isPresented)
            }
        }
        .allowsHitTesting(false)
        .onAppear {
            showIfNeeded(message)
        }
        .onChange(of: message) {
            showIfNeeded(message)
        }
        .onDisappear {
            dismissTask?.cancel()
            dismissTask = nil
        }
    }

    @MainActor
    private func showIfNeeded(_ nextMessage: String?) {
        dismissTask?.cancel()
        dismissTask = nil

        guard let nextMessage, nextMessage.isEmpty == false else {
            hideRenderedMessage()
            return
        }

        renderedMessage = nextMessage
        withAnimation(NativeMotion.toastPresent) {
            isPresented = true
        }

        dismissTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard message == nextMessage else { return }
            hideRenderedMessage()
        }
    }

    @MainActor
    private func hideRenderedMessage() {
        withAnimation(NativeMotion.toastDismiss) {
            isPresented = false
        }

        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 280_000_000)
            if isPresented == false {
                renderedMessage = nil
                message = nil
            }
        }
    }
}

struct NativeNavigationButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.72 : 1)
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(NativeMotion.press, value: configuration.isPressed)
    }
}

struct NativeStateCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 10) {
            content
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(Color.white.opacity(0.04), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }
}

struct NativeSkeletonBlock: View {
    var width: CGFloat? = nil
    var height: CGFloat
    var cornerRadius: CGFloat

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)

        shape
            .fill(Color.white.opacity(NativeMotion.skeletonBaseOpacity))
            .frame(width: width, height: height)
            .overlay {
                TimelineView(.animation) { context in
                    GeometryReader { proxy in
                        let size = proxy.size
                        let seconds = context.date.timeIntervalSinceReferenceDate
                        let progress = seconds.truncatingRemainder(dividingBy: NativeMotion.skeletonCycle) / NativeMotion.skeletonCycle
                        let referenceWidth = max(size.width, size.height)
                        let bandWidth = max(referenceWidth * NativeMotion.skeletonBandWidthRatio, NativeMotion.skeletonBandMinWidth)
                        let travel = size.width + bandWidth * 2
                        let offset = (-bandWidth) + (travel * progress)

                        Rectangle()
                            .fill(
                                LinearGradient(
                                    colors: [
                                        .clear,
                                        Color.white.opacity(NativeMotion.skeletonGlowOpacity * 0.45),
                                        Color.white.opacity(NativeMotion.skeletonGlowOpacity),
                                        Color.white.opacity(NativeMotion.skeletonGlowOpacity * 0.45),
                                        .clear
                                    ],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: bandWidth, height: max(size.width, size.height) * NativeMotion.skeletonBandHeightMultiplier)
                            .rotationEffect(.degrees(NativeMotion.skeletonAngle))
                            .blur(radius: NativeMotion.skeletonBlurRadius)
                            .offset(
                                x: offset - bandWidth,
                                y: -max(size.width, size.height) * NativeMotion.skeletonBandVerticalOffsetMultiplier
                            )
                    }
                }
                .mask(shape)
            }
    }

    init(width: CGFloat? = nil, height: CGFloat, cornerRadius: CGFloat = 16) {
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }
}

struct NativeSkeletonText: View {
    let widths: [CGFloat]
    var lineHeight: CGFloat = 12
    var spacing: CGFloat = 8
    var cornerRadius: CGFloat = 6

    var body: some View {
        VStack(alignment: .leading, spacing: spacing) {
            ForEach(Array(widths.enumerated()), id: \.offset) { _, width in
                NativeSkeletonBlock(width: width, height: lineHeight, cornerRadius: cornerRadius)
            }
        }
    }
}

struct NativeSkeletonIcon: View {
    var size: CGFloat
    var cornerRadius: CGFloat

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(Color(hex: 0x232326))
            .overlay {
                NativeSkeletonBlock(height: size, cornerRadius: cornerRadius)
            }
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
            )
    }
}

struct NativeRefreshPill: View {
    let text: String

    var body: some View {
        HStack(spacing: 8) {
            ProgressView()
                .tint(Color.white)
                .scaleEffect(0.82)
            Text(text)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.black.opacity(NativeMotion.pillBackgroundOpacity), in: Capsule())
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}

struct NativeSectionRefreshOverlay: View {
    var lineWidths: [CGFloat] = [92, 64]
    var cornerRadius: CGFloat = 20

    var body: some View {
        ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .fill(Color.black.opacity(NativeMotion.sectionOverlayOpacity))

            VStack(alignment: .trailing, spacing: 8) {
                NativeRefreshPill(text: AppLanguageStore.currentSync() == .english ? "Refreshing" : "正在刷新")
                VStack(alignment: .trailing, spacing: 8) {
                    ForEach(Array(lineWidths.enumerated()), id: \.offset) { _, width in
                        NativeSkeletonBlock(width: width, height: 10, cornerRadius: 5)
                    }
                }
            }
            .padding(14)
        }
        .allowsHitTesting(false)
        .transition(.opacity.combined(with: .scale(scale: 0.985, anchor: .topTrailing)))
    }
}
