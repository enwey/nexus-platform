package com.nexus.platform.service;

import com.nexus.platform.dto.OpsCampaignDto;
import com.nexus.platform.dto.OpsCampaignStatusRequest;
import com.nexus.platform.dto.OpsCampaignUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsCampaign;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsCampaignRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsCampaignService {
    private static final Set<String> TYPES = Set.of("TOPIC", "POPUP", "TASK", "GIFT");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "PAUSED", "ARCHIVED");
    private static final Set<String> AUDIENCES = Set.of("ALL", "NEW_USER", "RETURNING_USER", "DEVELOPER");

    private final OpsCampaignRepository opsCampaignRepository;
    private final AuditLogService auditLogService;

    public Result<List<OpsCampaignDto>> listCampaigns() {
        return Result.success(opsCampaignRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList());
    }

    @Transactional
    public Result<OpsCampaignDto> create(OpsCampaignUpsertRequest request, User currentUser, String requestUri) {
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_CAMPAIGN_CREATE", currentUser, null, "CAMPAIGN", false, error, requestUri);
            return Result.error(error);
        }
        OpsCampaign entity = new OpsCampaign();
        applyRequest(entity, request, currentUser);
        OpsCampaign saved = opsCampaignRepository.save(entity);
        auditLogService.logOpsAudit("OPS_CAMPAIGN_CREATE", currentUser, null, saved.getCampaignCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsCampaignDto> update(Long id, OpsCampaignUpsertRequest request, User currentUser, String requestUri) {
        OpsCampaign entity = opsCampaignRepository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_CAMPAIGN_UPDATE", currentUser, null, "CAMPAIGN", false, "Campaign not found", requestUri);
            return Result.error("Campaign not found");
        }
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_CAMPAIGN_UPDATE", currentUser, null, entity.getCampaignCode(), false, error, requestUri);
            return Result.error(error);
        }
        applyRequest(entity, request, currentUser);
        OpsCampaign saved = opsCampaignRepository.save(entity);
        auditLogService.logOpsAudit("OPS_CAMPAIGN_UPDATE", currentUser, null, saved.getCampaignCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsCampaignDto> updateStatus(Long id, OpsCampaignStatusRequest request, User currentUser, String requestUri) {
        OpsCampaign entity = opsCampaignRepository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_CAMPAIGN_STATUS_UPDATE", currentUser, null, "CAMPAIGN", false, "Campaign not found", requestUri);
            return Result.error("Campaign not found");
        }
        String status = normalize(request == null ? null : request.status(), STATUSES);
        if (status == null) {
            auditLogService.logOpsAudit("OPS_CAMPAIGN_STATUS_UPDATE", currentUser, null, entity.getCampaignCode(), false, "Invalid campaign status", requestUri);
            return Result.error("Invalid campaign status");
        }
        entity.setStatus(status);
        entity.setUpdatedBy(resolveUsername(currentUser));
        OpsCampaign saved = opsCampaignRepository.save(entity);
        auditLogService.logOpsAudit("OPS_CAMPAIGN_STATUS_UPDATE", currentUser, null, saved.getCampaignCode(), true, status, requestUri);
        return Result.success(toDto(saved));
    }

    private void applyRequest(OpsCampaign entity, OpsCampaignUpsertRequest request, User currentUser) {
        entity.setCampaignType(normalize(request.campaignType(), TYPES));
        entity.setTitle(request.title().trim());
        entity.setStatus(normalize(request.status(), STATUSES));
        entity.setAudience(normalize(request.audience(), AUDIENCES));
        entity.setLandingUrl(trim(request.landingUrl(), 512));
        entity.setBannerUrl(trim(request.bannerUrl(), 512));
        entity.setPriority(request.priority() == null ? 0 : Math.max(0, request.priority()));
        entity.setNote(trim(request.note(), 256));
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setUpdatedBy(resolveUsername(currentUser));
    }

    private String validate(OpsCampaignUpsertRequest request) {
        if (request == null) return "Request body is required";
        if (normalize(request.campaignType(), TYPES) == null) return "Invalid campaign type";
        if (normalize(request.status(), STATUSES) == null) return "Invalid campaign status";
        if (normalize(request.audience(), AUDIENCES) == null) return "Invalid audience";
        String title = trim(request.title(), 128);
        if (title == null || title.length() < 2) return "Campaign title is invalid";
        if (!isValidSchedule(request.startAt(), request.endAt())) return "Campaign schedule is invalid";
        if (!isValidUrl(request.landingUrl())) return "landingUrl is invalid";
        if (!isValidUrl(request.bannerUrl())) return "bannerUrl is invalid";
        return null;
    }

    private OpsCampaignDto toDto(OpsCampaign entity) {
        return new OpsCampaignDto(
                entity.getId(),
                entity.getCampaignCode(),
                entity.getCampaignType(),
                entity.getTitle(),
                entity.getStatus(),
                deriveEffectiveStatus(entity),
                entity.getAudience(),
                entity.getLandingUrl(),
                entity.getBannerUrl(),
                entity.getPriority(),
                entity.getNote(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String deriveEffectiveStatus(OpsCampaign entity) {
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
}
