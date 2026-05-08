package com.nexus.platform.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.OpsMessageNoticeDto;
import com.nexus.platform.dto.OpsMessageNoticeStatusRequest;
import com.nexus.platform.dto.OpsMessageNoticeUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsMessageNotice;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsMessageNoticeRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpsMessageNoticeService {
    private static final Set<String> AUDIENCE_ROLES = Set.of("ALL", "DEVELOPER", "PLAYER");
    private static final Set<String> CATEGORIES = Set.of("SYSTEM", "REVIEW", "PUBLISH", "RISK");
    private static final Set<String> DELIVERY_STATUSES = Set.of("DRAFT", "PUBLISHED", "PAUSED");

    private final OpsMessageNoticeRepository opsMessageNoticeRepository;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public Result<List<OpsMessageNoticeDto>> listAdminNotices() {
        return Result.success(
                opsMessageNoticeRepository.findAllByOrderByUpdatedAtDesc()
                        .stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    public Result<List<OpsMessageNoticeDto>> listDeveloperNotices(User currentUser) {
        LocalDateTime now = LocalDateTime.now();
        return Result.success(
                opsMessageNoticeRepository.findAllByOrderByUpdatedAtDesc()
                        .stream()
                        .filter(item -> isVisibleToDeveloper(item, currentUser, now))
                        .map(this::toDto)
                        .toList()
        );
    }

    @Transactional
    public Result<OpsMessageNoticeDto> createNotice(
            User currentUser,
            OpsMessageNoticeUpsertRequest request,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsMessageNoticeDto.class);
        return idempotencyService.execute(
                "OPS_NOTICE_CREATE",
                idempotencyKey,
                request,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> createNoticeInternal(currentUser, request, requestUri)
        );
    }

    private Result<OpsMessageNoticeDto> createNoticeInternal(
            User currentUser,
            OpsMessageNoticeUpsertRequest request,
            String requestUri
    ) {
        ValidationResult validation = validateRequest(request);
        if (!validation.success()) {
            auditLogService.logOpsAudit("OPS_NOTICE_CREATE", currentUser, null, "NOTICE", false, validation.message(), requestUri);
            return Result.error(validation.message());
        }
        OpsMessageNotice notice = new OpsMessageNotice();
        applyChanges(notice, validation, currentUser.getId());
        OpsMessageNotice saved = opsMessageNoticeRepository.save(notice);
        auditLogService.logOpsAudit("OPS_NOTICE_CREATE", currentUser, null, "NOTICE", true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsMessageNoticeDto> updateNotice(
            Long noticeId,
            User currentUser,
            OpsMessageNoticeUpsertRequest request,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsMessageNoticeDto.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("noticeId", noticeId);
        payload.put("request", request);
        return idempotencyService.execute(
                "OPS_NOTICE_UPDATE",
                idempotencyKey,
                payload,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> updateNoticeInternal(noticeId, currentUser, request, requestUri)
        );
    }

    private Result<OpsMessageNoticeDto> updateNoticeInternal(
            Long noticeId,
            User currentUser,
            OpsMessageNoticeUpsertRequest request,
            String requestUri
    ) {
        OpsMessageNotice notice = opsMessageNoticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            auditLogService.logOpsAudit("OPS_NOTICE_UPDATE", currentUser, null, "NOTICE", false, "Notice not found", requestUri);
            return Result.error("Notice not found");
        }
        ValidationResult validation = validateRequest(request);
        if (!validation.success()) {
            auditLogService.logOpsAudit("OPS_NOTICE_UPDATE", currentUser, null, "NOTICE", false, validation.message(), requestUri);
            return Result.error(validation.message());
        }
        applyChanges(notice, validation, notice.getCreatedBy());
        OpsMessageNotice saved = opsMessageNoticeRepository.save(notice);
        auditLogService.logOpsAudit("OPS_NOTICE_UPDATE", currentUser, null, "NOTICE", true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsMessageNoticeDto> updateNoticeStatus(
            Long noticeId,
            User currentUser,
            OpsMessageNoticeStatusRequest request,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, OpsMessageNoticeDto.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("noticeId", noticeId);
        payload.put("request", request);
        return idempotencyService.execute(
                "OPS_NOTICE_STATUS_UPDATE",
                idempotencyKey,
                payload,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> updateNoticeStatusInternal(noticeId, currentUser, request, requestUri)
        );
    }

    private Result<OpsMessageNoticeDto> updateNoticeStatusInternal(
            Long noticeId,
            User currentUser,
            OpsMessageNoticeStatusRequest request,
            String requestUri
    ) {
        OpsMessageNotice notice = opsMessageNoticeRepository.findById(noticeId).orElse(null);
        if (notice == null) {
            auditLogService.logOpsAudit("OPS_NOTICE_STATUS_UPDATE", currentUser, null, "NOTICE", false, "Notice not found", requestUri);
            return Result.error("Notice not found");
        }
        String status = normalizeEnum(request == null ? null : request.deliveryStatus(), DELIVERY_STATUSES);
        if (status == null) {
            auditLogService.logOpsAudit("OPS_NOTICE_STATUS_UPDATE", currentUser, null, "NOTICE", false, "Invalid notice status", requestUri);
            return Result.error("Invalid notice status");
        }
        if ("PUBLISHED".equals(status) && notice.getStartAt() != null && notice.getEndAt() != null && notice.getStartAt().isAfter(notice.getEndAt())) {
            auditLogService.logOpsAudit("OPS_NOTICE_STATUS_UPDATE", currentUser, null, "NOTICE", false, "Invalid notice schedule", requestUri);
            return Result.error("Invalid notice schedule");
        }
        notice.setDeliveryStatus(status);
        OpsMessageNotice saved = opsMessageNoticeRepository.save(notice);
        auditLogService.logOpsAudit("OPS_NOTICE_STATUS_UPDATE", currentUser, null, "NOTICE", true, status, requestUri);
        return Result.success(toDto(saved));
    }

    private boolean isVisibleToDeveloper(OpsMessageNotice item, User currentUser, LocalDateTime now) {
        if (!"PUBLISHED".equalsIgnoreCase(item.getDeliveryStatus())) {
            return false;
        }
        if (!"ALL".equalsIgnoreCase(item.getAudienceRole()) && !"DEVELOPER".equalsIgnoreCase(item.getAudienceRole())) {
            return false;
        }
        if (item.getTargetUserId() != null && !item.getTargetUserId().equals(currentUser.getId())) {
            return false;
        }
        if (item.getStartAt() != null && item.getStartAt().isAfter(now)) {
            return false;
        }
        return item.getEndAt() == null || !item.getEndAt().isBefore(now);
    }

    private void applyChanges(OpsMessageNotice notice, ValidationResult validation, Long createdBy) {
        notice.setAudienceRole(validation.audienceRole());
        notice.setTargetUserId(validation.targetUserId());
        notice.setCategory(validation.category());
        notice.setDeliveryStatus(validation.deliveryStatus());
        notice.setTitle(validation.title());
        notice.setBody(validation.body());
        notice.setActionUrl(validation.actionUrl());
        notice.setStartAt(validation.startAt());
        notice.setEndAt(validation.endAt());
        notice.setCreatedBy(createdBy);
    }

    private ValidationResult validateRequest(OpsMessageNoticeUpsertRequest request) {
        String audienceRole = normalizeEnum(request == null ? null : request.audienceRole(), AUDIENCE_ROLES);
        if (audienceRole == null) {
            return ValidationResult.error("Invalid audience role");
        }
        String category = normalizeEnum(request == null ? null : request.category(), CATEGORIES);
        if (category == null) {
            return ValidationResult.error("Invalid notice category");
        }
        String deliveryStatus = normalizeEnum(request == null ? null : request.deliveryStatus(), DELIVERY_STATUSES);
        if (deliveryStatus == null) {
            deliveryStatus = "DRAFT";
        }
        String title = trim(request == null ? null : request.title(), 128);
        if (title == null || title.length() < 4) {
            return ValidationResult.error("Title must be at least 4 characters");
        }
        String body = trim(request == null ? null : request.body(), 2000);
        if (body == null || body.length() < 8) {
            return ValidationResult.error("Body must be at least 8 characters");
        }
        String actionUrl = trim(request == null ? null : request.actionUrl(), 512);
        if (actionUrl != null && !(actionUrl.startsWith("/") || actionUrl.startsWith("http://") || actionUrl.startsWith("https://"))) {
            return ValidationResult.error("Invalid action URL");
        }
        Long targetUserId = request == null ? null : request.targetUserId();
        if (!"DEVELOPER".equals(audienceRole) && targetUserId != null) {
            return ValidationResult.error("Target user is only supported for developer notices");
        }
        LocalDateTime startAt = request == null ? null : request.startAt();
        LocalDateTime endAt = request == null ? null : request.endAt();
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            return ValidationResult.error("Start time cannot be later than end time");
        }
        return ValidationResult.success(audienceRole, targetUserId, category, deliveryStatus, title, body, actionUrl, startAt, endAt);
    }

    private OpsMessageNoticeDto toDto(OpsMessageNotice notice) {
        return new OpsMessageNoticeDto(
                notice.getId(),
                notice.getAudienceRole(),
                notice.getTargetUserId(),
                notice.getCategory(),
                notice.getDeliveryStatus(),
                effectiveStatus(notice),
                notice.getTitle(),
                notice.getBody(),
                notice.getActionUrl(),
                notice.getStartAt(),
                notice.getEndAt(),
                notice.getCreatedBy(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private String effectiveStatus(OpsMessageNotice notice) {
        if (!"PUBLISHED".equalsIgnoreCase(notice.getDeliveryStatus())) {
            return notice.getDeliveryStatus();
        }
        LocalDateTime now = LocalDateTime.now();
        if (notice.getStartAt() != null && notice.getStartAt().isAfter(now)) {
            return "SCHEDULED";
        }
        if (notice.getEndAt() != null && notice.getEndAt().isBefore(now)) {
            return "EXPIRED";
        }
        return "LIVE";
    }

    private String normalizeEnum(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private record ValidationResult(
            boolean success,
            String message,
            String audienceRole,
            Long targetUserId,
            String category,
            String deliveryStatus,
            String title,
            String body,
            String actionUrl,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        static ValidationResult error(String message) {
            return new ValidationResult(false, message, null, null, null, null, null, null, null, null, null);
        }

        static ValidationResult success(
                String audienceRole,
                Long targetUserId,
                String category,
                String deliveryStatus,
                String title,
                String body,
                String actionUrl,
                LocalDateTime startAt,
                LocalDateTime endAt
        ) {
            return new ValidationResult(true, null, audienceRole, targetUserId, category, deliveryStatus, title, body, actionUrl, startAt, endAt);
        }
    }
}
