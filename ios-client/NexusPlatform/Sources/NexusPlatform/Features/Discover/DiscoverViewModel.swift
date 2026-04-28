import Foundation

@MainActor
final class DiscoverViewModel: ObservableObject {
    @Published private(set) var isLoading = false
    @Published private(set) var categories: [String] = ["全部"]
    @Published private(set) var selectedCategory: String = "全部"
    @Published private(set) var games: [Game] = []
    @Published private(set) var topRanked: [Game] = []
    @Published private(set) var hero: DiscoverHero?
    @Published private(set) var heroGame: Game?
    @Published private(set) var heroFallbackMessage: String?
    @Published private(set) var errorMessage: String?

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
            isLoading = true
            defer { isLoading = false }
            do {
                errorMessage = nil
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

                let availableCategories = Set(
                    loaded.compactMap { game -> String? in
                        guard let category = game.category?.trimmingCharacters(in: .whitespacesAndNewlines),
                              category.isEmpty == false else {
                            return nil
                        }
                        return category
                    }
                )
                let orderedCategories = home.categories
                    .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                    .filter { label in
                        label.isEmpty == false &&
                        label.caseInsensitiveCompare("all") != .orderedSame &&
                        label != "全部" &&
                        availableCategories.contains(label)
                    }
                let remainingCategories = availableCategories
                    .filter { orderedCategories.contains($0) == false }
                    .sorted()
                categories = ["全部"] + orderedCategories + remainingCategories
                if categories.contains(selectedCategory) == false {
                    selectedCategory = "全部"
                }

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
                if games.isEmpty {
                    games = []
                    topRanked = []
                    categories = ["全部"]
                    hero = nil
                    heroGame = nil
                    heroFallbackMessage = nil
                }
                errorMessage = error.localizedDescription
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
