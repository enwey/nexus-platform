import SwiftUI
import UIKit

struct LaunchExperienceView: View {
    private enum Stage {
        case onboarding
        case app
    }

    @State private var stage: Stage?
    @State private var hasBootstrapped = false
    @State private var language: AppLanguage = .simplifiedChinese
    private let launchStore = AppLaunchStore.shared

    var body: some View {
        Group {
            switch stage {
            case .onboarding:
                OnboardingView(copy: .forLanguage(language)) {
                    Task {
                        await launchStore.markOnboardingCompleted()
                        await MainActor.run {
                            stage = .app
                        }
                    }
                }
            case .app:
                RootTabView()
            case nil:
                LaunchBootstrapView()
            }
        }
        .task {
            guard hasBootstrapped == false else { return }
            hasBootstrapped = true
            let currentLanguage = await AppLanguageStore.shared.current()
            await MainActor.run {
                language = currentLanguage
            }
            let completed = await launchStore.hasCompletedOnboarding()
            await MainActor.run {
                stage = completed ? .app : .onboarding
            }
        }
    }
}

private struct LaunchBootstrapView: View {
    var body: some View {
        ZStack {
            Color(hex: 0x121212)
                .ignoresSafeArea()

            BrandLogoImage(size: 126, cornerRadius: 28)
        }
    }
}

private struct OnboardingView: View {
    let copy: LaunchCopy
    let onFinish: () -> Void

    @State private var pageIndex = 0

    var body: some View {
        GeometryReader { proxy in
            let heroHeight = proxy.size.height > 760 ? min(proxy.size.height * 0.61, 520) : proxy.size.height * 0.58
            let bottomPadding = proxy.size.height > 760 ? proxy.safeAreaInsets.bottom + 50 : proxy.safeAreaInsets.bottom + 28

            ZStack(alignment: .topTrailing) {
                Color(hex: 0x121212)
                    .ignoresSafeArea()

                if pageIndex < copy.pages.count - 1 {
                    Button(copy.skip, action: onFinish)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Color(hex: 0xA0A0A0))
                        .padding(.top, proxy.safeAreaInsets.top + 12)
                        .padding(.trailing, 30)
                }

                VStack(spacing: 0) {
                    ZStack {
                        ForEach(Array(copy.pages.enumerated()), id: \.element.id) { index, page in
                            if index == pageIndex {
                                HeroImageView(
                                    imageURL: page.imageURL,
                                    imageAlignment: page.imageAlignment
                                )
                                .frame(height: heroHeight)
                                .transition(.opacity)
                            }
                        }
                    }
                    .frame(height: heroHeight)
                    .animation(.easeInOut(duration: 0.28), value: pageIndex)

                    let current = copy.pages[pageIndex]

                    VStack(alignment: .leading, spacing: 0) {
                        Spacer()
                            .frame(height: 8)

                        title(for: current)
                            .font(.system(size: 40, weight: .black))
                            .lineSpacing(8)
                            .fixedSize(horizontal: false, vertical: true)

                        Text(current.description)
                            .font(.system(size: 16))
                            .foregroundColor(Color(hex: 0xA0A0A0))
                            .lineSpacing(10)
                            .padding(.top, 16)

                        HStack(spacing: 8) {
                            ForEach(Array(copy.pages.enumerated()), id: \.element.id) { index, page in
                                Capsule()
                                    .fill(index == pageIndex ? page.highlightColor : Color(hex: 0x2C2C2C))
                                    .frame(width: index == pageIndex ? 32 : 16, height: 4)
                            }
                        }
                        .padding(.top, 16)

                        Spacer()

                        Button(action: primaryAction) {
                            Text(pageIndex == copy.pages.count - 1 ? copy.start : copy.next)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(current.buttonTextColor)
                                .frame(maxWidth: .infinity)
                                .frame(height: 60)
                                .background(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .fill(current.buttonColor)
                                )
                        }
                        .buttonStyle(ScaleButtonStyle())
                    }
                    .padding(.horizontal, 32)
                    .padding(.bottom, bottomPadding)
                    .offset(y: -52)
                }
            }
        }
    }

    private func title(for page: LaunchPageCopy) -> Text {
        if page.titleHighlight.isEmpty {
            return Text(page.titlePrefix)
                .foregroundColor(.white)
        }

        return Text(page.titlePrefix + "\n")
            .foregroundColor(.white)
        + Text(page.titleHighlight)
            .foregroundColor(page.highlightColor)
    }

    private func primaryAction() {
        if pageIndex == copy.pages.count - 1 {
            onFinish()
        } else {
            withAnimation(.easeInOut(duration: 0.28)) {
                pageIndex += 1
            }
        }
    }
}

private struct LaunchCopy {
    let skip: String
    let next: String
    let start: String
    let pages: [LaunchPageCopy]

