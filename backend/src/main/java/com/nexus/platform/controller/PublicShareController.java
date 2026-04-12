package com.nexus.platform.controller;

import com.nexus.platform.config.ShareLandingProperties;
import com.nexus.platform.dto.GameOpsDtos.RuntimeProfileResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.GameOpsProfileService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/share")
@RequiredArgsConstructor
public class PublicShareController {
    private static final String SHARE_PAGE_VERSION = "2026-04-13-ui06-v4";
    private final GameOpsProfileService gameOpsProfileService;
    private final ShareLandingProperties shareLandingProperties;

    @GetMapping(value = "/game/{appId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> gameLanding(@PathVariable String appId) {
        Result<RuntimeProfileResponse> profileResult = gameOpsProfileService.getRuntimeProfile(appId);
        if (profileResult.getCode() != 0 || profileResult.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        RuntimeProfileResponse profile = profileResult.getData();

        String title = firstNonBlank(profile.shareTitle(), profile.gameName(), "Nexus Game");
        String subtitle = firstNonBlank(profile.shareSubtitle(), "Open instantly on BringBox");
        String heroBanner = firstNonBlank(profile.runtimeBannerUrl(), profile.shareImageUrl(), "");
        String logoImage = firstNonBlank(profile.shareImageUrl(), profile.runtimeBannerUrl(), "");
        String playerCountText = firstNonBlank(profile.playerCountText(), "240萬+ 玩家");
        String studioName = firstNonBlank(profile.studioName(), "Nexus 官方優選作品");
        String openScheme = buildOpenSchemeUrl(appId);

        String html = renderHtml(
                title,
                subtitle,
                heroBanner,
                logoImage,
                playerCountText,
                studioName,
                openScheme,
                shareLandingProperties.getAndroidInstallUrl(),
                shareLandingProperties.getIosInstallUrl(),
                shareLandingProperties.getOtherInstallUrl()
        );
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("X-Share-Page-Version", SHARE_PAGE_VERSION)
                .body(html);
    }

    private String safeSchemeTemplate() {
        String template = shareLandingProperties.getAppSchemeTemplate();
        if (template == null || template.isBlank()) {
            return "bringbox://game/{appId}";
        }
        return template;
    }

    private String buildOpenSchemeUrl(String appId) {
        String template = safeSchemeTemplate();
        if (template.contains("{appId}")) {
            return template.replace("{appId}", appId);
        }
        if (template.contains("%s")) {
            return template.replace("%s", appId);
        }
        if (template.endsWith("/")) {
            return template + appId;
        }
        return template + "/" + appId;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String renderHtml(
            String title,
            String subtitle,
            String bannerUrl,
            String logoUrl,
            String playerCountText,
            String studioName,
            String appSchemeUrl,
            String androidInstallUrl,
            String iosInstallUrl,
            String otherInstallUrl
    ) {
        String safeTitle = escapeHtml(title);
        String safeSubtitle = escapeHtml(subtitle);
        String safePlayerCountText = escapeHtml(playerCountText);
        String safeStudioName = escapeHtml(studioName);
        return """
                <!doctype html>
                <!-- share-ui-version: %s -->
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                  <style>
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                      background: #090a0f;
                      color: #fff;
                    }
                    .wrap {
                      max-width: 480px;
                      margin: 0 auto;
                      min-height: 100vh;
                      padding-bottom: 110px;
                    }
                    .hero {
                      position: relative;
                      height: 300px;
                      background: linear-gradient(to bottom, rgba(9,10,15,0) 0%%, #090a0f 100%%), #1d2230 center top/contain no-repeat;
                    }
                    .content {
                      position: relative;
                      z-index: 2;
                      margin-top: -40px;
                      padding: 0 20px;
                    }
                    .detail-header {
                      display: flex;
                      align-items: center;
                      gap: 16px;
                    }
                    .detail-icon {
                      width: 88px;
                      height: 88px;
                      border-radius: 22px;
                      background: #222433 center/cover no-repeat;
                      border: 1px solid rgba(255,255,255,0.1);
                      box-shadow: 0 16px 30px rgba(0,0,0,0.35);
                      flex-shrink: 0;
                    }
                    .title {
                      margin: 0 0 6px;
                      font-size: 24px;
                      font-weight: 800;
                    }
                    .studio {
                      margin: 0;
                      color: #5c67ff;
                      font-size: 13px;
                      font-weight: 600;
                    }
                    .stats {
                      display: flex;
                      gap: 12px;
                      margin-top: 18px;
                    }
                    .stat {
                      flex: 1;
                      background: #15161d;
                      border: 1px solid rgba(255,255,255,0.08);
                      border-radius: 14px;
                      padding: 14px 10px;
                      text-align: center;
                    }
                    .stat-top {
                      font-size: 18px;
                      font-weight: 800;
                    }
                    .stat-bottom {
                      margin-top: 4px;
                      font-size: 11px;
                      color: #8b8d99;
                    }
                    .section-title {
                      margin: 22px 0 12px;
                      font-size: 16px;
                      font-weight: 700;
                    }
                    .desc {
                      margin: 0;
                      color: #8b8d99;
                      font-size: 14px;
                      line-height: 1.6;
                    }
                    .play-fab {
                      position: fixed;
                      left: 50%%;
                      bottom: max(18px, env(safe-area-inset-bottom));
                      transform: translateX(-50%%);
                      width: min(440px, calc(100vw - 28px));
                      padding: 0 4px;
                    }
                    .cta {
                      width: 100%%;
                      height: 64px;
                      border: 0;
                      border-radius: 20px;
                      background: linear-gradient(90deg, #5c67ff, #cb63ff);
                      color: #fff;
                      font-size: 18px;
                      font-weight: 700;
                    }
                  </style>
                </head>
                <body>
                  <main class="wrap">
                    <div id="hero" class="hero"></div>
                    <section class="content">
                      <div class="detail-header">
                        <div id="icon" class="detail-icon"></div>
                        <div>
                          <h1 class="title">%s</h1>
                          <p class="studio">%s</p>
                        </div>
                      </div>
                      <div class="stats">
                        <div class="stat">
                          <div class="stat-top">4.9 ★</div>
                          <div class="stat-bottom">%s</div>
                        </div>
                        <div class="stat">
                          <div class="stat-top">#1</div>
                          <div class="stat-bottom">動作排行榜</div>
                        </div>
                        <div class="stat">
                          <div class="stat-top">18<span style="font-size:12px;">MB</span></div>
                          <div class="stat-bottom">引擎極速包</div>
                        </div>
                      </div>
                      <h3 class="section-title">遊戲簡介</h3>
                      <p class="desc">%s</p>
                    </section>
                    <div class="play-fab">
                      <button id="openBtn" class="cta">立即秒開</button>
                    </div>
                  </main>

                  <script>
                    const APP_SCHEME_URL = "%s";
                    const ANDROID_INSTALL_URL = "%s";
                    const IOS_INSTALL_URL = "%s";
                    const OTHER_INSTALL_URL = "%s";
                    const BANNER_URL = "%s";
                    const LOGO_URL = "%s";

                    const openBtn = document.getElementById("openBtn");
                    const hero = document.getElementById("hero");
                    const icon = document.getElementById("icon");

                    if (BANNER_URL) {
                      hero.style.backgroundImage = `linear-gradient(to bottom, rgba(9,10,15,0) 0%%, #090a0f 100%%), url("${BANNER_URL}")`;
                      hero.style.backgroundPosition = "center top";
                      hero.style.backgroundSize = "contain";
                      hero.style.backgroundRepeat = "no-repeat";
                    }
                    if (LOGO_URL) {
                      icon.style.backgroundImage = `url("${LOGO_URL}")`;
                    }

                    function detectOS() {
                      const ua = navigator.userAgent || "";
                      if (/android/i.test(ua)) return "android";
                      if (/iPhone|iPad|iPod/i.test(ua)) return "ios";
                      return "other";
                    }

                    function installUrlFor(os) {
                      if (os === "android") return ANDROID_INSTALL_URL;
                      if (os === "ios") return IOS_INSTALL_URL;
                      return OTHER_INSTALL_URL;
                    }

                    function tryDetectInstalled(os) {
                      return new Promise((resolve) => {
                        if (os === "other") {
                          resolve(false);
                          return;
                        }
                        let hidden = false;
                        const onHide = () => { hidden = true; };
                        document.addEventListener("visibilitychange", onHide, { once: true });
                        const iframe = document.createElement("iframe");
                        iframe.style.display = "none";
                        iframe.src = APP_SCHEME_URL;
                        document.body.appendChild(iframe);
                        setTimeout(() => {
                          document.removeEventListener("visibilitychange", onHide);
                          try { document.body.removeChild(iframe); } catch (_) {}
                          resolve(hidden || document.hidden);
                        }, 900);
                      });
                    }

                    async function initButton() {
                      const os = detectOS();
                      const installed = await tryDetectInstalled(os);
                      openBtn.textContent = installed ? "立即秒開" : "安裝 BringBox";
                      openBtn.dataset.os = os;
                      openBtn.dataset.installed = installed ? "1" : "0";
                    }

                    openBtn.addEventListener("click", () => {
                      const os = openBtn.dataset.os || detectOS();
                      const installed = openBtn.dataset.installed === "1";
                      const installUrl = installUrlFor(os);
                      if (installed) {
                        window.location.href = APP_SCHEME_URL;
                        return;
                      }
                      const started = Date.now();
                      window.location.href = APP_SCHEME_URL;
                      setTimeout(() => {
                        if (Date.now() - started < 1800) {
                          window.location.href = installUrl;
                        }
                      }, 1200);
                    });

                    initButton();
                  </script>
                </body>
                </html>
                """.formatted(
                SHARE_PAGE_VERSION,
                safeTitle,
                safeTitle,
                safeStudioName,
                safePlayerCountText,
                safeSubtitle,
                escapeJs(appSchemeUrl),
                escapeJs(androidInstallUrl),
                escapeJs(iosInstallUrl),
                escapeJs(otherInstallUrl),
                escapeJs(bannerUrl),
                escapeJs(logoUrl)
        );
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeJs(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
