import SwiftUI
import WebKit

struct GameView: View {
    let game: Game
    @State private var isLoading = true
    @State private var webView: WKWebView?
    
    var body: some View {
        ZStack {
            WebViewWrapper(game: game, isLoading: $isLoading, webView: $webView)
            
            if isLoading {
                ProgressView("加载中...")
                    .scaleEffect(1.5)
                    .padding()
                    .background(Color.white.opacity(0.9))
                    .cornerRadius(12)
            }
        }
        .navigationBarHidden(true)
        .statusBar(hidden: true)
        .onAppear {
            loadGame()
        }
    }
    
    private func loadGame() {
        guard let webView = webView else { return }
        GameManager.shared.loadGame(game, in: webView) { success in
            DispatchQueue.main.async {
                isLoading = !success
            }
        }
    }
}

struct WebViewWrapper: UIViewRepresentable {
    let game: Game
    @Binding var isLoading: Bool
    @Binding var webView: WKWebView?
    
    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaPlaybackRequiresUserAction = false
        
        let userContentController = WKUserContentController()
        let script = WKUserScript(
            source: GameManager.shared.getSDKContent(),
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        )
        userContentController.addUserScript(script)
        
        let schemeHandler = GameSchemeHandler(game: game)
        config.setURLSchemeHandler(schemeHandler, forURLScheme: "nexus")
        
        config.userContentController = userContentController
        
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.navigationDelegate = context.coordinator
        
        DispatchQueue.main.async {
            self.webView = webView
        }
        
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: WebViewWrapper
        
        init(_ parent: WebViewWrapper) {
            self.parent = parent
        }
        
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
        }
        
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
            print("WebView navigation failed: \(error.localizedDescription)")
        }
    }
}

class GameSchemeHandler: NSObject, WKURLSchemeHandler {
    let game: Game
    
    init(game: Game) {
        self.game = game
        super.init()
    }
    
    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let url = urlSchemeTask.request.url else {
            urlSchemeTask.didFailWithError(URLError(.badURL))
            return
        }
        
        let path = url.path
        
        if path == "/" || path == "/index.html" {
            let gameDir = GameManager.shared.getGameDirectory(gameId: game.id)
            let indexPath = gameDir.appendingPathComponent("index.html")
            
            if FileManager.default.fileExists(atPath: indexPath.path) {
                serveFile(at: indexPath, for: urlSchemeTask)
            } else {
                urlSchemeTask.didFailWithError(URLError(.fileDoesNotExist))
            }
        } else {
            let gameDir = GameManager.shared.getGameDirectory(gameId: game.id)
            let filePath = gameDir.appendingPathComponent(path)
            
            if FileManager.default.fileExists(atPath: filePath.path) {
                serveFile(at: filePath, for: urlSchemeTask)
            } else {
                urlSchemeTask.didFailWithError(URLError(.fileDoesNotExist))
            }
        }
    }
    
    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        urlSchemeTask.didFailWithError(URLError(.cancelled)
    }
    
    private func serveFile(at path: URL, for task: WKURLSchemeTask) {
        do {
            let data = try Data(contentsOf: path)
            let mimeType = getMimeType(for: path.pathExtension)
            let response = URLResponse(
                url: task.request.url!,
                mimeType: mimeType,
                expectedContentLength: data.count,
                textEncodingName: nil
            )
            task.didReceive(response)
            task.didReceive(data)
            task.didFinish()
        } catch {
            task.didFailWithError(error)
        }
    }
    
    private func getMimeType(for extension: String) -> String {
        switch extension.lowercased() {
        case "html":
            return "text/html"
        case "css":
            return "text/css"
        case "js":
            return "application/javascript"
        case "json":
            return "application/json"
        case "png":
            return "image/png"
        case "jpg", "jpeg":
            return "image/jpeg"
        case "gif":
            return "image/gif"
        case "svg":
            return "image/svg+xml"
        case "mp3":
            return "audio/mpeg"
        case "mp4":
            return "video/mp4"
        case "woff":
            return "font/woff"
        case "woff2":
            return "font/woff2"
        case "ttf":
            return "font/ttf"
        default:
            return "application/octet-stream"
        }
    }
}
