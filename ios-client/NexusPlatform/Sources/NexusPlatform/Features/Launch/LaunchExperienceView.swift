import SafariServices
import SwiftUI
import UIKit

struct LaunchExperienceView: View {
    private enum Stage {
        case onboarding
        case app
    }

    @State private var stage: Stage?
    @State private var hasBootstrapped = false
    @State private var language: AppLanguage = AppLanguageStore.currentSync()
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
                Color(hex: 0x090A0F)
                    .ignoresSafeArea()
            }
        }
        .task {
            guard hasBootstrapped == false else { return }
            hasBootstrapped = true
            let currentLanguage = await AppLanguageStore.shared.current()
            let completed = await launchStore.hasCompletedOnboarding()
            await MainActor.run {
                language = currentLanguage
                stage = completed ? .app : .onboarding
            }
            await LaunchAdService.shared.refreshCacheIfNeeded()
        }
    }
}

private struct LaunchAdView: View {
    let copy: LaunchAdCopy
    let onSkip: () -> Void
    let onOpenLanding: () -> Void

    @State private var remainingSeconds: Int
    @State private var isPresentingLanding = false
    @State private var hasCompleted = false

    init(copy: LaunchAdCopy, onSkip: @escaping () -> Void, onOpenLanding: @escaping () -> Void) {
        self.copy = copy
        self.onSkip = onSkip
        self.onOpenLanding = onOpenLanding
        _remainingSeconds = State(initialValue: max(1, copy.displaySeconds))
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .topTrailing) {
                LaunchAdImageView(copy: copy)
                    .ignoresSafeArea()

                LinearGradient(
                    gradient: Gradient(stops: [
                        .init(color: Color.black.opacity(0.18), location: 0),
                        .init(color: Color.black.opacity(0.28), location: 0.25),
                        .init(color: Color(hex: 0x090A0F).opacity(0.95), location: 0.88),
                        .init(color: Color(hex: 0x090A0F), location: 1)
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                Button(action: skip) {
                    Text(copy.skipText(remainingSeconds))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: 0xC7CBD8))
                        .padding(.horizontal, 14)
                        .frame(height: 36)
                        .background(Color.black.opacity(0.28), in: Capsule())
                        .overlay(
                            Capsule()
                                .stroke(Color.white.opacity(0.12), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .padding(.top, proxy.safeAreaInsets.top + 14)
                .padding(.trailing, 20)

                VStack(alignment: .leading, spacing: 0) {
                    Spacer()

                    Button(action: openLanding) {
                        VStack(alignment: .leading, spacing: 0) {
                            HStack(spacing: 8) {
                                Text(copy.badge)
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 10)
                                    .frame(height: 28)
                                    .background(Color(hex: 0x5C67FF), in: Capsule())

                                Text(copy.sponsor)
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(Color.white.opacity(0.78))
                                    .lineLimit(1)
                            }

                            HStack(alignment: .center, spacing: 16) {
                                BrandLogoImage(size: 68, cornerRadius: 20)

                                VStack(alignment: .leading, spacing: 0) {
                                    Text(copy.title)
                                        .font(.system(size: 30, weight: .black))
                                        .foregroundStyle(.white)
                                        .fixedSize(horizontal: false, vertical: true)

                                    Text(copy.description)
                                        .font(.system(size: 15))
                                        .foregroundStyle(Color(hex: 0xC7CBD8))
                                        .lineSpacing(6)
                                        .padding(.top, 12)
                                }
                            }
                            .padding(.top, 18)

                            HStack(spacing: 10) {
                                Text(copy.cta)
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 22)
                                    .frame(height: 50)
                                    .background(
                                        LinearGradient(
                                            colors: [Color(hex: 0x5C67FF), Color(hex: 0x7E59FF)],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        ),
                                        in: Capsule()
                                    )

                                Text(copy.footer)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundStyle(Color(hex: 0xA8ADBF))
                                    .lineLimit(1)
                            }
                            .padding(.top, 24)
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 24)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.black.opacity(0.34), in: RoundedRectangle(cornerRadius: 28, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 28, style: .continuous)
                                .stroke(Color.white.opacity(0.10), lineWidth: 1)
                        )
                    }
                    .buttonStyle(ScaleButtonStyle())
                    .padding(.horizontal, 20)
                    .padding(.bottom, max(proxy.safeAreaInsets.bottom, 22) + 18)
                }
            }
        }
        .task {
            while remainingSeconds > 0 && hasCompleted == false {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard isPresentingLanding == false else { continue }
                guard hasCompleted == false else { return }
                remainingSeconds -= 1
            }

            if hasCompleted == false && isPresentingLanding == false {
                skip()
            }
        }
    }

    private func skip() {
        guard hasCompleted == false else { return }
        hasCompleted = true
        onSkip()
    }

    private func openLanding() {
        guard isPresentingLanding == false, hasCompleted == false else { return }
        isPresentingLanding = true
        onOpenLanding()
    }
}

