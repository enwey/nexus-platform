package com.nexus.platform.controller;

import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.WalletSummaryDto;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_WALLET_READ)")
public class WalletController {
    private final AccountService accountService;

    @GetMapping("/summary")
    public Result<WalletSummaryDto> summary(
            @AuthenticationPrincipal User user) {
        return accountService.getWalletSummary(user);
    }
}
