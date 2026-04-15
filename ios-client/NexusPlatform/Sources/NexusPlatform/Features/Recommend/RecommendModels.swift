import Foundation

struct RecommendTodayItem: Identifiable, Sendable {
    let id: String
    let appID: String
    let gameName: String
    let gameIconURL: String
    let gameCategory: String
    let coverURL: String
    let cardCategory: String
    let cardTitle: String
    let articleTag: String
    let articleTitle: String
    let articleBody: String
    let actionText: String
}
