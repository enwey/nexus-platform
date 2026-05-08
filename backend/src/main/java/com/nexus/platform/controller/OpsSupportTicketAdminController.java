package com.nexus.platform.controller;

import com.nexus.platform.dto.OpsSupportTicketDto;
import com.nexus.platform.dto.OpsSupportTicketMessageDto;
import com.nexus.platform.dto.OpsSupportTicketStatusRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.SupportTicketMessageCreateRequest;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.OpsSupportTicketService;
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
@RequestMapping("/admin/ops/tickets")
@RequiredArgsConstructor
public class OpsSupportTicketAdminController {
    private final OpsSupportTicketService opsSupportTicketService;

    @GetMapping
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsSupportTicketDto>> listTickets() {
        return opsSupportTicketService.listOpsTickets();
    }

    @GetMapping("/{ticketId}/messages")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).AUDIT_LOG_READ)")
    public Result<List<OpsSupportTicketMessageDto>> listTicketMessages(@PathVariable Long ticketId) {
        return opsSupportTicketService.listOpsMessages(ticketId);
    }

    @PutMapping("/{ticketId}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsSupportTicketDto> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody OpsSupportTicketStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSupportTicketService.updateOpsTicketStatus(ticketId, request, currentUser, httpRequest.getRequestURI(), idempotencyKey);
    }

    @PostMapping("/{ticketId}/messages")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).ANDROID_ADMIN_WRITE)")
    public Result<OpsSupportTicketMessageDto> createTicketMessage(
            @PathVariable Long ticketId,
            @RequestBody SupportTicketMessageCreateRequest request,
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        return opsSupportTicketService.createOpsMessage(ticketId, request, currentUser, httpRequest.getRequestURI(), idempotencyKey);
    }
}
