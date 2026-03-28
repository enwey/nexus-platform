package com.nexus.platform.service;

import com.nexus.platform.dto.AndroidAdminDtos.AndroidBridgeApiItem;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidConsolePayload;
import com.nexus.platform.dto.AndroidAdminDtos.AndroidGameAssetRow;
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
        int implemented = (int) bridgeApis().stream().filter(api -> "implemented".equals(api.supportStatus())).count();
        int stub = (int) bridgeApis().stream().filter(api -> !"implemented".equals(api.supportStatus())).count();
        return new AndroidOverview(total, approved, pending, processing, rejected, implemented, stub);
    }

    private List<AndroidBridgeApiItem> bridgeApis() {
        return List.of(
                api("wx.login", "implemented", false, "SystemApis", "returns mock login code"),
                api("wx.request", "implemented", false, "NetworkApis", "native http proxy"),
                api("wx.getSystemInfoSync", "implemented", true, "SystemApis", "sync bridge ready"),
                api("wx.setStorage", "implemented", false, "StorageApi", "shared preferences"),
                api("wx.getStorage", "implemented", false, "StorageApi", "shared preferences"),
                api("wx.removeStorage", "implemented", false, "StorageApi", "shared preferences"),
                api("wx.clearStorage", "implemented", false, "StorageApi", "shared preferences"),
                api("wx.showToast", "implemented", false, "NetworkApis", "native toast"),
                api("wx.showModal", "implemented", false, "NetworkApis", "returns mock confirm"),
                api("wx.downloadFile", "implemented", false, "OtherApis", "returns mock file path"),
                api("wx.uploadFile", "implemented", false, "OtherApis", "returns mock upload"),
                api("wx.getNetworkType", "implemented", false, "NetworkApis", "returns wifi"),
                api("wx.chooseImage", "implemented", false, "OtherApis", "currently mock"),
                api("wx.setClipboardData", "implemented", false, "OtherApis", "system clipboard"),
                api("wx.getClipboardData", "implemented", false, "OtherApis", "system clipboard"),
                api("wx.vibrateShort", "implemented", false, "OtherApis", "native vibration"),
                api("wx.vibrateLong", "implemented", false, "OtherApis", "native vibration"),
                api("wx.navigateToMiniProgram", "stub", false, "SDK Stub", "no-op in host app"),
                api("wx.openSetting", "stub", false, "UnsupportedApi", "not yet implemented")
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