private struct LaunchAdLandingView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.dismissButtonStyle = .close
        controller.preferredControlTintColor = .white
        controller.preferredBarTintColor = UIColor(Color(hex: 0x090A0F))
        return controller
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

struct LaunchAdCopy {
    let skipPrefix: String
    let badge: String
    let sponsor: String
    let title: String
    let description: String
    let cta: String
    let footer: String
    let displaySeconds: Int
    let localImagePath: String?
    let imageURL: URL?
    let landingURL: URL

    func skipText(_ seconds: Int) -> String {
        "\(skipPrefix) \(seconds)"
    }

    static func fallback(for language: AppLanguage) -> LaunchAdCopy {
        let imageURL = URL(string: "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80")
        let landingURL = URL(string: "https://bringbox.com/download")!
        switch language {
        case .simplifiedChinese:
            return LaunchAdCopy(
                skipPrefix: "跳过",
                badge: "广告",
                sponsor: "BringBox 官方活动",
                title: "新用户限时福利",
                description: "登录 BringBox，领取限时试玩权益与精选礼包，热门游戏开局更轻松。",
                cta: "立即查看",
                footer: "点击查看活动详情",
                displaySeconds: 4,
                localImagePath: nil,
                imageURL: imageURL,
                landingURL: landingURL
            )
        case .traditionalChinese:
            return LaunchAdCopy(
                skipPrefix: "跳過",
                badge: "廣告",
                sponsor: "BringBox 官方活動",
                title: "新用戶限時福利",
                description: "登入 BringBox，領取限時試玩權益與精選禮包，熱門遊戲開局更輕鬆。",
                cta: "立即查看",
                footer: "點擊查看活動詳情",
                displaySeconds: 4,
                localImagePath: nil,
                imageURL: imageURL,
                landingURL: landingURL
            )
        case .english:
            return LaunchAdCopy(
                skipPrefix: "Skip",
                badge: "Ad",
                sponsor: "BringBox Official Campaign",
                title: "New Player Rewards",
                description: "Sign in to BringBox for limited-time trial perks and curated bundles for trending games.",
                cta: "View Now",
                footer: "Tap to view campaign details",
                displaySeconds: 4,
                localImagePath: nil,
                imageURL: imageURL,
                landingURL: landingURL
            )
        }
    }

    static func fromCached(_ payload: LaunchAdCachedPayload, language: AppLanguage) -> LaunchAdCopy {
        let fallback = fallback(for: language)
        switch language {
        case .simplifiedChinese:
            return LaunchAdCopy(
                skipPrefix: fallback.skipPrefix,
                badge: fallback.badge,
                sponsor: payload.sponsorZhCn.isEmpty ? fallback.sponsor : payload.sponsorZhCn,
                title: payload.titleZhCn.isEmpty ? fallback.title : payload.titleZhCn,
                description: payload.descriptionZhCn.isEmpty ? fallback.description : payload.descriptionZhCn,
                cta: payload.ctaZhCn.isEmpty ? fallback.cta : payload.ctaZhCn,
                footer: payload.footerZhCn.isEmpty ? fallback.footer : payload.footerZhCn,
                displaySeconds: max(1, payload.displaySeconds),
                localImagePath: payload.localImagePath,
                imageURL: nil,
                landingURL: URL(string: payload.targetUrl) ?? fallback.landingURL
            )
        case .traditionalChinese:
            return LaunchAdCopy(
                skipPrefix: fallback.skipPrefix,
                badge: fallback.badge,
                sponsor: payload.sponsorZhTw.isEmpty ? fallback.sponsor : payload.sponsorZhTw,
                title: payload.titleZhTw.isEmpty ? fallback.title : payload.titleZhTw,
                description: payload.descriptionZhTw.isEmpty ? fallback.description : payload.descriptionZhTw,
                cta: payload.ctaZhTw.isEmpty ? fallback.cta : payload.ctaZhTw,
                footer: payload.footerZhTw.isEmpty ? fallback.footer : payload.footerZhTw,
                displaySeconds: max(1, payload.displaySeconds),
                localImagePath: payload.localImagePath,
                imageURL: nil,
                landingURL: URL(string: payload.targetUrl) ?? fallback.landingURL
            )
        case .english:
            return LaunchAdCopy(
                skipPrefix: fallback.skipPrefix,
                badge: fallback.badge,
                sponsor: payload.sponsorEn.isEmpty ? fallback.sponsor : payload.sponsorEn,
                title: payload.titleEn.isEmpty ? fallback.title : payload.titleEn,
                description: payload.descriptionEn.isEmpty ? fallback.description : payload.descriptionEn,
                cta: payload.ctaEn.isEmpty ? fallback.cta : payload.ctaEn,
                footer: payload.footerEn.isEmpty ? fallback.footer : payload.footerEn,
                displaySeconds: max(1, payload.displaySeconds),
                localImagePath: payload.localImagePath,
                imageURL: nil,
                landingURL: URL(string: payload.targetUrl) ?? fallback.landingURL
            )
        }
    }
}

