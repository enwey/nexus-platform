package com.nexus.platform.controller;

import com.nexus.platform.dto.AuthResponse;
import com.nexus.platform.dto.DeveloperApiKeyCreateRequest;
import com.nexus.platform.dto.DeveloperApiKeyCreateResponse;
import com.nexus.platform.dto.DeveloperApiKeyDto;
import com.nexus.platform.dto.DeveloperApiKeyRevokeRequest;
import com.nexus.platform.dto.DeveloperApiKeyRotateRequest;
import com.nexus.platform.dto.DeveloperDocArticleDto;
import com.nexus.platform.dto.DeveloperDocArticleUpsertRequest;
import com.nexus.platform.dto.DeveloperDocArticleVersionDto;
import com.nexus.platform.dto.DeveloperCertificationProfileDto;
import com.nexus.platform.dto.DeveloperCertificationReviewRecordDto;
import com.nexus.platform.dto.DeveloperCertificationUpsertRequest;
import com.nexus.platform.dto.DeveloperSupportTicketCreateRequest;
import com.nexus.platform.dto.DeveloperTeamMemberDto;
import com.nexus.platform.dto.DeveloperTeamMemberStatusRequest;
import com.nexus.platform.dto.DeveloperTeamMemberUpsertRequest;
import com.nexus.platform.dto.DeveloperWorkspaceAuditLogDto;
import com.nexus.platform.dto.DeviceSessionDto;
import com.nexus.platform.dto.OpsMessageNoticeDto;
import com.nexus.platform.dto.OpsSupportTicketDto;
import com.nexus.platform.dto.OpsSupportTicketMessageDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.SupportTicketMessageCreateRequest;
import com.nexus.platform.dto.UserProfileDetailDto;
import com.nexus.platform.dto.UserProfileDto;
import com.nexus.platform.dto.VerificationCodeResponse;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.AccountOpsService;
import com.nexus.platform.service.AccountService;
import com.nexus.platform.service.DeveloperCertificationService;
import com.nexus.platform.service.DeveloperDocumentationService;
import com.nexus.platform.service.OpsSupportTicketService;
import com.nexus.platform.service.DeveloperWorkspaceService;
import com.nexus.platform.service.OpsMessageNoticeService;
import com.nexus.platform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AccountService accountService;
    private final AccountOpsService accountOpsService;
    private final DeveloperCertificationService developerCertificationService;
    private final DeveloperDocumentationService developerDocumentationService;
    private final OpsSupportTicketService opsSupportTicketService;
    private final DeveloperWorkspaceService developerWorkspaceService;
    private final OpsMessageNoticeService opsMessageNoticeService;

    @PostMapping("/register")
    public Result<AuthResponse> register(@RequestBody RegisterRequest request) {
        return userService.register(request.email(), request.password(), request.code(), request.accountType());
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String loginId = firstNotBlank(request.email(), request.username());
        return userService.login(
                loginId,
                request.password(),
                extractClientIp(httpRequest),
                extractDeviceId(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return userService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_LOGOUT)")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token != null) {
            userService.logout(token);
        }
        return Result.success();
    }

    @PostMapping("/send-code")
    public Result<VerificationCodeResponse> sendCode(
            @RequestBody SendCodeRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        return accountOpsService.sendCode(
                request.email(),
                request.purpose(),
                new AccountOpsService.VerificationCodeIssueContext(
                        user,
                        extractClientIp(httpRequest),
                        httpRequest.getRequestURI(),
                        firstNotBlank(request.source(), httpRequest.getHeader("X-Client-Source")),
                        firstNotBlank(request.scene(), httpRequest.getHeader("X-Client-Scene")),
                        httpRequest.getHeader("User-Agent")
                )
        );
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        return accountOpsService.resetPassword(request.email(), request.code(), request.newPassword());
    }

    @PostMapping("/password/change")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<Void> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody ChangePasswordRequest request) {
        return accountOpsService.changePassword(user, request.email(), request.code(), request.newPassword());
    }

    @GetMapping("/devices")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeviceSessionDto>> devices(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        String currentDeviceId = token == null ? null : accountService.resolveDeviceIdFromToken(token);
        return accountOpsService.listDevices(user.getId(), currentDeviceId);
    }

    @PostMapping("/devices/{deviceId}/kick")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<Void> kickDevice(
            @AuthenticationPrincipal User user,
            @PathVariable String deviceId) {
        return accountOpsService.kickDevice(user.getId(), deviceId);
    }

    @PostMapping("/logout-all")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_LOGOUT)")
    public Result<Void> logoutAll(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        String currentDeviceId = token == null ? null : accountService.resolveDeviceIdFromToken(token);
        return accountOpsService.logoutAll(user.getId(), currentDeviceId);
    }

    @PostMapping("/terminate")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<Void> terminate(
            @AuthenticationPrincipal User user,
            @RequestBody TerminateRequest request) {
        return accountOpsService.terminateAccount(user, request.confirmText());
    }

    @GetMapping("/me")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<UserProfileDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return Result.success(UserProfileDto.from(user));
    }

    @GetMapping("/profile")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<UserProfileDetailDto> getProfile(
            @AuthenticationPrincipal User user) {
        return accountService.getProfile(user);
    }

    @PostMapping("/profile")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<UserProfileDetailDto> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequest request) {
        return accountService.updateProfile(
                user,
                request.displayName(),
                request.avatarUrl(),
                request.languageTag(),
                request.email(),
                request.phone()
        );
    }

    @GetMapping("/developer-certification")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<DeveloperCertificationProfileDto> getMyDeveloperCertification(@AuthenticationPrincipal User user) {
        return developerCertificationService.getMyProfile(user);
    }

    @GetMapping("/developer-certification/reviews")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeveloperCertificationReviewRecordDto>> getMyDeveloperCertificationReviews(@AuthenticationPrincipal User user) {
        return developerCertificationService.getMyReviewRecords(user);
    }

    @PostMapping("/developer-certification")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperCertificationProfileDto> saveMyDeveloperCertification(
            @AuthenticationPrincipal User user,
            @RequestBody DeveloperCertificationUpsertRequest request,
            HttpServletRequest httpRequest
    ) {
        return developerCertificationService.saveMyProfile(user, request, httpRequest.getRequestURI());
    }

    @GetMapping("/tickets")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<OpsSupportTicketDto>> listMyTickets(@AuthenticationPrincipal User user) {
        return opsSupportTicketService.listDeveloperTickets(user);
    }

    @PostMapping("/tickets")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<OpsSupportTicketDto> createMyTicket(
            @AuthenticationPrincipal User user,
            @RequestBody DeveloperSupportTicketCreateRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest
    ) {
        return opsSupportTicketService.createDeveloperTicket(user, request, httpRequest.getRequestURI(), idempotencyKey);
    }

    @GetMapping("/tickets/{ticketId}/messages")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<OpsSupportTicketMessageDto>> listMyTicketMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long ticketId
    ) {
        return opsSupportTicketService.listDeveloperMessages(user, ticketId);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<OpsSupportTicketMessageDto> createMyTicketMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long ticketId,
            @RequestBody SupportTicketMessageCreateRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest
    ) {
        return opsSupportTicketService.createDeveloperMessage(user, ticketId, request, httpRequest.getRequestURI(), idempotencyKey);
    }

    @GetMapping("/team-members")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeveloperTeamMemberDto>> listTeamMembers(@AuthenticationPrincipal User user) {
        return developerWorkspaceService.listTeamMembers(user);
    }

    @PostMapping("/team-members")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperTeamMemberDto> createTeamMember(
            @AuthenticationPrincipal User user,
            @RequestBody DeveloperTeamMemberUpsertRequest request,
            HttpServletRequest httpRequest
    ) {
        return developerWorkspaceService.createTeamMember(user, request, httpRequest.getRequestURI());
    }

    @PostMapping("/team-members/{memberId}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperTeamMemberDto> updateTeamMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long memberId,
            @RequestBody DeveloperTeamMemberUpsertRequest request,
            HttpServletRequest httpRequest
    ) {
        return developerWorkspaceService.updateTeamMember(user, memberId, request, httpRequest.getRequestURI());
    }

    @PostMapping("/team-members/{memberId}/status")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperTeamMemberDto> updateTeamMemberStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long memberId,
            @RequestBody DeveloperTeamMemberStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        return developerWorkspaceService.updateTeamMemberStatus(user, memberId, request, httpRequest.getRequestURI());
    }

    @GetMapping("/api-keys")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeveloperApiKeyDto>> listApiKeys(@AuthenticationPrincipal User user) {
        return developerWorkspaceService.listApiKeys(user);
    }

    @GetMapping("/workspace-audit-logs")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeveloperWorkspaceAuditLogDto>> listWorkspaceAuditLogs(@AuthenticationPrincipal User user) {
        return developerWorkspaceService.listWorkspaceAuditLogs(user);
    }

    @GetMapping("/notices")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<OpsMessageNoticeDto>> listMyNotices(@AuthenticationPrincipal User user) {
        return opsMessageNoticeService.listDeveloperNotices(user);
    }

    @PostMapping("/api-keys")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperApiKeyCreateResponse> createApiKey(
            @AuthenticationPrincipal User user,
            @RequestBody DeveloperApiKeyCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        return developerWorkspaceService.createApiKey(user, request, httpRequest.getRequestURI());
    }

    @PostMapping("/api-keys/{keyId}/revoke")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperApiKeyDto> revokeApiKey(
            @AuthenticationPrincipal User user,
            @PathVariable Long keyId,
            @RequestBody DeveloperApiKeyRevokeRequest request,
            HttpServletRequest httpRequest
    ) {
        return developerWorkspaceService.revokeApiKey(user, keyId, request, httpRequest.getRequestURI());
    }

    @PostMapping("/api-keys/{keyId}/rotate")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperApiKeyCreateResponse> rotateApiKey(
            @AuthenticationPrincipal User user,
            @PathVariable Long keyId,
            @RequestBody DeveloperApiKeyRotateRequest request,
            HttpServletRequest httpRequest
    ) {
        return developerWorkspaceService.rotateApiKey(user, keyId, request, httpRequest.getRequestURI());
    }

    @GetMapping("/docs")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeveloperDocArticleDto>> listMyDocs(
            @AuthenticationPrincipal User user,
            @org.springframework.web.bind.annotation.RequestParam(value = "docType", required = false) String docType) {
        return developerDocumentationService.listArticles(user, docType);
    }

    @GetMapping("/docs/{articleId}/versions")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_READ)")
    public Result<List<DeveloperDocArticleVersionDto>> listMyDocVersions(
            @AuthenticationPrincipal User user,
            @PathVariable Long articleId) {
        return developerDocumentationService.listVersions(user, articleId);
    }

    @PostMapping("/docs")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperDocArticleDto> createMyDoc(
            @AuthenticationPrincipal User user,
            @RequestBody DeveloperDocArticleUpsertRequest request,
            HttpServletRequest httpRequest) {
        return developerDocumentationService.createArticle(user, request, httpRequest.getRequestURI());
    }

    @PostMapping("/docs/{articleId}")
    @PreAuthorize("@rolePermissionService.hasPermission(authentication, T(com.nexus.platform.security.Permission).USER_PROFILE_WRITE)")
    public Result<DeveloperDocArticleDto> updateMyDoc(
            @AuthenticationPrincipal User user,
            @PathVariable Long articleId,
            @RequestBody DeveloperDocArticleUpsertRequest request,
            HttpServletRequest httpRequest) {
        return developerDocumentationService.updateArticle(user, articleId, request, httpRequest.getRequestURI());
    }

    @GetMapping("/{id}")
    public Result<UserProfileDto> getUser(@PathVariable Long id) {
        UserProfileDto user = userService.findById(id);
        if (user == null) {
            return Result.error("User not found");
        }
        return Result.success(user);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private String extractDeviceId(HttpServletRequest request) {
        String explicitDeviceId = firstNotBlank(
                request.getHeader("X-Device-Id"),
                request.getHeader("X-Client-Device-Id")
        );
        if (explicitDeviceId != null) {
            return explicitDeviceId;
        }
        String userAgent = firstNotBlank(request.getHeader("User-Agent"), "unknown-agent");
        String clientIp = firstNotBlank(extractClientIp(request), "unknown-ip");
        return "fp_" + sha256Hex(userAgent + "|" + clientIp).substring(0, 24);
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
        }
    }
}

record RegisterRequest(String email, String password, String code, String accountType) {}
record LoginRequest(String email, String username, String password) {}
record RefreshRequest(String refreshToken) {}
record SendCodeRequest(String email, String purpose, String source, String scene) {}
record ResetPasswordRequest(String email, String code, String newPassword) {}
record ChangePasswordRequest(String email, String code, String newPassword) {}
record TerminateRequest(String confirmText) {}
record UpdateProfileRequest(
        String displayName,
        String avatarUrl,
        String languageTag,
        String email,
        String phone
) {}
