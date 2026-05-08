package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsRiskIncidentDto;
import com.nexus.platform.dto.OpsRiskIncidentRecordDto;
import com.nexus.platform.dto.OpsRiskIncidentStatusRequest;
import com.nexus.platform.dto.OpsRiskIncidentUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsRiskIncidentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/risk-incidents")
@RequiredArgsConstructor
public class OpsRiskIncidentAdminController {
    private final OpsRiskIncidentService opsRiskIncidentService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsRiskIncidentDto>> list() {
        return opsRiskIncidentService.list();
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsRiskIncidentRecordDto>> records(@PathVariable Long id) {
        return opsRiskIncidentService.listRecords(id);
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsRiskIncidentDto>> create(
            @RequestBody OpsRiskIncidentUpsertRequest request,
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return opsRiskIncidentService.create(request, currentUser, httpRequest.getRequestURI(), idempotencyKey);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsRiskIncidentDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody OpsRiskIncidentStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return opsRiskIncidentService.updateStatus(id, request, currentUser, httpRequest.getRequestURI(), idempotencyKey);
    }
}
