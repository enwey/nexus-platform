import Foundation

@MainActor
final class DiscoverViewModel: ObservableObject {
    @Published private(set) var categories: [String] = ["全部"]
    @Published private(set) var selectedCategory: String = "全部"
    @Published private(set) var games: [Game] = []
    @Published private(set) var topRanked: [Game] = []
    @Published private(set) var hero: DiscoverHero?
    @Published private(set) var heroGame: Game?
    @Published private(set) var heroFallbackMessage: String?

    private let service: GameCatalogServiceProtocol
    private let homeService: DiscoverHomeServiceProtocol

    init(
        service: GameCatalogServiceProtocol = GameCatalogService(),
        homeService: DiscoverHomeServiceProtocol = DiscoverHomeService()
    ) {
        self.service = service
        self.homeService = homeService
    }

    func load() {
        Task {
            do {
                let home = try await homeService.fetchHome(limit: 20)
                hero = home.hero
                heroFallbackMessage = nil

                let loaded = try await service.fetchGames()
                games = loaded

                if home.rankedGames.isEmpty {
                    topRanked = Array(loaded.prefix(3))
                } else {
                    topRanked = home.rankedGames
                }

                let baseCategories = home.categories.isEmpty ? ["全部"] : home.categories
                let dynamic = loaded.compactMap { $0.category }.filter { !$0.isEmpty }
                let deduped = Array(Set(baseCategories + dynamic))
                    .filter { $0.isEmpty == false && $0 != "all" }
                    .sorted()
                categories = ["全部"] + deduped.filter { $0 != "全部" }

                if let hero {
                    if let matched = loaded.first(where: { $0.id == hero.appID }) {
                        heroGame = matched
                    } else {
                        heroGame = (try? await service.fetchGame(appID: hero.appID)) ?? nil
                    }

                    if heroGame == nil {
                        // If ops config points to an invalid appId, keep the hero usable by
                        // gracefully falling back to a playable game.
                        heroGame = topRanked.first ?? loaded.first
                        if heroGame != nil {
                            heroFallbackMessage = "顶部推荐配置异常，已自动回退到可玩的游戏"
                        }
                    }
                } else {
                    heroGame = nil
                }
            } catch {
                games = []
                topRanked = []
                categories = ["全部"]
                hero = nil
                heroGame = nil
                heroFallbackMessage = nil
            }
        }
    }

    func selectCategory(_ value: String) {
        selectedCategory = value
    }

    var filteredGames: [Game] {
        if selectedCategory == "全部" {
            return games
        }
        return games.filter {
            ($0.category ?? "").caseInsensitiveCompare(selectedCategory) == .orderedSame
        }
    }
}