private struct LaunchAdImageView: View {
    let copy: LaunchAdCopy

    var body: some View {
        if let localImagePath = copy.localImagePath,
           let image = UIImage(contentsOfFile: localImagePath) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
        } else {
            RemoteImageView(url: copy.imageURL, alignment: .center)
        }
    }
}

struct LaunchAdRemotePayload: Sendable {
    let imageUrl: String
    let targetUrl: String
    let imageVersion: String
    let displaySeconds: Int
    let sponsorZhCn: String
    let sponsorZhTw: String
    let sponsorEn: String
    let titleZhCn: String
    let titleZhTw: String
    let titleEn: String
    let descriptionZhCn: String
    let descriptionZhTw: String
    let descriptionEn: String
    let ctaZhCn: String
    let ctaZhTw: String
    let ctaEn: String
    let footerZhCn: String
    let footerZhTw: String
    let footerEn: String
}

struct LaunchAdCachedPayload: Codable, Sendable {
    let imageVersion: String
    let displaySeconds: Int
    let targetUrl: String
    let localImagePath: String
    let sponsorZhCn: String
    let sponsorZhTw: String
    let sponsorEn: String
    let titleZhCn: String
    let titleZhTw: String
    let titleEn: String
    let descriptionZhCn: String
    let descriptionZhTw: String
    let descriptionEn: String
    let ctaZhCn: String
    let ctaZhTw: String
    let ctaEn: String
    let footerZhCn: String
    let footerZhTw: String
    let footerEn: String
}

