package com.nexus.platform.controller;

import com.nexus.platform.dto.AuditLogItemDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.service.AuditLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
public class AuditController {
    private final AuditLogService auditLogService;

    @GetMapping("/logs")
    public Result<List<AuditLogItemDto>> getAuditLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String targetAppId
    ) {
        return Result.success(auditLogService.searchLogs(action, success, operatorId, targetAppId, limit));
    }
}
