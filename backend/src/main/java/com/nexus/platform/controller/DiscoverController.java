package com.nexus.platform.controller;

import com.nexus.platform.dto.DiscoverFeedItem;
import com.nexus.platform.dto.DiscoverHomeResponse;
import com.nexus.platform.dto.DiscoverCommunityItem;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.DiscoverService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/discover")
@RequiredArgsConstructor
public class DiscoverController {
    private final DiscoverService discoverService;

    @GetMapping("/feed")
    public Result<List<DiscoverFeedItem>> feed(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String category,
            HttpServletRequest request) {
        Result<List<DiscoverFeedItem>> result = discoverService.getFeed(limit, category);
        if (result.getData() != null) {
            result = Result.success(result.getData().stream()
                    .map(item -> item.withDownloadUrl(rewriteLoopbackDownloadUrl(item.downloadUrl(), request)))
                    .toList());
        }
        return result;
    }

    @GetMapping("/home")
    public Result<DiscoverHomeResponse> home(@RequestParam(defaultValue = "20") int limit, HttpServletRequest request) {
        Result<DiscoverHomeResponse> result = discoverService.getHome(limit);
        DiscoverHomeResponse data = result.getData();
        if (data == null) {
            return result;
        }
        return Result.success(new DiscoverHomeResponse(
                data.hero(),
                data.libraryTopBanner(),
                data.categories(),
                rewriteFeedItems(data.rankedGames(), request),
                rewriteFeedItems(data.newbieMustPlay(), request),
                rewriteFeedItems(data.everyonePlaying(), request)
        ));
    }

    @GetMapping("/community")
    public Result<List<DiscoverCommunityItem>> community(@RequestParam(defaultValue = "10") int limit) {
        return discoverService.getCommunity(limit);
    }

    private List<DiscoverFeedItem> rewriteFeedItems(List<DiscoverFeedItem> items, HttpServletRequest request) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> item.withDownloadUrl(rewriteLoopbackDownloadUrl(item.downloadUrl(), request)))
                .toList();
    }

    private String rewriteLoopbackDownloadUrl(String rawUrl, HttpServletRequest request) {
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