actor LaunchAdService {
    static let shared = LaunchAdService()

    private func makeClient() throws -> BackendAPIClient {
        let environment = try BackendEnvironment.current()
        return .init(session: BackendPinnedSession.shared, baseURL: environment.apiBaseURL)
    }

    func refreshCacheIfNeeded() async {
        do {
            guard let remote = try await fetchCurrent() else { return }
            let cachedVersion = await LaunchAdCacheStore.shared.cachedImageVersion()
            guard cachedVersion != remote.imageVersion else { return }
            try await LaunchAdCacheStore.shared.save(remote: remote)
        } catch {
            return
        }
    }

    private func fetchCurrent() async throws -> LaunchAdRemotePayload? {
        let client = try makeClient()
        guard let payload = try await client.request(path: "launch/ad/current", authMode: .optional) as? [String: Any] else {
            return nil
        }
        let imageUrl = (payload["imageUrl"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let targetUrl = (payload["targetUrl"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let imageVersion = (payload["imageVersion"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard imageUrl.isEmpty == false, targetUrl.isEmpty == false, imageVersion.isEmpty == false else {
            return nil
        }

        return LaunchAdRemotePayload(
            imageUrl: imageUrl,
            targetUrl: targetUrl,
            imageVersion: imageVersion,
            displaySeconds: max(1, payload["displaySeconds"] as? Int ?? 4),
            sponsorZhCn: payload["sponsorZhCn"] as? String ?? "",
            sponsorZhTw: payload["sponsorZhTw"] as? String ?? "",
            sponsorEn: payload["sponsorEn"] as? String ?? "",
            titleZhCn: payload["titleZhCn"] as? String ?? "",
            titleZhTw: payload["titleZhTw"] as? String ?? "",
            titleEn: payload["titleEn"] as? String ?? "",
            descriptionZhCn: payload["descriptionZhCn"] as? String ?? "",
            descriptionZhTw: payload["descriptionZhTw"] as? String ?? "",
            descriptionEn: payload["descriptionEn"] as? String ?? "",
            ctaZhCn: payload["ctaZhCn"] as? String ?? "",
            ctaZhTw: payload["ctaZhTw"] as? String ?? "",
            ctaEn: payload["ctaEn"] as? String ?? "",
            footerZhCn: payload["footerZhCn"] as? String ?? "",
            footerZhTw: payload["footerZhTw"] as? String ?? "",
            footerEn: payload["footerEn"] as? String ?? ""
        )
    }
}

actor LaunchAdCacheStore {
    static let shared = LaunchAdCacheStore()

    private let defaults = UserDefaults.standard
    private let metadataKey = "nexus.launch.ad.cached"

    func cachedImageVersion() -> String? {
        Self.cachedPayload(defaults: defaults)?.imageVersion
    }

    func save(remote: LaunchAdRemotePayload) async throws {
        let imageURL = try validateURL(remote.imageUrl)
        let (data, _) = try await URLSession.shared.data(from: imageURL)
        guard data.isEmpty == false else { return }

        let directory = try cacheDirectory()
        let fileExtension = imageURL.pathExtension.isEmpty ? "img" : imageURL.pathExtension
        let fileURL = directory.appendingPathComponent("launch-ad-\(remote.imageVersion).\(fileExtension)")
        try data.write(to: fileURL, options: .atomic)

        if let previous = Self.cachedPayload(defaults: defaults),
           previous.localImagePath != fileURL.path {
            try? FileManager.default.removeItem(atPath: previous.localImagePath)
        }

        let payload = LaunchAdCachedPayload(
            imageVersion: remote.imageVersion,
            displaySeconds: max(1, remote.displaySeconds),
            targetUrl: remote.targetUrl,
            localImagePath: fileURL.path,
            sponsorZhCn: remote.sponsorZhCn,
            sponsorZhTw: remote.sponsorZhTw,
            sponsorEn: remote.sponsorEn,
            titleZhCn: remote.titleZhCn,
            titleZhTw: remote.titleZhTw,
            titleEn: remote.titleEn,
            descriptionZhCn: remote.descriptionZhCn,
            descriptionZhTw: remote.descriptionZhTw,
            descriptionEn: remote.descriptionEn,
            ctaZhCn: remote.ctaZhCn,
            ctaZhTw: remote.ctaZhTw,
            ctaEn: remote.ctaEn,
            footerZhCn: remote.footerZhCn,
            footerZhTw: remote.footerZhTw,
            footerEn: remote.footerEn
        )
        if let encoded = try? JSONEncoder().encode(payload) {
            defaults.set(encoded, forKey: metadataKey)
        }
    }

    static func cachedCopy(for language: AppLanguage, defaults: UserDefaults = .standard) -> LaunchAdCopy? {
        guard let payload = cachedPayload(defaults: defaults) else { return nil }
        guard FileManager.default.fileExists(atPath: payload.localImagePath) else { return nil }
        return LaunchAdCopy.fromCached(payload, language: language)
    }

    private static func cachedPayload(defaults: UserDefaults) -> LaunchAdCachedPayload? {
        guard let data = defaults.data(forKey: "nexus.launch.ad.cached") else { return nil }
        return try? JSONDecoder().decode(LaunchAdCachedPayload.self, from: data)
    }

    private func cacheDirectory() throws -> URL {
        let root = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
        let directory = (root ?? FileManager.default.temporaryDirectory).appendingPathComponent("launch-ad", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }

    private func validateURL(_ raw: String) throws -> URL {
        guard let url = URL(string: raw), url.scheme?.isEmpty == false else {
            throw BackendAPIClientError.invalidResponse
        }
        return url
    }
}

private struct OnboardingView: View {
    let copy: LaunchCopy
    let onFinish: () -> Void

    @State private var pageIndex = 0

    var body: some View {
        GeometryReader { proxy in
            let heroHeight = proxy.size.height > 760 ? min(proxy.size.height * 0.67, 572) : proxy.size.height * 0.64
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
                                    imageAssetName: page.imageAssetName,
                                    imageAlignment: page.imageAlignment
                                )
                                .frame(height: heroHeight + proxy.safeAreaInsets.top)
                                .transition(.opacity)
                            }
                        }
                    }
                    .frame(height: heroHeight + proxy.safeAreaInsets.top)
                    .offset(y: -proxy.safeAreaInsets.top)
                    .animation(.easeInOut(duration: 0.28), value: pageIndex)

                    let current = copy.pages[pageIndex]

                    VStack(alignment: .leading, spacing: 0) {
                        Spacer()
                            .frame(height: 8)

                        VStack(alignment: .leading, spacing: 0) {
                            title(for: current)
                                .font(.system(size: 40, weight: .black))
                                .lineSpacing(8)
                                .fixedSize(horizontal: false, vertical: true)

                            Text(current.description)
                                .font(.system(size: 16))
                                .foregroundColor(Color(hex: 0xA0A0A0))
                                .lineSpacing(10)
                                .fixedSize(horizontal: false, vertical: true)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.top, 16)

                            HStack(spacing: 8) {
                                ForEach(Array(copy.pages.enumerated()), id: \.element.id) { index, page in
                                    Capsule()
                                        .fill(index == pageIndex ? page.highlightColor : Color(hex: 0x2C2C2C))
                                        .frame(width: index == pageIndex ? 32 : 16, height: 4)
                                }
                            }
                            .padding(.top, 16)
                        }
                        .offset(y: -42)

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
                        .offset(y: -24)
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
                        imageAssetName: "OnboardingHeroStep1",
                        imageAlignment: .center,
                        titlePrefix: "告别下载，",
                        titleHighlight: "秒开即玩。",
                        description: "为你量身定制的次世代游戏宇宙，海量精品大作，随时随地拯救不开心。",
                        highlightColor: Color(hex: 0xFFB020),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1638803040283-7a5ffa48bf0d?auto=format&fit=crop&w=800&q=80"),
                        imageAssetName: "OnboardingHeroStep2",
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
                        imageAssetName: "OnboardingHeroStep3",
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
                        imageAssetName: "OnboardingHeroStep1",
                        imageAlignment: .center,
                        titlePrefix: "告別下載，",
                        titleHighlight: "秒開即玩。",
                        description: "為你量身定製的次世代遊戲宇宙，海量精品大作，隨時隨地拯救不開心。",
                        highlightColor: Color(hex: 0xFFB020),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1638803040283-7a5ffa48bf0d?auto=format&fit=crop&w=800&q=80"),
                        imageAssetName: "OnboardingHeroStep2",
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
                        imageAssetName: "OnboardingHeroStep3",
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
                        imageAssetName: "OnboardingHeroStep1",
                        imageAlignment: .center,
                        titlePrefix: "Skip Downloads,",
                        titleHighlight: "Tap to Play.",
                        description: "A next-gen game universe tailored for you, packed with premium titles ready to lift every dull moment.",
                        highlightColor: Color(hex: 0xFFB020),
                        buttonColor: Color(hex: 0x6B4EFF),
                        buttonTextColor: .white
                    ),
                    LaunchPageCopy(
                        imageURL: URL(string: "https://images.unsplash.com/photo-1638803040283-7a5ffa48bf0d?auto=format&fit=crop&w=800&q=80"),
                        imageAssetName: "OnboardingHeroStep2",
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
                        imageAssetName: "OnboardingHeroStep3",
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
    let imageAssetName: String?
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
    let imageAssetName: String?
    let imageAlignment: Alignment

    var body: some View {
        ZStack {
            if let imageAssetName {
                LocalImageView(assetName: imageAssetName, alignment: imageAlignment)
            } else {
                RemoteImageView(url: imageURL, alignment: imageAlignment)
            }
        }
        .background(Color(hex: 0x121212))

        .overlay {
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

private struct LocalImageView: View {
    let assetName: String
    let alignment: Alignment

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color(hex: 0x121212)

                Image(assetName)
                    .resizable()
                    .scaledToFill()
                    .frame(width: proxy.size.width, height: proxy.size.height, alignment: alignment)
                    .clipped()
            }
        }
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
