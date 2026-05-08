package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsMessageNoticeDto;
import com.nexus.platform.dto.OpsMessageNoticeStatusRequest;
import com.nexus.platform.dto.OpsMessageNoticeUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsMessageNoticeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/notices")
@RequiredArgsConstructor
public class OpsMessageNoticeAdminController {
    private final OpsMessageNoticeService opsMessageNoticeService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsMessageNoticeDto>> listNotices() {
        return opsMessageNoticeService.listAdminNotices();
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsMessageNoticeDto> createNotice(
            @AuthenticationPrincipal User currentUser,
            @RequestBody OpsMessageNoticeUpsertRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsMessageNoticeService.createNotice(currentUser, request, httpRequest.getRequestURI(), idempotencyKey);
    }

    @PostMapping("/{noticeId}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsMessageNoticeDto> updateNotice(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody OpsMessageNoticeUpsertRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsMessageNoticeService.updateNotice(noticeId, currentUser, request, httpRequest.getRequestURI(), idempotencyKey);
    }

    @PostMapping("/{noticeId}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsMessageNoticeDto> updateNoticeStatus(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody OpsMessageNoticeStatusRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsMessageNoticeService.updateNoticeStatus(noticeId, currentUser, request, httpRequest.getRequestURI(), idempotencyKey);
    }
}
