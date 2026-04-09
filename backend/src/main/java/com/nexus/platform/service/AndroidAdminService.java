package com.nexus.platform.service;

import com.nexus.platform.dto.AndroidAdminDtos.AndroidBridgeApiItem;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidConsolePayload;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGameAssetRow;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidHostCapability;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidOverview;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidRuntimeConfig;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidRuntimeConfigUpdateRequest;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AndroidAdminService {
    private final GameRepository gameRepository;

    private final AtomicReference<AndroidRuntimeConfig> runtimeConfigRef =
            new AtomicReference<>(new AndroidRuntimeConfig(
                    "http://10.0.2.2:8080/api/v1",
                    "https://appassets.androidplatform.net/assets/",
                    "zh-TW",
                    true,
                    true,
                    false,
                    "MIXED_CONTENT_NEVER_ALLOW",
                    "LOAD_DEFAULT",
                    200,
                    "SPLASH_DIRECT_TO_MAIN",
                    "system",
                    LocalDateTime.now()
            ));

    public AndroidConsolePayload getConsolePayload() {
        return new AndroidConsolePayload(
                buildOverview(),
                runtimeConfigRef.get(),
                hostCapabilities(),
                bridgeApis(),
                gameAssets()
        );
    }

    public AndroidRuntimeConfig getRuntimeConfig() {
        return runtimeConfigRef.get();
    }

    public AndroidRuntimeConfig updateRuntimeConfig(AndroidRuntimeConfigUpdateRequest request, User currentUser) {
        AndroidRuntimeConfig current = runtimeConfigRef.get();
        AndroidRuntimeConfig next = new AndroidRuntimeConfig(
                valueOrDefault(request.apiBaseUrl(), current.apiBaseUrl()),
                valueOrDefault(request.assetHost(), current.assetHost()),
                valueOrDefault(request.defaultLanguage(), current.defaultLanguage()),
                boolOrDefault(request.debugUseMockData(), current.debugUseMockData()),
                boolOrDefault(request.enableSyncBridge(), current.enableSyncBridge()),
                boolOrDefault(request.allowCleartextTraffic(), current.allowCleartextTraffic()),
                valueOrDefault(request.webViewMixedContentMode(), current.webViewMixedContentMode()),
                valueOrDefault(request.webViewCacheMode(), current.webViewCacheMode()),
                intOrDefault(request.maxZipSizeMb(), current.maxZipSizeMb()),
                valueOrDefault(request.startupRoutePolicy(), current.startupRoutePolicy()),
                currentUser == null ? "unknown" : currentUser.getUsername(),
                LocalDateTime.now()
        );
        runtimeConfigRef.set(next);
        return next;
    }

    public List<AndroidBridgeApiItem> getBridgeApis() {
        return bridgeApis();
    }

    public List<AndroidGameAssetRow> getGameAssets() {
        return gameAssets();
    }

    private AndroidOverview buildOverview() {
        List<Game> games = gameRepository.findAll();
        long total = games.size();
        long approved = games.stream().filter(g -> g.getStatus() == Game.GameStatus.APPROVED).count();
        long pending = games.stream().filter(g -> g.getStatus() == Game.GameStatus.PENDING).count();
        long processing = games.stream().filter(g -> g.getStatus() == Game.GameStatus.PROCESSING).count();
        long rejected = games.stream().filter(g -> g.getStatus() == Game.GameStatus.REJECTED).count();
        long runtimeReady = games.stream()
                .filter(g -> g.getStatus() == Game.GameStatus.APPROVED)
                .filter(g -> g.getDownloadUrl() != null && !g.getDownloadUrl().isBlank())
                .filter(g -> g.getMd5() != null && !g.getMd5().isBlank())
                .count();
        int implemented = (int) bridgeApis().stream().filter(api -> "implemented".equals(api.supportStatus())).count();
        int partial = (int) bridgeApis().stream().filter(api -> !"implemented".equals(api.supportStatus())).count();
        return new AndroidOverview(total, approved, pending, processing, rejected, runtimeReady, implemented, partial);
    }

    private List<AndroidHostCapability> hostCapabilities() {
        return List.of(
                capability(
                        "android.webview.lockdown",
                        "WebView Sandbox",
                        "security",
                        "implemented",
                        "GameRuntimeActivity",
                        "JavaScript enabled with file/content access disabled and mixed content blocked by default"
                ),
                capability(
                        "android.asset.loader",
                        "App Asset Loader",
                        "delivery",
                        "implemented",
                        "GameRuntimeActivity",
                        "Loads game bundles from app internal storage via appassets domain"
                ),
                capability(
                        "android.bridge.async",
                        "Async JS Bridge",
                        "bridge",
                        "implemented",
                        "NexusBridge",
                        "AndroidApp bridge handles async wx APIs and callbacks"
                ),
                capability(
                        "android.bridge.sync",
                        "Sync JS Bridge",
                        "bridge",
                        "implemented",
                        "NexusSyncBridge",
                        "AndroidAppSync handles sync bridge invocation for supported APIs"
                ),
                capability(
                        "android.runtime.update",
                        "Runtime Update Channel",
                        "delivery",
                        "implemented",
                        "UpdateApi/GameManager",
                        "Supports update check and apply flow before entering runtime"
                ),
                capability(
                        "android.runtime.overlay",
                        "Runtime Capsule Menu",
                        "runtime",
                        "implemented",
                        "GameRuntimeActivity",
                        "Native runtime overlay supports back, share, favorite and refresh actions"
                ),
                capability(
                        "android.native.network",
                        "Native Network Proxy",
                        "bridge",
                        "implemented",
                        "RequestApi",
                        "wx.request is proxied through native OkHttp"
                ),
                capability(
                        "android.local.storage",
                        "Local Storage",
                        "bridge",
                        "implemented",
                        "StorageApi",
                        "wx storage APIs are backed by SharedPreferences"
                ),
                capability(
                        "android.media.integration",
                        "Media & Clipboard",
                        "device",
                        "partial",
                        "ImageApi/ClipboardApi",
                        "Image selection and save APIs are mock-oriented, clipboard APIs are available"
                )
        );
    }

    private List<AndroidBridgeApiItem> bridgeApis() {
        return List.of(
                api("wx.login", "implemented", false, "LoginApi", "returns runtime session code derived from auth state"),
                api("wx.request", "implemented", false, "RequestApi", "native http proxy backed by OkHttp"),
                api("wx.getSystemInfoSync", "implemented", true, "SystemInfoApi", "returns runtime metrics and safe-area data"),
                api("wx.getMenuButtonBoundingClientRect", "implemented", true, "SystemInfoApi", "returns runtime capsule coordinates"),
                api("wx.update.check", "implemented", false, "UpdateApi", "checks cached bundle and remote version state"),
                api("wx.update.apply", "implemented", false, "UpdateApi", "applies prepared bundle update and restarts runtime"),
                api("wx.setStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.getStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.removeStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.clearStorage", "implemented", false, "StorageApi", "shared preferences persistence"),
                api("wx.getUserInfo", "implemented", false, "UserInfoApi", "returns profile from backend session when available"),
                api("wx.shareAppMessage", "implemented", false, "ShareApi", "delegates to native share sheet"),
                api("wx.showToast", "implemented", false, "ToastApi", "native toast prompt"),
                api("wx.showModal", "implemented", false, "ModalApi", "renders native modal and returns user decision"),
                api("wx.downloadFile", "implemented", false, "FileApi", "downloads file via OkHttp into app cache"),
                api("wx.uploadFile", "implemented", false, "FileApi", "uploads local file using multipart form"),
                api("wx.getNetworkType", "implemented", false, "NetworkApi", "reads active transport from ConnectivityManager"),
                api("wx.chooseImage", "implemented", false, "ImageApi", "returns latest media items from device gallery query"),
                api("wx.previewImage", "implemented", false, "ImageApi", "opens native image viewer via ACTION_VIEW"),
                api("wx.getImageInfo", "implemented", false, "ImageApi", "reads dimensions/type from local or remote image source"),
                api("wx.saveImageToPhotosAlbum", "implemented", false, "ImageApi", "copies image into MediaStore album"),
                api("wx.setClipboardData", "implemented", false, "ClipboardApi", "system clipboard write"),
                api("wx.getClipboardData", "implemented", false, "ClipboardApi", "system clipboard read"),
                api("wx.vibrateShort", "implemented", false, "VibrateApi", "native vibration"),
                api("wx.vibrateLong", "implemented", false, "VibrateApi", "native vibration")
        );
    }

    private List<AndroidGameAssetRow> gameAssets() {
        return gameRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(game -> new AndroidGameAssetRow(
                        game.getId(),
                        game.getAppId(),
                        game.getName(),
                        game.getVersion(),
                        game.getStatus() == null ? "UNKNOWN" : game.getStatus().name(),
                        game.getDownloadUrl(),
                        game.getMd5(),
                        game.getStatus() == Game.GameStatus.APPROVED
                                && game.getDownloadUrl() != null
                                && !game.getDownloadUrl().isBlank(),
                        game.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    private AndroidBridgeApiItem api(String name, String status, boolean sync, String module, String notes) {
        return new AndroidBridgeApiItem(name, status, sync, module, notes);
    }

    private AndroidHostCapability capability(
            String key,
            String name,
            String category,
            String status,
            String sourceModule,
            String summary
    ) {
        return new AndroidHostCapability(key, name, category, status, sourceModule, summary);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean boolOrDefault(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private int intOrDefault(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }
}
