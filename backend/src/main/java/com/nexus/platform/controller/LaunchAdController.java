package com.nexus.platform.controller;

import com.nexus.platform.dto.LaunchAdDtos.LaunchAdPublicResponse;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.LaunchAdService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/launch/ad")
@RequiredArgsConstructor
public class LaunchAdController {
    private final LaunchAdService launchAdService;

    @GetMapping("/current")
    public Result<LaunchAdPublicResponse> current(HttpServletRequest request) {
        Result<LaunchAdPublicResponse> result = launchAdService.currentActive();
        LaunchAdPublicResponse data = result.getData();
        if (data == null) {
            return result;
        }
        return Result.success(new LaunchAdPublicResponse(
                data.id(),
                data.code(),
                rewriteLoopbackUrl(data.imageUrl(), request),
                data.targetUrl(),
                data.imageVersion(),
                data.displaySeconds(),
                data.status(),
                data.startAt(),
                data.endAt(),
                data.sponsorZhCn(),
                data.sponsorZhTw(),
                data.sponsorEn(),
                data.titleZhCn(),
                data.titleZhTw(),
                data.titleEn(),
                data.descriptionZhCn(),
                data.descriptionZhTw(),
                data.descriptionEn(),
                data.ctaZhCn(),
                data.ctaZhTw(),
                data.ctaEn(),
                data.footerZhCn(),
                data.footerZhTw(),
                data.footerEn(),
                data.effectiveState(),
                data.updatedAt()
        ));
    }

    private String rewriteLoopbackUrl(String rawUrl, HttpServletRequest request) {
        if (rawUrl == null || rawUrl.isBlank() || request == null) {
            return rawUrl;
        }
        try {
            URI uri = URI.create(rawUrl);
            if (!isLoopbackHost(uri.getHost())) {
                return rawUrl;
            }
            return resolveBaseUrl(request) + uri.getPath()
                    + (uri.getQuery() == null || uri.getQuery().isBlank() ? "" : "?" + uri.getQuery());
        } catch (Exception ignored) {
            return rawUrl;
        }
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String scheme = firstNonBlank(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = firstNonBlank(request.getHeader("X-Forwarded-Host"), null);
        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean appendPort = ("http".equalsIgnoreCase(scheme) && port != 80)
                    || ("https".equalsIgnoreCase(scheme) && port != 443);
            if (appendPort) {
                host = host + ":" + port;
            }
        }
        return scheme + "://" + host;
    }

    private boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
