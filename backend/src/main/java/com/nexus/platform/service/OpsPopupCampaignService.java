package com.nexus.platform.service;

import com.nexus.platform.dto.OpsPopupCampaignDto;
import com.nexus.platform.dto.OpsPopupCampaignStatusRequest;
import com.nexus.platform.dto.OpsPopupCampaignUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsPopupCampaign;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsPopupCampaignRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsPopupCampaignService {
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "PAUSED", "ARCHIVED");
    private static final Set<String> AUDIENCES = Set.of("ALL", "NEW_USER", "RETURNING_USER", "DEVELOPER");
    private static final Set<String> TRIGGER_SCENES = Set.of("APP_LAUNCH", "TAB_ENTER", "GAME_EXIT", "PAY_SUCCESS");

    private final OpsPopupCampaignRepository repository;
    private final AuditLogService auditLogService;

    public Result<List<OpsPopupCampaignDto>> list() {
        return Result.success(repository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList());
    }

    @Transactional
    public Result<OpsPopupCampaignDto> create(OpsPopupCampaignUpsertRequest request, User currentUser, String requestUri) {
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_POPUP_CREATE", currentUser, null, "POPUP", false, error, requestUri);
            return Result.error(error);
        }
        OpsPopupCampaign entity = new OpsPopupCampaign();
        entity.setPopupCode(generateCode());
        applyRequest(entity, request, currentUser);
        OpsPopupCampaign saved = repository.save(entity);
        auditLogService.logOpsAudit("OPS_POPUP_CREATE", currentUser, null, saved.getPopupCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsPopupCampaignDto> update(Long id, OpsPopupCampaignUpsertRequest request, User currentUser, String requestUri) {
        OpsPopupCampaign entity = repository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_POPUP_UPDATE", currentUser, null, "POPUP", false, "Popup campaign not found", requestUri);
            return Result.error("Popup campaign not found");
        }
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_POPUP_UPDATE", currentUser, null, entity.getPopupCode(), false, error, requestUri);
            return Result.error(error);
        }
        applyRequest(entity, request, currentUser);
        OpsPopupCampaign saved = repository.save(entity);
        auditLogService.logOpsAudit("OPS_POPUP_UPDATE", currentUser, null, saved.getPopupCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsPopupCampaignDto> updateStatus(Long id, OpsPopupCampaignStatusRequest request, User currentUser, String requestUri) {
        OpsPopupCampaign entity = repository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_POPUP_STATUS_UPDATE", currentUser, null, "POPUP", false, "Popup campaign not found", requestUri);
            return Result.error("Popup campaign not found");
        }
        String status = normalize(request == null ? null : request.status(), STATUSES);
        if (status == null) {
            auditLogService.logOpsAudit("OPS_POPUP_STATUS_UPDATE", currentUser, null, entity.getPopupCode(), false, "Invalid popup status", requestUri);
            return Result.error("Invalid popup status");
        }
        entity.setStatus(status);
        entity.setUpdatedBy(resolveUsername(currentUser));
        OpsPopupCampaign saved = repository.save(entity);
        auditLogService.logOpsAudit("OPS_POPUP_STATUS_UPDATE", currentUser, null, saved.getPopupCode(), true, status, requestUri);
        return Result.success(toDto(saved));
    }

    private void applyRequest(OpsPopupCampaign entity, OpsPopupCampaignUpsertRequest request, User currentUser) {
        entity.setTitle(request.title().trim());
        entity.setStatus(normalize(request.status(), STATUSES));
        entity.setAudience(normalize(request.audience(), AUDIENCES));
        entity.setTriggerScene(normalize(request.triggerScene(), TRIGGER_SCENES));
        entity.setLandingUrl(trim(request.landingUrl(), 512));
        entity.setImageUrl(trim(request.imageUrl(), 512));
        entity.setButtonText(trim(request.buttonText(), 64));
        entity.setPriority(request.priority() == null ? 0 : Math.max(0, request.priority()));
        entity.setFrequencyLimitPerDay(request.frequencyLimitPerDay() == null ? 1 : Math.max(1, request.frequencyLimitPerDay()));
        entity.setNote(trim(request.note(), 256));
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setUpdatedBy(resolveUsername(currentUser));
    }

    private String validate(OpsPopupCampaignUpsertRequest request) {
        if (request == null) return "Request body is required";
        if (normalize(request.status(), STATUSES) == null) return "Invalid popup status";
        if (normalize(request.audience(), AUDIENCES) == null) return "Invalid popup audience";
        if (normalize(request.triggerScene(), TRIGGER_SCENES) == null) return "Invalid trigger scene";
        String title = trim(request.title(), 128);
        if (title == null || title.length() < 2) return "Popup title is invalid";
        if (!isValidSchedule(request.startAt(), request.endAt())) return "Popup schedule is invalid";
        if (!isValidUrl(request.landingUrl())) return "landingUrl is invalid";
        if (!isValidUrl(request.imageUrl())) return "imageUrl is invalid";
        return null;
    }

    private OpsPopupCampaignDto toDto(OpsPopupCampaign entity) {
        return new OpsPopupCampaignDto(
                entity.getId(),
                entity.getPopupCode(),
                entity.getTitle(),
                entity.getStatus(),
                deriveEffectiveStatus(entity),
                entity.getAudience(),
                entity.getTriggerScene(),
                entity.getLandingUrl(),
                entity.getImageUrl(),
                entity.getButtonText(),
                entity.getPriority(),
                entity.getFrequencyLimitPerDay(),
                entity.getNote(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String deriveEffectiveStatus(OpsPopupCampaign entity) {
        String status = entity.getStatus() == null ? "DRAFT" : entity.getStatus();
        if (!"PUBLISHED".equals(status)) return status;
        LocalDateTime now = LocalDateTime.now();
        if (entity.getStartAt() != null && entity.getStartAt().isAfter(now)) return "SCHEDULED";
        if (entity.getEndAt() != null && entity.getEndAt().isBefore(now)) return "EXPIRED";
        return "LIVE";
    }

    private boolean isValidSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        return startAt == null || endAt == null || !endAt.isBefore(startAt);
    }

    private boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) return true;
        String normalized = value.trim();
        return normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/");
    }

    private String normalize(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
    }

    private String trim(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String resolveUsername(User currentUser) {
        return currentUser == null ? "unknown" : currentUser.getUsername();
    }

    private String generateCode() {
        return "POPUP_" + System.currentTimeMillis();
    }
}