    static func forLanguage(_ language: AppLanguage) -> LaunchCopy {
        switch language {
        case .simplifiedChinese:
            return LaunchCopy(
                skip: "跳过",
                next: "下一步",
                start: "开启体验",
                pages: [
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .center,
                        titlePrefix: "打败你的所有无聊",
                        titleHighlight: "",
                        description: "为你量身定制的次世代游戏宇宙，海量精品大作，随时随地拯救不开心。",
                        highlightColor: Color(hex: 0x6B4EFF),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1638803040283-7a5ffa48bf0d?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .top,
                        titlePrefix: "无需下载，",
                        titleHighlight: "秒开即玩。",
                        description: "告别漫长的安装包等待。自研底层引擎突破物理限制，享受满帧 60FPS 的极致流畅。",
                        highlightColor: Color(hex: 0x00E676),
                        buttonColor: Color(hex: 0x00E676),
                        buttonTextColor: .black
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1614729939124-032f0b56c9ce?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .center,
                        titlePrefix: "海量大作，",
                        titleHighlight: "装进口袋。",
                        description: "无论是手机、PC 还是平板，您的游戏资产、成就与进度实时云端同步，永不丢失。",
                        highlightColor: Color(hex: 0x6B4EFF),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    )
                ]
            )
        case .traditionalChinese:
            return LaunchCopy(
                skip: "跳過",
                next: "下一步",
                start: "開啟體驗",
                pages: [
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .center,
                        titlePrefix: "打敗你的所有無聊",
                        titleHighlight: "",
                        description: "為你量身定製的次世代遊戲宇宙，海量精品大作，隨時隨地拯救不開心。",
                        highlightColor: Color(hex: 0x6B4EFF),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1638803040283-7a5ffa48bf0d?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .top,
                        titlePrefix: "無需下載，",
                        titleHighlight: "秒開即玩。",
                        description: "告別漫長的安裝包等待。自研底層引擎突破物理限制，享受滿幀 60FPS 的極致流暢。",
                        highlightColor: Color(hex: 0x00E676),
                        buttonColor: Color(hex: 0x00E676),
                        buttonTextColor: .black
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1614729939124-032f0b56c9ce?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .center,
                        titlePrefix: "海量大作，",
                        titleHighlight: "裝進口袋。",
                        description: "無論是手機、PC 還是平板，你的遊戲資產、成就與進度實時雲端同步，永不丟失。",
                        highlightColor: Color(hex: 0x6B4EFF),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    )
                ]
            )
        case .english:
            return LaunchCopy(
                skip: "Skip",
                next: "Next",
                start: "Start Exploring",
                pages: [
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .center,
                        titlePrefix: "Beat All Your Boredom",
                        titleHighlight: "",
                        description: "A next-gen game universe tailored for you, packed with premium titles ready to lift every dull moment.",
                        highlightColor: Color(hex: 0x6B4EFF),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1638803040283-7a5ffa48bf0d?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .top,
                        titlePrefix: "No Downloads,",
                        titleHighlight: "Instant Play.",
                        description: "Skip the long install wait. Our in-house engine breaks device limits for a silky 60 FPS instant-play experience.",
                        highlightColor: Color(hex: 0x00E676),
                        buttonColor: Color(hex: 0x00E676),
                        buttonTextColor: .black
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1614729939124-032f0b56c9ce?auto=format&fit=crop&w=800&q=80"),
                        imageAlignment: .center,
                        titlePrefix: "Big Games,",
                        titleHighlight: "In Your Pocket.",
                        description: "Across phone, PC, and tablet, your games, achievements, and progress stay synced in the cloud.",
                        highlightColor: Color(hex: 0x6B4EFF),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    )
                ]
            )
        }
    }
}

private struct LaunchPageCopy: Identifiable {
    let id = UUID()
    let imageURL: URL?
    let imageAlignment: Alignment
    let titlePrefix: String
    let titleHighlight: String
    let description: String
    let highlightColor: Color
    let buttonColor: Color
    let buttonTextColor: Color
}

private struct HeroImageView: View {
    let imageURL: URL?
    let imageAlignment: Alignment

    var body: some View {
        ZStack {
            RemoteImageView(url: imageURL, alignment: imageAlignment)
                .background(Color(hex: 0x121212))

            LinearGradient(
                gradient: Gradient(stops: [
                    .init(color: Color(hex: 0x121212).opacity(0), location: 0.4),
                    .init(color: Color(hex: 0x121212), location: 1)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .clipped()
    }
}

private struct RemoteImageView: View {
    let url: URL?
    let alignment: Alignment

    @StateObject private var loader = RemoteImageLoader()

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color(hex: 0x121212)

                if let image = loader.image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: proxy.size.width, height: proxy.size.height, alignment: alignment)
                        .clipped()
                }
            }
            .onAppear {
                loader.load(from: url)
            }
        }
    }
}

private final class RemoteImageLoader: ObservableObject {
    @Published var image: UIImage?

    private static let cache = NSCache<NSURL, UIImage>()
    private var task: URLSessionDataTask?
    private var loadedURL: URL?

    deinit {
        task?.cancel()
    }

    func load(from url: URL?) {
        guard let url else { return }
        guard loadedURL != url else { return }
        loadedURL = url

        if let cached = Self.cache.object(forKey: url as NSURL) {
            image = cached
            return
        }

        task?.cancel()
        task = URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            guard
                let self,
                let data,
                let image = UIImage(data: data)
            else { return }

            Self.cache.setObject(image, forKey: url as NSURL)
            DispatchQueue.main.async {
                self.image = image
            }
        }
        task?.resume()
    }
}

private struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .opacity(configuration.isPressed ? 0.92 : 1)
    }
}
