package com.nexus.platform.controller;

import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.AuditLog;
import com.nexus.platform.service.AuditLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditLogService auditLogService;

    @GetMapping("/logs")
    public Result<List<AuditLog>> getAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(auditLogService.getRecentLogs(limit));
    }
}

