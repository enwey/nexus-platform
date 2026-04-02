package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.WalletSummaryDto;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final AccountService accountService;

    @GetMapping("/summary")
    public Result<WalletSummaryDto> summary(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User user) {
        return accountService.getWalletSummary(user);
    }
}
