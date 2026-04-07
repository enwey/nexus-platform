package com.nexus.platform.controller;

import com.nexus.platform.dto.ReferralRecordsResponse;
import com.nexus.platform.dto.ReferralSummaryDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AccountOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/referral")
@RequiredArgsConstructor
@PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_WALLET_READ)")
public class ReferralController {
    private final AccountOpsService accountOpsService;

    @GetMapping("/summary")
    public Result<ReferralSummaryDto> summary(
            @AuthenticationPrincipal User user) {
        return accountOpsService.getReferralSummary(user.getId());
    }

    @GetMapping("/records")
    public Result<ReferralRecordsResponse> records(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "20") int limit) {
        return accountOpsService.getReferralRecords(user.getId(), limit);
    }

    @PostMapping("/share")
    public Result<Void> share(
            @AuthenticationPrincipal User user,
            @RequestBody ReferralShareRequest request) {
        return accountOpsService.markReferralShared(user.getId(), request.channel());
    }
}

record ReferralShareRequest(String channel) {}
