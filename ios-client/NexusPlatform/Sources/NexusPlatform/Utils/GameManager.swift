import Foundation
import WebKit

struct GameLaunchSummary: Sendable {
    let activeVersion: String
    let forceUpdated: Bool
}

final class GameManager {
    static let shared = GameManager()

    private let launchCoordinator: GameLaunchCoordinator
    private let storageManager: VersionedGameStorageManager
    private let updateState: RuntimeUpdateStateStore

    private init(
        launchCoordinator: GameLaunchCoordinator = GameLaunchCoordinator(),
        storageManager: VersionedGameStorageManager = VersionedGameStorageManager(),
        updateState: RuntimeUpdateStateStore = .shared
    ) {
        self.launchCoordinator = launchCoordinator
        self.storageManager = storageManager
        self.updateState = updateState
    }

    func prepareLaunch(
        game: Game,
        onForceProgress: (@Sendable (GameInstallProgress) -> Void)? = nil
    ) async throws -> GameLaunchSummary {
        await updateState.setCurrentGameID(game.id)
        let result = try await launchCoordinator.prepareLaunch(game: game, forceProgress: onForceProgress)
        return GameLaunchSummary(activeVersion: result.activeVersion, forceUpdated: result.forceUpdated)
    }

    func updateSnapshot(gameID: String?) async -> RuntimeUpdateSnapshot? {
        if let gameID, gameID.isEmpty == false {
            return await updateState.snapshot(gameID: gameID)
        }
        guard let current = await updateState.currentGame() else {
            return nil
        }
        return await updateState.snapshot(gameID: current)
    }

    func applyPendingUpdate(gameID: String?) async throws {
        let resolvedID: String
        if let gameID, gameID.isEmpty == false {
            resolvedID = gameID
        } else if let current = await updateState.currentGame() {
            resolvedID = current
        } else {
            throw GameLaunchError.packageInstallFailed("未找到当前游戏上下文")
        }
        try await updateState.applyPending(gameID: resolvedID, storage: storageManager)
    }

    func loadGame(_ game: Game, in webView: WKWebView, completion: @escaping (Bool) -> Void) {
        Task {
            do {
                _ = try await prepareLaunch(game: game)
                guard let gameURL = URL(string: "nexus://\(game.id)/index.html") else {
                    completion(false)
                    return
                }
                webView.load(URLRequest(url: gameURL))
                completion(true)
            } catch {
                completion(false)
            }
        }
    }

    func getSDKContent() -> String {
        let fileManager = FileManager.default
        let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]

        let direct = documents.appendingPathComponent("wx-mock-sdk.js")
        if let text = try? String(contentsOf: direct), text.isEmpty == false {
            return text
        }

        let bundledPath = "wx-mock-sdk.iife"
        let bundledExt = "js"
        if let url = Bundle.main.url(forResource: bundledPath, withExtension: bundledExt),
           let text = try? String(contentsOf: url),
           text.isEmpty == false {
            return text
        }

        return defaultSDKStub()
    }

    private func defaultSDKStub() -> String {
        """
        ;(function () {
          if (window.wx) { return; }
          var callbacks = {};
          window.NexusBridgeCallback = function (response) {
            if (!response || !response.callbackId) { return; }
            var callback = callbacks[response.callbackId];
            if (!callback) { return; }
            delete callbacks[response.callbackId];
            if (response.error) { callback.reject(response.error); return; }
            callback.resolve(response.data || {});
          };
          function call(api, params) {
            return new Promise(function (resolve, reject) {
              if (!window.webkit || !window.webkit.messageHandlers || !window.webkit.messageHandlers.NexusBridge) {
                reject({ code: -1, errMsg: api + ':fail bridge unavailable' });
                return;
              }
              var callbackId = 'cb_' + Date.now() + '_' + Math.random().toString(16).slice(2);
              callbacks[callbackId] = { resolve: resolve, reject: reject };
              window.webkit.messageHandlers.NexusBridge.postMessage({
                api: api,
                params: params || {},
                callbackId: callbackId
              });
            });
          }
          function pass(api) { return Promise.resolve({ errMsg: api + ':ok' }); }
          function createUpdateManager() {
            var checked = [];
            var ready = [];
            var failed = [];
            function emit(list, payload) {
              list.forEach(function (cb) { try { cb(payload || {}); } catch (e) {} });
            }
            function poll() {
              call('wx.update.check', {}).then(function (res) {
                emit(checked, { hasUpdate: !!res.hasUpdate });
                if (res.ready) {
                  emit(ready, {});
                } else if (res.hasUpdate) {
                  setTimeout(poll, 1200);
                }
              }).catch(function () {
                emit(failed, {});
              });
            }
            setTimeout(poll, 0);
            return {
              onCheckForUpdate: function (cb) { if (typeof cb === 'function') checked.push(cb); },
              onUpdateReady: function (cb) { if (typeof cb === 'function') ready.push(cb); },
              onUpdateFailed: function (cb) { if (typeof cb === 'function') failed.push(cb); },
              applyUpdate: function () { return call('wx.update.apply', {}); }
            };
          }
          window.wx = {
            login: function (params) { return call('wx.login', params); },
            request: function (params) { return call('wx.request', params); },
            setStorage: function (params) { return call('wx.setStorage', params); },
            getStorage: function (params) { return call('wx.getStorage', params); },
            removeStorage: function (params) { return call('wx.removeStorage', params); },
            clearStorage: function () { return call('wx.clearStorage', {}); },
            setStorageSync: function (params) { return call('wx.setStorageSync', params); },
            getStorageSync: function (params) { return call('wx.getStorageSync', params); },
            removeStorageSync: function (params) { return call('wx.removeStorageSync', params); },
            clearStorageSync: function () { return call('wx.clearStorageSync', {}); },
            getMenuButtonBoundingClientRect: function () { return call('wx.getMenuButtonBoundingClientRect', {}); },
            showToast: function () { return pass('wx.showToast'); },
            getUpdateManager: function () { return createUpdateManager(); }
          };
        })();
        """
    }
}
