// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "NexusPlatform",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: "NexusPlatform",
            targets: ["NexusPlatform"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/Alamofire/Alamofire.git", from: "5.8.0"),
        .package(url: "https://github.com/SwiftyJSON/SwiftyJSON.git", from: "5.0.0")
    ],
    targets: [
        .target(
            name: "NexusPlatform",
            dependencies: [
                .product(name: "Alamofire", package: "Alamofire"),
                .product(name: "SwiftyJSON", package: "SwiftyJSON")
            ]
        )
    ]
)
