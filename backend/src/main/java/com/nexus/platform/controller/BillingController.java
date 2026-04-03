package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.BillingDetailDto;
import com.nexus.platform.dto.BillingRecordDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AccountOpsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet/billing")
@RequiredArgsConstructor
public class BillingController {
    private final AccountOpsService accountOpsService;

    @GetMapping("/list")
    public Result<List<BillingRecordDto>> list(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User user,
            @RequestParam(defaultValue = "30") int limit) {
        return accountOpsService.getBillingList(user.getId(), limit);
    }

    @GetMapping("/{id}")
    public Result<BillingDetailDto> detail(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User user,
            @PathVariable Long id) {
        return accountOpsService.getBillingDetail(user.getId(), id);
    }

    @GetMapping("/{id}/receipt")
    public Result<String> receipt(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User user,
            @PathVariable Long id) {
        return Result.success("/wallet/billing/" + id + "/receipt");
    }
}
