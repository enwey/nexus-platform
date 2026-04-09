package com.nexus.platform.controller;

import com.nexus.platform.config.LegalContentProperties;
import com.nexus.platform.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/legal")
@RequiredArgsConstructor
public class LegalContentController {
    private final LegalContentProperties legalContentProperties;

    @GetMapping("/config")
    public Result<LegalConfigResponse> config(HttpServletRequest request) {
        String base = resolveBaseUrl(request);
        return Result.success(new LegalConfigResponse(
                base + "/public/legal/terms",
                base + "/public/legal/privacy"
        ));
    }

    @GetMapping(value = "/terms", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> terms() {
        return ResponseEntity.ok(renderHtml(legalContentProperties.getTermsTitle(), legalContentProperties.getTermsHtml()));
    }

    @GetMapping(value = "/privacy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> privacy() {
        return ResponseEntity.ok(renderHtml(legalContentProperties.getPrivacyTitle(), legalContentProperties.getPrivacyHtml()));
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean appendPort = ("http".equalsIgnoreCase(scheme) && port != 80)
                    || ("https".equalsIgnoreCase(scheme) && port != 443);
            if (appendPort) {
                host = host + ":" + port;
            }
        }
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        return scheme + "://" + host + contextPath;
    }

    private String renderHtml(String title, String bodyHtml) {
        String safeTitle = title == null || title.isBlank() ? "Nexus" : title;
        String safeBody = bodyHtml == null || bodyHtml.isBlank()
                ? "<p>Content is empty.</p>"
                : bodyHtml;
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                  <style>
                    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #10131a; color: #f5f7ff; }
                    main { max-width: 920px; margin: 0 auto; padding: 24px 20px 48px; line-height: 1.7; }
                    h1 { font-size: 24px; margin: 0 0 16px; }
                    p { margin: 0 0 12px; color: #d6dbf5; }
                    a { color: #6cb7ff; }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>%s</h1>
                    %s
                  </main>
                </body>
                </html>
                """.formatted(safeTitle, safeTitle, safeBody);
    }
}

record LegalConfigResponse(String termsUrl, String privacyUrl) {}
