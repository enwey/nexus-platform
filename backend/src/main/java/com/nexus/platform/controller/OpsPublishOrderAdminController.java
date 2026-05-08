package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsPublishOrderCreateRequest;
import com.nexus.platform.dto.OpsPublishOrderDto;
import com.nexus.platform.dto.OpsPublishOrderPreviewDto;
import com.nexus.platform.dto.OpsPublishOrderStatusRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsPublishOrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/admin/ops/publish/orders")
@RequiredArgsConstructor
public class OpsPublishOrderAdminController {
    private final OpsPublishOrderService opsPublishOrderService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<List<OpsPublishOrderDto>> listOrders() {
        return opsPublishOrderService.listOrders();
    }

    @GetMapping("/{orderId}/preview")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_READ)")
    public Result<OpsPublishOrderPreviewDto> getOrderPreview(@PathVariable Long orderId) {
        return opsPublishOrderService.getOrderPreview(orderId);
    }

    @PostMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsPublishOrderDto> createOrder(
            @AuthenticationPrincipal User currentUser,
            @RequestBody OpsPublishOrderCreateRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsPublishOrderService.createOrder(currentUser, request, httpRequest.getRequestURI(), idempotencyKey);
    }

    @PostMapping("/{orderId}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsPublishOrderDto> updateStatus(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody OpsPublishOrderStatusRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsPublishOrderService.updateOrderStatus(orderId, currentUser, request, httpRequest.getRequestURI(), idempotencyKey);
    }

    @PostMapping("/execute-due")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<List<OpsPublishOrderDto>> executeDue(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsPublishOrderService.executeDueScheduledOrders(currentUser, httpRequest.getRequestURI(), idempotencyKey);
    }
}
