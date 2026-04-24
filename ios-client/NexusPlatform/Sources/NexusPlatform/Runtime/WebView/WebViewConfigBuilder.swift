import Foundation
import WebKit

struct GameLayoutMetrics: Equatable, Sendable {
    let windowWidth: CGFloat
    let windowHeight: CGFloat
    let safeAreaTop: CGFloat
    let safeAreaBottom: CGFloat
    let safeAreaLeft: CGFloat
    let safeAreaRight: CGFloat
    let menuButtonRect: CGRect

    var safeAreaRect: CGRect {
        CGRect(
            x: safeAreaLeft,
            y: safeAreaTop,
            width: max(windowWidth - safeAreaLeft - safeAreaRight, 0),
            height: max(windowHeight - safeAreaTop - safeAreaBottom, 0)
        )
    }

    var viewportRect: CGRect {
        CGRect(x: 0, y: 0, width: max(windowWidth, 0), height: max(windowHeight, 0))
    }
}

struct WebViewConfigBuilder {
    let schemeHandler: WKURLSchemeHandler
    let injectedSDKScript: String
    let layoutMetrics: GameLayoutMetrics

    func build() -> WKWebViewConfiguration {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []

        let userContentController = WKUserContentController()
        if !injectedSDKScript.isEmpty {
            let script = WKUserScript(
                source: injectedSDKScript,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: true
            )
            userContentController.addUserScript(script)
        }
        let layoutScript = WKUserScript(
            source: layoutBootstrapScript(metrics: layoutMetrics),
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        )
        userContentController.addUserScript(layoutScript)

        configuration.userContentController = userContentController
        configuration.setURLSchemeHandler(schemeHandler, forURLScheme: "nexus")

        let preferences = WKPreferences()
        preferences.javaScriptCanOpenWindowsAutomatically = true
        configuration.preferences = preferences

        if #available(iOS 14.0, *) {
            configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        }

        return configuration
    }

    func layoutUpdateScript() -> String {
        let payload = serializedMetricsPayload(layoutMetrics)
        return """
        ;(function () {
          var nextMetrics = \(payload);
          if (typeof window.__NEXUS_APPLY_LAYOUT_METRICS__ === 'function') {
            window.__NEXUS_APPLY_LAYOUT_METRICS__(nextMetrics);
          } else {
            \(layoutBootstrapScript(metrics: layoutMetrics))
          }
        })();
        """
    }

    private func layoutBootstrapScript(metrics: GameLayoutMetrics) -> String {
        let payload = serializedMetricsPayload(metrics)
        return """
        ;(function () {
          var metrics = \(payload);
          function clone(value) {
            return JSON.parse(JSON.stringify(value));
          }
          function systemInfo() {
            return {
              brand: 'Apple',
              model: 'iPhone',
              pixelRatio: window.devicePixelRatio || 1,
              screenWidth: metrics.windowWidth,
              screenHeight: metrics.windowHeight,
              windowWidth: metrics.windowWidth,
              windowHeight: metrics.windowHeight,
              safeArea: clone(metrics.safeArea),
              statusBarHeight: metrics.safeArea.top,
              platform: 'ios',
              SDKVersion: '1.0.0'
            };
          }
          window.__NEXUS_LAYOUT_METRICS__ = metrics;
          window.__NEXUS_APPLY_LAYOUT_METRICS__ = function (nextMetrics) {
            metrics = nextMetrics;
            window.__NEXUS_LAYOUT_METRICS__ = nextMetrics;
            if (typeof window.dispatchEvent === 'function' && typeof CustomEvent === 'function') {
              window.dispatchEvent(new CustomEvent('nexuslayoutchange', { detail: clone(nextMetrics) }));
            }
          };
          window.wx = window.wx || {};
          window.wx.getSystemInfoSync = function () {
            return systemInfo();
          };
          window.wx.getSystemInfo = function (options) {
            var result = systemInfo();
            if (options && typeof options.success === 'function') {
              options.success(result);
            }
            if (options && typeof options.complete === 'function') {
              options.complete(result);
            }
            return Promise.resolve(result);
          };
          window.wx.getMenuButtonBoundingClientRect = function () {
            return clone(metrics.menuButtonRect);
          };
          window.wx.nexusLayout = window.wx.nexusLayout || {};
          window.wx.nexusLayout.getSafeArea = function () {
            return clone(metrics.safeArea);
          };
          window.wx.nexusLayout.getGameViewport = function () {
            return clone(metrics.viewport);
          };
          window.wx.nexusLayout.applyCanvasSafeArea = function (canvas) {
            if (!canvas || !canvas.style) {
              return clone(metrics.viewport);
            }
            canvas.style.position = 'absolute';
            canvas.style.left = metrics.viewport.x + 'px';
            canvas.style.top = metrics.viewport.y + 'px';
            canvas.style.width = metrics.viewport.width + 'px';
            canvas.style.height = metrics.viewport.height + 'px';
            return clone(metrics.viewport);
          };
        })();
        """
    }

    private func serializedMetricsPayload(_ metrics: GameLayoutMetrics) -> String {
        let safeArea = metrics.safeAreaRect
        let viewport = metrics.viewportRect
        let payload: [String: Any] = [
            "windowWidth": metrics.windowWidth,
            "windowHeight": metrics.windowHeight,
            "safeArea": [
                "left": safeArea.minX,
                "top": safeArea.minY,
                "right": safeArea.maxX,
                "bottom": safeArea.maxY,
                "width": safeArea.width,
                "height": safeArea.height,
                "x": safeArea.minX,
                "y": safeArea.minY
            ],
            "viewport": [
                "x": viewport.minX,
                "y": viewport.minY,
                "width": viewport.width,
                "height": viewport.height
            ],
            "menuButtonRect": [
                "left": metrics.menuButtonRect.minX,
                "top": metrics.menuButtonRect.minY,
                "right": metrics.menuButtonRect.maxX,
                "bottom": metrics.menuButtonRect.maxY,
                "width": metrics.menuButtonRect.width,
                "height": metrics.menuButtonRect.height,
                "x": metrics.menuButtonRect.minX,
                "y": metrics.menuButtonRect.minY
            ]
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let json = String(data: data, encoding: .utf8) else {
            return "{}"
        }
        return json
    }
}
