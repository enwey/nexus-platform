package com.nexus.platform.service;

import com.nexus.platform.dto.OpsGiftCodeCampaignDto;
import com.nexus.platform.dto.OpsGiftCodeCampaignStatusRequest;
import com.nexus.platform.dto.OpsGiftCodeCampaignUpsertRequest;
import com.nexus.platform.dto.OpsGiftCodeEntryDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsGiftCodeCampaign;
import com.nexus.platform.entity.OpsGiftCodeEntry;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsGiftCodeCampaignRepository;
import com.nexus.platform.repository.OpsGiftCodeEntryRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsGiftCodeCampaignService {
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "PAUSED", "ARCHIVED");
    private static final Set<String> AUDIENCES = Set.of("ALL", "NEW_USER", "RETURNING_USER", "DEVELOPER");
    private static final Set<String> REWARD_TYPES = Set.of("COUPON", "POINTS", "GIFT_PACK", "CASH_VOUCHER");
    private static final Set<String> CODE_STATUSES = Set.of("AVAILABLE", "REDEEMED", "VOID");

    private final OpsGiftCodeCampaignRepository campaignRepository;
    private final OpsGiftCodeEntryRepository codeRepository;
    private final AuditLogService auditLogService;

    public Result<List<OpsGiftCodeCampaignDto>> list() {
        return Result.success(campaignRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList());
    }

    public Result<List<OpsGiftCodeEntryDto>> listCodes(Long campaignId) {
        OpsGiftCodeCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            return Result.error("Gift campaign not found");
        }
        return Result.success(codeRepository.findTop200ByCampaignIdOrderByCreatedAtDesc(campaignId).stream().map(this::toCodeDto).toList());
    }

    @Transactional
    public Result<OpsGiftCodeCampaignDto> create(OpsGiftCodeCampaignUpsertRequest request, User currentUser, String requestUri) {
        String error = validate(null, request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_GIFT_CODE_CREATE", currentUser, null, "GIFT_CODE", false, error, requestUri);
            return Result.error(error);
        }
        OpsGiftCodeCampaign entity = new OpsGiftCodeCampaign();
        entity.setCampaignCode(generateCampaignCode());
        applyRequest(entity, request, currentUser);
        OpsGiftCodeCampaign saved = campaignRepository.save(entity);
        generateCodes(saved, request.totalStock() == null ? 0 : request.totalStock());
        auditLogService.logOpsAudit("OPS_GIFT_CODE_CREATE", currentUser, null, saved.getCampaignCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsGiftCodeCampaignDto> update(Long id, OpsGiftCodeCampaignUpsertRequest request, User currentUser, String requestUri) {
        OpsGiftCodeCampaign entity = campaignRepository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_GIFT_CODE_UPDATE", currentUser, null, "GIFT_CODE", false, "Gift campaign not found", requestUri);
            return Result.error("Gift campaign not found");
        }
        String error = validate(entity, request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_GIFT_CODE_UPDATE", currentUser, null, entity.getCampaignCode(), false, error, requestUri);
            return Result.error(error);
        }
        long existing = codeRepository.countByCampaign(entity);
        int targetStock = request.totalStock() == null ? 0 : request.totalStock();
        applyRequest(entity, request, currentUser);
        OpsGiftCodeCampaign saved = campaignRepository.save(entity);
        if (targetStock > existing) {
            generateCodes(saved, (int) (targetStock - existing));
        }
        auditLogService.logOpsAudit("OPS_GIFT_CODE_UPDATE", currentUser, null, saved.getCampaignCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsGiftCodeCampaignDto> updateStatus(Long id, OpsGiftCodeCampaignStatusRequest request, User currentUser, String requestUri) {
        OpsGiftCodeCampaign entity = campaignRepository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_GIFT_CODE_STATUS_UPDATE", currentUser, null, "GIFT_CODE", false, "Gift campaign not found", requestUri);
            return Result.error("Gift campaign not found");
        }
        String status = normalize(request == null ? null : request.status(), STATUSES);
        if (status == null) {
            auditLogService.logOpsAudit("OPS_GIFT_CODE_STATUS_UPDATE", currentUser, null, entity.getCampaignCode(), false, "Invalid gift campaign status", requestUri);
            return Result.error("Invalid gift campaign status");
        }
        entity.setStatus(status);
        entity.setUpdatedBy(resolveUsername(currentUser));
        OpsGiftCodeCampaign saved = campaignRepository.save(entity);
        auditLogService.logOpsAudit("OPS_GIFT_CODE_STATUS_UPDATE", currentUser, null, saved.getCampaignCode(), true, status, requestUri);
        return Result.success(toDto(saved));
    }

    private void applyRequest(OpsGiftCodeCampaign entity, OpsGiftCodeCampaignUpsertRequest request, User currentUser) {
        entity.setTitle(request.title().trim());
        entity.setStatus(normalize(request.status(), STATUSES));
        entity.setAudience(normalize(request.audience(), AUDIENCES));
        entity.setRewardType(normalize(request.rewardType(), REWARD_TYPES));
        entity.setRewardSummary(trim(request.rewardSummary(), 256));
        entity.setLandingUrl(trim(request.landingUrl(), 512));
        entity.setCodePrefix(normalizeCodePrefix(request.codePrefix()));
        entity.setTotalStock(request.totalStock() == null ? 0 : request.totalStock());
        entity.setPerUserLimit(request.perUserLimit() == null ? 1 : Math.max(1, request.perUserLimit()));
        entity.setNote(trim(request.note(), 256));
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setUpdatedBy(resolveUsername(currentUser));
    }

    private String validate(OpsGiftCodeCampaign current, OpsGiftCodeCampaignUpsertRequest request) {
        if (request == null) return "Request body is required";
        if (normalize(request.status(), STATUSES) == null) return "Invalid gift campaign status";
        if (normalize(request.audience(), AUDIENCES) == null) return "Invalid gift campaign audience";
        if (normalize(request.rewardType(), REWARD_TYPES) == null) return "Invalid reward type";
        String title = trim(request.title(), 128);
        if (title == null || title.length() < 2) return "Gift campaign title is invalid";
        String rewardSummary = trim(request.rewardSummary(), 256);
        if (rewardSummary == null || rewardSummary.length() < 2) return "Reward summary is invalid";
        String codePrefix = normalizeCodePrefix(request.codePrefix());
        if (codePrefix == null || codePrefix.length() < 2) return "Code prefix is invalid";
        Integer totalStock = request.totalStock();
        if (totalStock == null || totalStock < 1 || totalStock > 5000) return "Total stock is invalid";
        if (request.perUserLimit() == null || request.perUserLimit() < 1 || request.perUserLimit() > 20) return "Per-user limit is invalid";
        if (!isValidSchedule(request.startAt(), request.endAt())) return "Gift campaign schedule is invalid";
        if (!isValidUrl(request.landingUrl())) return "landingUrl is invalid";
        if (current != null) {
            long existing = codeRepository.countByCampaign(current);
            long redeemed = codeRepository.countByCampaignIdAndStatus(current.getId(), "REDEEMED");
            if (totalStock < existing) return "Total stock cannot be smaller than generated code count";
            if (totalStock < redeemed) return "Total stock cannot be smaller than redeemed stock";
        }
        return null;
    }

    private OpsGiftCodeCampaignDto toDto(OpsGiftCodeCampaign entity) {
        int available = (int) codeRepository.countByCampaignIdAndStatus(entity.getId(), "AVAILABLE");
        int redeemed = (int) codeRepository.countByCampaignIdAndStatus(entity.getId(), "REDEEMED");
        return new OpsGiftCodeCampaignDto(
                entity.getId(),
                entity.getCampaignCode(),
                entity.getTitle(),
                entity.getStatus(),
                deriveEffectiveStatus(entity),
                entity.getAudience(),
                entity.getRewardType(),
                entity.getRewardSummary(),
                entity.getLandingUrl(),
                entity.getCodePrefix(),
                entity.getTotalStock(),
                available,
                redeemed,
                entity.getPerUserLimit(),
                entity.getNote(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OpsGiftCodeEntryDto toCodeDto(OpsGiftCodeEntry entity) {
        return new OpsGiftCodeEntryDto(
                entity.getId(),
                entity.getCode(),
                entity.getStatus(),
                entity.getRedeemedByUserId(),
                entity.getRedeemedAt(),
                entity.getCreatedAt()
        );
    }

    private void generateCodes(OpsGiftCodeCampaign campaign, int quantity) {
        if (quantity <= 0) {
            return;
        }
        List<OpsGiftCodeEntry> entries = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            OpsGiftCodeEntry entry = new OpsGiftCodeEntry();
            entry.setCampaign(campaign);
            entry.setStatus("AVAILABLE");
            entry.setCode(generateUniqueCode(campaign.getCodePrefix()));
            entries.add(entry);
        }
        codeRepository.saveAll(entries);
    }

    private String generateUniqueCode(String prefix) {
        String normalizedPrefix = prefix == null ? "GIFT" : prefix;
        String candidate = normalizedPrefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        while (codeRepository.existsByCode(candidate)) {
            candidate = normalizedPrefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        }
        return candidate;
    }

    private String deriveEffectiveStatus(OpsGiftCodeCampaign entity) {
        String status = entity.getStatus() == null ? "DRAFT" : entity.getStatus();
        if (!"PUBLISHED".equals(status)) return status;
        LocalDateTime now = LocalDateTime.now();
        if (entity.getStartAt() != null && entity.getStartAt().isAfter(now)) return "SCHEDULED";
        if (entity.getEndAt() != null && entity.getEndAt().isBefore(now)) return "EXPIRED";
        if (codeRepository.countByCampaignIdAndStatus(entity.getId(), "AVAILABLE") <= 0) return "SOLD_OUT";
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

    private String normalizeCodePrefix(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
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

    private String generateCampaignCode() {
        return "GIFT_" + System.currentTimeMillis();
    }
}
