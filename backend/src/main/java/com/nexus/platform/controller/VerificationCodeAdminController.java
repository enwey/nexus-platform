package com.nexus.platform.controller;

import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.VerificationCodeLog;
import com.nexus.platform.service.AccountOpsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ops/verification-codes")
@RequiredArgsConstructor
@PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
public class VerificationCodeAdminController {
    private final AccountOpsService accountOpsService;

    @GetMapping
    public Result<List<VerificationCodeLog>> getVerificationCodeLogs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String source
    ) {
        return accountOpsService.listVerificationCodeLogs(email, purpose, source, limit);
    }
}
