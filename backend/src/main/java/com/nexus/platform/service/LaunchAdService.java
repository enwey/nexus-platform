package com.nexus.platform.service;

import com.nexus.platform.dto.LaunchAdDtos.LaunchAdAdminItem;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdAdminListResponse;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdPublicResponse;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.LaunchAd;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.LaunchAdRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LaunchAdService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final LaunchAdRepository launchAdRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Result<LaunchAdAdminListResponse> listAdminAds() {
        LaunchAdPublicResponse active = currentActive().getData();
        List<LaunchAdAdminItem> items = launchAdRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toAdminItem)
                .toList();
        return Result.success(new LaunchAdAdminListResponse(active, items));
    }

    @Transactional
    public Result<LaunchAdAdminItem> create(LaunchAdUpsertRequest request, User currentUser) {
        String validationError = validate(request);
        if (validationError != null) {
            auditLogService.logOpsAudit("LAUNCH_AD_CREATE", currentUser, null, null, false, validationError, "/admin/launch-ads");
            return Result.error(validationError);
        }
        LaunchAd entity = new LaunchAd();
        applyRequest(entity, request, currentUser, true);
        LaunchAd saved = launchAdRepository.save(entity);
        if (STATUS_ACTIVE.equals(saved.getStatus())) {
            deactivateOthers(saved.getId());
        }
        auditLogService.logOpsAudit("LAUNCH_AD_CREATE", currentUser, null, null, true, "Launch ad created: " + saved.getCode(), "/admin/launch-ads");
        return Result.success(toAdminItem(saved));
    }

    @Transactional
    public Result<LaunchAdAdminItem> update(Long id, LaunchAdUpsertRequest request, User currentUser) {
        String validationError = validate(request);
        if (validationError != null) {
            auditLogService.logOpsAudit("LAUNCH_AD_UPDATE", currentUser, null, null, false, validationError, "/admin/launch-ads/" + id);
            return Result.error(validationError);
        }
        LaunchAd entity = launchAdRepository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("LAUNCH_AD_UPDATE", currentUser, null, null, false, "Launch ad not found", "/admin/launch-ads/" + id);
            return Result.error("Launch ad not found");
        }
        applyRequest(entity, request, currentUser, false);
        LaunchAd saved = launchAdRepository.save(entity);
        if (STATUS_ACTIVE.equals(saved.getStatus())) {
            deactivateOthers(saved.getId());
        }
        auditLogService.logOpsAudit("LAUNCH_AD_UPDATE", currentUser, null, null, true, "Launch ad updated: " + saved.getCode(), "/admin/launch-ads/" + id);
        return Result.success(toAdminItem(saved));
    }

    @Transactional
    public Result<LaunchAdAdminItem> activate(Long id, User currentUser) {
        LaunchAd entity = launchAdRepository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("LAUNCH_AD_ACTIVATE", currentUser, null, null, false, "Launch ad not found", "/admin/launch-ads/" + id + "/activate");
            return Result.error("Launch ad not found");
        }
        if (!isValidSchedule(entity.getStartAt(), entity.getEndAt())) {
            auditLogService.logOpsAudit("LAUNCH_AD_ACTIVATE", currentUser, null, null, false, "Launch ad schedule is invalid", "/admin/launch-ads/" + id + "/activate");
            return Result.error("Launch ad schedule is invalid");
        }
        deactivateOthers(id);
        entity.setStatus(STATUS_ACTIVE);
        entity.setUpdatedBy(resolveUsername(currentUser));
        LaunchAd saved = launchAdRepository.save(entity);
        auditLogService.logOpsAudit("LAUNCH_AD_ACTIVATE", currentUser, null, null, true, "Launch ad activated: " + saved.getCode(), "/admin/launch-ads/" + id + "/activate");
        return Result.success(toAdminItem(saved));
    }

    @Transactional
    public Result<LaunchAdAdminItem> deactivate(Long id, User currentUser) {
        LaunchAd entity = launchAdRepository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("LAUNCH_AD_DEACTIVATE", currentUser, null, null, false, "Launch ad not found", "/admin/launch-ads/" + id + "/deactivate");
            return Result.error("Launch ad not found");
        }
        entity.setStatus(STATUS_INACTIVE);
        entity.setUpdatedBy(resolveUsername(currentUser));
        LaunchAd saved = launchAdRepository.save(entity);
        auditLogService.logOpsAudit("LAUNCH_AD_DEACTIVATE", currentUser, null, null, true, "Launch ad deactivated: " + saved.getCode(), "/admin/launch-ads/" + id + "/deactivate");
        return Result.success(toAdminItem(saved));
    }

    @Transactional
    public Result<LaunchAdPublicResponse> currentActive() {
        LocalDateTime now = LocalDateTime.now();
        LaunchAd entity = launchAdRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> STATUS_ACTIVE.equals(item.getStatus()))
                .filter(item -> isWithinSchedule(item, now))
                .findFirst()
                .orElse(null);
        return Result.success(entity == null ? null : toPublicResponse(entity));
    }

    private void deactivateOthers(Long activeId) {
        List<LaunchAd> activeRows = launchAdRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> STATUS_ACTIVE.equals(item.getStatus()))
                .filter(item -> item.getId() != null && item.getId().equals(activeId) == false)
                .toList();
        for (LaunchAd row : activeRows) {
            row.setStatus(STATUS_INACTIVE);
            launchAdRepository.save(row);
        }
    }

    private void applyRequest(LaunchAd entity, LaunchAdUpsertRequest request, User currentUser, boolean creating) {
        String nextImageUrl = request.imageUrl().trim();
        boolean imageChanged = creating || entity.getImageUrl() == null || entity.getImageUrl().equals(nextImageUrl) == false;
        entity.setImageUrl(nextImageUrl);
        entity.setTargetUrl(request.targetUrl().trim());
        entity.setDisplaySeconds(request.displaySeconds() == null ? 4 : Math.max(1, request.displaySeconds()));
        entity.setSponsorZhCn(trimToEmpty(request.sponsorZhCn()));
        entity.setSponsorZhTw(trimToEmpty(request.sponsorZhTw()));
        entity.setSponsorEn(trimToEmpty(request.sponsorEn()));
        entity.setTitleZhCn(trimToEmpty(request.titleZhCn()));
        entity.setTitleZhTw(trimToEmpty(request.titleZhTw()));
        entity.setTitleEn(trimToEmpty(request.titleEn()));
        entity.setDescriptionZhCn(trimToEmpty(request.descriptionZhCn()));
        entity.setDescriptionZhTw(trimToEmpty(request.descriptionZhTw()));
        entity.setDescriptionEn(trimToEmpty(request.descriptionEn()));
        entity.setCtaZhCn(trimToEmpty(request.ctaZhCn()));
        entity.setCtaZhTw(trimToEmpty(request.ctaZhTw()));
        entity.setCtaEn(trimToEmpty(request.ctaEn()));
        entity.setFooterZhCn(trimToEmpty(request.footerZhCn()));
        entity.setFooterZhTw(trimToEmpty(request.footerZhTw()));
        entity.setFooterEn(trimToEmpty(request.footerEn()));
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setUpdatedBy(resolveUsername(currentUser));

        String requestedStatus = normalizeStatus(request.status());
        if (requestedStatus != null) {
            entity.setStatus(requestedStatus);
        } else if (creating && (entity.getStatus() == null || entity.getStatus().isBlank())) {
            entity.setStatus(STATUS_DRAFT);
        }

        if (request.imageVersion() != null && request.imageVersion().isBlank() == false) {
            entity.setImageVersion(request.imageVersion().trim());
        } else if (creating || imageChanged) {
            entity.setImageVersion(String.valueOf(System.currentTimeMillis()));
        }
    }

    private String validate(LaunchAdUpsertRequest request) {
        if (request == null) {
            return "Request body is required";
        }
        if (request.imageUrl() == null || request.imageUrl().isBlank()) {
            return "imageUrl is required";
        }
        if (request.imageUrl().trim().length() > 1024) {
            return "imageUrl is too long";
        }
        if (request.targetUrl() == null || request.targetUrl().isBlank()) {
            return "targetUrl is required";
        }
        if (request.targetUrl().trim().length() > 1024) {
            return "targetUrl is too long";
        }
        if (!isValidUrl(request.imageUrl()) || !isValidUrl(request.targetUrl())) {
            return "imageUrl or targetUrl is invalid";
        }
        if (request.titleZhCn() == null || request.titleZhCn().isBlank()) {
            return "titleZhCn is required";
        }
        if (request.titleZhTw() == null || request.titleZhTw().isBlank()) {
            return "titleZhTw is required";
        }
        if (request.titleEn() == null || request.titleEn().isBlank()) {
            return "titleEn is required";
        }
        if (!isValidSchedule(request.startAt(), request.endAt())) {
            return "Schedule is invalid";
        }
        return null;
    }

    private boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim();
        return normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/");
    }

    private LaunchAdPublicResponse toPublicResponse(LaunchAd entity) {
        return new LaunchAdPublicResponse(
                entity.getId(),
                entity.getCode(),
                entity.getImageUrl(),
                entity.getTargetUrl(),
                entity.getImageVersion(),
                entity.getDisplaySeconds(),
                entity.getStatus(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getSponsorZhCn(),
                entity.getSponsorZhTw(),
                entity.getSponsorEn(),
                entity.getTitleZhCn(),
                entity.getTitleZhTw(),
                entity.getTitleEn(),
                entity.getDescriptionZhCn(),
                entity.getDescriptionZhTw(),
                entity.getDescriptionEn(),
                entity.getCtaZhCn(),
                entity.getCtaZhTw(),
                entity.getCtaEn(),
                entity.getFooterZhCn(),
                entity.getFooterZhTw(),
                entity.getFooterEn(),
                deriveEffectiveState(entity, LocalDateTime.now()),
                entity.getUpdatedAt()
        );
    }

    private LaunchAdAdminItem toAdminItem(LaunchAd entity) {
        return new LaunchAdAdminItem(
                entity.getId(),
                entity.getCode(),
                entity.getImageUrl(),
                entity.getTargetUrl(),
                entity.getImageVersion(),
                entity.getDisplaySeconds(),
                entity.getSponsorZhCn(),
                entity.getSponsorZhTw(),
                entity.getSponsorEn(),
                entity.getTitleZhCn(),
                entity.getTitleZhTw(),
                entity.getTitleEn(),
                entity.getDescriptionZhCn(),
                entity.getDescriptionZhTw(),
                entity.getDescriptionEn(),
                entity.getCtaZhCn(),
                entity.getCtaZhTw(),
                entity.getCtaEn(),
                entity.getFooterZhCn(),
                entity.getFooterZhTw(),
                entity.getFooterEn(),
                entity.getStatus(),
                entity.getStartAt(),
                entity.getEndAt(),
                deriveEffectiveState(entity, LocalDateTime.now()),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private boolean isValidSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        return startAt == null || endAt == null || !endAt.isBefore(startAt);
    }

    private boolean isWithinSchedule(LaunchAd entity, LocalDateTime now) {
        return (entity.getStartAt() == null || !entity.getStartAt().isAfter(now))
                && (entity.getEndAt() == null || !entity.getEndAt().isBefore(now));
    }

    private String deriveEffectiveState(LaunchAd entity, LocalDateTime now) {
        String status = entity.getStatus() == null ? STATUS_DRAFT : entity.getStatus().trim().toUpperCase();
        if (STATUS_INACTIVE.equals(status)) {
            return "INACTIVE";
        }
        if (!STATUS_ACTIVE.equals(status)) {
            return "DRAFT";
        }
        if (entity.getStartAt() != null && entity.getStartAt().isAfter(now)) {
            return "SCHEDULED";
        }
        if (entity.getEndAt() != null && entity.getEndAt().isBefore(now)) {
            return "EXPIRED";
        }
        return "LIVE";
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        if (STATUS_ACTIVE.equals(normalized) || STATUS_DRAFT.equals(normalized) || STATUS_INACTIVE.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String resolveUsername(User currentUser) {
        return currentUser == null ? "unknown" : currentUser.getUsername();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
