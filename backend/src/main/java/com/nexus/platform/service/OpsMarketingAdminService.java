package com.nexus.platform.service;

import com.nexus.platform.dto.OpsBatchActionResultDto;
import com.nexus.platform.dto.OpsMarketingAnalyticsDto;
import com.nexus.platform.dto.OpsMarketingBatchStatusRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.LaunchAd;
import com.nexus.platform.entity.OpsCampaign;
import com.nexus.platform.entity.OpsGiftCodeCampaign;
import com.nexus.platform.entity.OpsGiftCodeEntry;
import com.nexus.platform.entity.OpsPopupCampaign;
import com.nexus.platform.entity.OpsTaskCampaign;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.LaunchAdRepository;
import com.nexus.platform.repository.OpsCampaignRepository;
import com.nexus.platform.repository.OpsGiftCodeCampaignRepository;
import com.nexus.platform.repository.OpsGiftCodeEntryRepository;
import com.nexus.platform.repository.OpsPopupCampaignRepository;
import com.nexus.platform.repository.OpsTaskCampaignRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsMarketingAdminService {
    private static final Set<String> CONTENT_STATUSES = Set.of("DRAFT", "PUBLISHED", "PAUSED", "ARCHIVED");
    private static final Set<String> LAUNCH_STATUSES = Set.of("ACTIVE", "INACTIVE", "DRAFT");
    private static final Set<String> ASSET_TYPES = Set.of("CAMPAIGN", "POPUP", "TASK", "GIFT", "LAUNCH_AD");

    private final OpsCampaignRepository campaignRepository;
    private final OpsPopupCampaignRepository popupCampaignRepository;
    private final OpsTaskCampaignRepository taskCampaignRepository;
    private final OpsGiftCodeCampaignRepository giftCodeCampaignRepository;
    private final OpsGiftCodeEntryRepository giftCodeEntryRepository;
    private final LaunchAdRepository launchAdRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Result<OpsMarketingAnalyticsDto> getAnalytics() {
        List<OpsMarketingAnalyticsDto.AssetMetrics> metrics = new ArrayList<>();
        metrics.add(buildCampaignMetrics());
        metrics.add(buildPopupMetrics());
        metrics.add(buildTaskMetrics());
        metrics.add(buildGiftMetrics());
        metrics.add(buildLaunchAdMetrics());

        int totalAssets = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::total).sum();
        int liveAssets = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::live).sum();
        int scheduledAssets = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::scheduled).sum();
        int draftAssets = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::draft).sum();
        int pausedAssets = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::paused).sum();
        int archivedAssets = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::archived).sum();
        int expiredAssets = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::expired).sum();
        int availableGiftCodes = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::stock).sum();
        int redeemedGiftCodes = metrics.stream().mapToInt(OpsMarketingAnalyticsDto.AssetMetrics::redeemed).sum();
        return Result.success(new OpsMarketingAnalyticsDto(
                totalAssets,
                liveAssets,
                scheduledAssets,
                draftAssets,
                pausedAssets,
                archivedAssets,
                expiredAssets,
                availableGiftCodes,
                redeemedGiftCodes,
                metrics
        ));
    }

    @Transactional
    public Result<OpsBatchActionResultDto> batchUpdateStatus(
            OpsMarketingBatchStatusRequest request,
            User currentUser,
            String requestUri
    ) {
        String assetType = normalize(request == null ? null : request.assetType(), ASSET_TYPES);
        if (assetType == null) {
            auditLogService.logOpsAudit("OPS_MARKETING_BATCH_STATUS", currentUser, null, "MARKETING", false, "Invalid asset type", requestUri);
            return Result.error("Invalid asset type");
        }
        List<Long> ids = request == null || request.ids() == null ? List.of() : request.ids().stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty() || ids.size() > 100) {
            auditLogService.logOpsAudit("OPS_MARKETING_BATCH_STATUS", currentUser, null, assetType, false, "Invalid batch size", requestUri);
            return Result.error("Invalid batch size");
        }
        String reason = trim(request.reason(), 200);
        if (requiresReason(request == null ? null : request.status()) && reason == null) {
            auditLogService.logOpsAudit("OPS_MARKETING_BATCH_STATUS", currentUser, null, assetType, false, "Reason is required", requestUri);
            return Result.error("Reason is required");
        }

        return switch (assetType) {
            case "CAMPAIGN" -> batchUpdateCampaignStatus(ids, request.status(), reason, currentUser, requestUri);
            case "POPUP" -> batchUpdatePopupStatus(ids, request.status(), reason, currentUser, requestUri);
            case "TASK" -> batchUpdateTaskStatus(ids, request.status(), reason, currentUser, requestUri);
            case "GIFT" -> batchUpdateGiftStatus(ids, request.status(), reason, currentUser, requestUri);
            case "LAUNCH_AD" -> batchUpdateLaunchAdStatus(ids, request.status(), reason, currentUser, requestUri);
            default -> Result.error("Invalid asset type");
        };
    }

    private Result<OpsBatchActionResultDto> batchUpdateCampaignStatus(List<Long> ids, String rawStatus, String reason, User currentUser, String requestUri) {
        String status = normalize(rawStatus, CONTENT_STATUSES);
        if (status == null) {
            return batchError("CAMPAIGN", "Invalid campaign status", currentUser, requestUri);
        }
        List<OpsCampaign> rows = campaignRepository.findAllById(ids);
        if (rows.size() != ids.size()) {
            return batchError("CAMPAIGN", "Some campaigns were not found", currentUser, requestUri);
        }
        if (rows.stream().anyMatch(row -> !isAllowedContentTransition(row.getStatus(), status))) {
            return batchError("CAMPAIGN", "Campaign batch transition is invalid", currentUser, requestUri);
        }
        rows.forEach(row -> {
            row.setStatus(status);
            row.setUpdatedBy(resolveUsername(currentUser));
        });
        campaignRepository.saveAll(rows);
        logBatchRows("OPS_CAMPAIGN_BATCH_STATUS", rows.stream().map(OpsCampaign::getCampaignCode).toList(), status, reason, currentUser, requestUri);
        return Result.success(new OpsBatchActionResultDto(ids.size(), rows.size(), "CAMPAIGN_" + status, ids));
    }

    private Result<OpsBatchActionResultDto> batchUpdatePopupStatus(List<Long> ids, String rawStatus, String reason, User currentUser, String requestUri) {
        String status = normalize(rawStatus, CONTENT_STATUSES);
        if (status == null) {
            return batchError("POPUP", "Invalid popup status", currentUser, requestUri);
        }
        List<OpsPopupCampaign> rows = popupCampaignRepository.findAllById(ids);
        if (rows.size() != ids.size()) {
            return batchError("POPUP", "Some popup campaigns were not found", currentUser, requestUri);
        }
        if (rows.stream().anyMatch(row -> !isAllowedContentTransition(row.getStatus(), status))) {
            return batchError("POPUP", "Popup batch transition is invalid", currentUser, requestUri);
        }
        rows.forEach(row -> {
            row.setStatus(status);
            row.setUpdatedBy(resolveUsername(currentUser));
        });
        popupCampaignRepository.saveAll(rows);
        logBatchRows("OPS_POPUP_BATCH_STATUS", rows.stream().map(OpsPopupCampaign::getPopupCode).toList(), status, reason, currentUser, requestUri);
        return Result.success(new OpsBatchActionResultDto(ids.size(), rows.size(), "POPUP_" + status, ids));
    }

    private Result<OpsBatchActionResultDto> batchUpdateTaskStatus(List<Long> ids, String rawStatus, String reason, User currentUser, String requestUri) {
        String status = normalize(rawStatus, CONTENT_STATUSES);
        if (status == null) {
            return batchError("TASK", "Invalid task status", currentUser, requestUri);
        }
        List<OpsTaskCampaign> rows = taskCampaignRepository.findAllById(ids);
        if (rows.size() != ids.size()) {
            return batchError("TASK", "Some task campaigns were not found", currentUser, requestUri);
        }
        if (rows.stream().anyMatch(row -> !isAllowedContentTransition(row.getStatus(), status))) {
            return batchError("TASK", "Task batch transition is invalid", currentUser, requestUri);
        }
        rows.forEach(row -> {
            row.setStatus(status);
            row.setUpdatedBy(resolveUsername(currentUser));
        });
        taskCampaignRepository.saveAll(rows);
        logBatchRows("OPS_TASK_BATCH_STATUS", rows.stream().map(OpsTaskCampaign::getTaskCode).toList(), status, reason, currentUser, requestUri);
        return Result.success(new OpsBatchActionResultDto(ids.size(), rows.size(), "TASK_" + status, ids));
    }

    private Result<OpsBatchActionResultDto> batchUpdateGiftStatus(List<Long> ids, String rawStatus, String reason, User currentUser, String requestUri) {
        String status = normalize(rawStatus, CONTENT_STATUSES);
        if (status == null) {
            return batchError("GIFT", "Invalid gift campaign status", currentUser, requestUri);
        }
        List<OpsGiftCodeCampaign> rows = giftCodeCampaignRepository.findAllById(ids);
        if (rows.size() != ids.size()) {
            return batchError("GIFT", "Some gift campaigns were not found", currentUser, requestUri);
        }
        if (rows.stream().anyMatch(row -> !isAllowedContentTransition(row.getStatus(), status))) {
            return batchError("GIFT", "Gift batch transition is invalid", currentUser, requestUri);
        }
        if ("PUBLISHED".equals(status) && rows.stream().anyMatch(row -> giftCodeEntryRepository.countByCampaignIdAndStatus(row.getId(), "AVAILABLE") <= 0)) {
            return batchError("GIFT", "Gift campaigns without available codes cannot be published", currentUser, requestUri);
        }
        rows.forEach(row -> {
            row.setStatus(status);
            row.setUpdatedBy(resolveUsername(currentUser));
        });
        giftCodeCampaignRepository.saveAll(rows);
        logBatchRows("OPS_GIFT_BATCH_STATUS", rows.stream().map(OpsGiftCodeCampaign::getCampaignCode).toList(), status, reason, currentUser, requestUri);
        return Result.success(new OpsBatchActionResultDto(ids.size(), rows.size(), "GIFT_" + status, ids));
    }

    private Result<OpsBatchActionResultDto> batchUpdateLaunchAdStatus(List<Long> ids, String rawStatus, String reason, User currentUser, String requestUri) {
        String status = normalize(rawStatus, LAUNCH_STATUSES);
        if (status == null) {
            return batchError("LAUNCH_AD", "Invalid launch ad status", currentUser, requestUri);
        }
        List<LaunchAd> rows = launchAdRepository.findAllById(ids);
        if (rows.size() != ids.size()) {
            return batchError("LAUNCH_AD", "Some launch ads were not found", currentUser, requestUri);
        }
        if (rows.stream().anyMatch(row -> !isAllowedLaunchTransition(row.getStatus(), status))) {
            return batchError("LAUNCH_AD", "Launch ad batch transition is invalid", currentUser, requestUri);
        }
        if ("ACTIVE".equals(status)) {
            if (ids.size() != 1) {
                return batchError("LAUNCH_AD", "Only one launch ad can be activated at a time", currentUser, requestUri);
            }
            LaunchAd row = rows.get(0);
            if (!isValidSchedule(row.getStartAt(), row.getEndAt())) {
                return batchError("LAUNCH_AD", "Launch ad schedule is invalid", currentUser, requestUri);
            }
            launchAdRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .filter(item -> "ACTIVE".equals(item.getStatus()))
                    .filter(item -> !item.getId().equals(row.getId()))
                    .forEach(item -> {
                        item.setStatus("INACTIVE");
                        item.setUpdatedBy(resolveUsername(currentUser));
                        launchAdRepository.save(item);
                    });
        }
        rows.forEach(row -> {
            row.setStatus(status);
            row.setUpdatedBy(resolveUsername(currentUser));
        });
        launchAdRepository.saveAll(rows);
        logBatchRows("LAUNCH_AD_BATCH_STATUS", rows.stream().map(LaunchAd::getCode).toList(), status, reason, currentUser, requestUri);
        return Result.success(new OpsBatchActionResultDto(ids.size(), rows.size(), "LAUNCH_AD_" + status, ids));
    }

    private OpsMarketingAnalyticsDto.AssetMetrics buildCampaignMetrics() {
        List<OpsCampaign> rows = campaignRepository.findAllByOrderByUpdatedAtDesc();
        return summarizeAssetMetrics(
                "CAMPAIGN",
                "Campaign Hubs",
                rows.stream().map(this::deriveEffectiveStatus).toList(),
                0,
                0
        );
    }

    private OpsMarketingAnalyticsDto.AssetMetrics buildPopupMetrics() {
        List<OpsPopupCampaign> rows = popupCampaignRepository.findAllByOrderByUpdatedAtDesc();
        return summarizeAssetMetrics(
                "POPUP",
                "Popup Campaigns",
                rows.stream().map(this::deriveEffectiveStatus).toList(),
                0,
                0
        );
    }

    private OpsMarketingAnalyticsDto.AssetMetrics buildTaskMetrics() {
        List<OpsTaskCampaign> rows = taskCampaignRepository.findAllByOrderByUpdatedAtDesc();
        return summarizeAssetMetrics(
                "TASK",
                "Task Campaigns",
                rows.stream().map(this::deriveEffectiveStatus).toList(),
                0,
                0
        );
    }

    private OpsMarketingAnalyticsDto.AssetMetrics buildGiftMetrics() {
        List<OpsGiftCodeCampaign> rows = giftCodeCampaignRepository.findAllByOrderByUpdatedAtDesc();
        int available = rows.stream().mapToInt(row -> (int) giftCodeEntryRepository.countByCampaignIdAndStatus(row.getId(), "AVAILABLE")).sum();
        int redeemed = rows.stream().mapToInt(row -> (int) giftCodeEntryRepository.countByCampaignIdAndStatus(row.getId(), "REDEEMED")).sum();
        return summarizeAssetMetrics(
                "GIFT",
                "Gift Code Campaigns",
                rows.stream().map(this::deriveEffectiveStatus).toList(),
                available,
                redeemed
        );
    }

    private OpsMarketingAnalyticsDto.AssetMetrics buildLaunchAdMetrics() {
        List<LaunchAd> rows = launchAdRepository.findAllByOrderByUpdatedAtDesc();
        return summarizeAssetMetrics(
                "LAUNCH_AD",
                "Launch Ads",
                rows.stream().map(this::deriveEffectiveState).toList(),
                0,
                0
        );
    }

    private OpsMarketingAnalyticsDto.AssetMetrics summarizeAssetMetrics(
            String assetType,
            String assetName,
            List<String> states,
            int stock,
            int redeemed
    ) {
        return new OpsMarketingAnalyticsDto.AssetMetrics(
                assetType,
                assetName,
                states.size(),
                countState(states, "LIVE"),
                countState(states, "SCHEDULED"),
                countState(states, "DRAFT"),
                countState(states, "PAUSED") + countState(states, "INACTIVE"),
                countState(states, "ARCHIVED"),
                countState(states, "EXPIRED") + countState(states, "SOLD_OUT"),
                stock,
                redeemed
        );
    }

    private int countState(List<String> states, String target) {
        return (int) states.stream().filter(target::equals).count();
    }

    private String deriveEffectiveStatus(OpsCampaign entity) {
        return deriveTimedStatus(entity.getStatus(), entity.getStartAt(), entity.getEndAt());
    }

    private String deriveEffectiveStatus(OpsPopupCampaign entity) {
        return deriveTimedStatus(entity.getStatus(), entity.getStartAt(), entity.getEndAt());
    }

    private String deriveEffectiveStatus(OpsTaskCampaign entity) {
        return deriveTimedStatus(entity.getStatus(), entity.getStartAt(), entity.getEndAt());
    }

    private String deriveEffectiveStatus(OpsGiftCodeCampaign entity) {
        if (!"PUBLISHED".equals(entity.getStatus())) {
            return entity.getStatus() == null ? "DRAFT" : entity.getStatus();
        }
        if (giftCodeEntryRepository.countByCampaignIdAndStatus(entity.getId(), "AVAILABLE") <= 0) {
            return "SOLD_OUT";
        }
        return deriveTimedStatus(entity.getStatus(), entity.getStartAt(), entity.getEndAt());
    }

    private String deriveEffectiveState(LaunchAd entity) {
        String status = entity.getStatus() == null ? "DRAFT" : entity.getStatus();
        if (!"ACTIVE".equals(status)) {
            return status;
        }
        LocalDateTime now = LocalDateTime.now();
        if (entity.getStartAt() != null && entity.getStartAt().isAfter(now)) {
            return "SCHEDULED";
        }
        if (entity.getEndAt() != null && entity.getEndAt().isBefore(now)) {
            return "EXPIRED";
        }
        return "LIVE";
    }

    private String deriveTimedStatus(String status, LocalDateTime startAt, LocalDateTime endAt) {
        String current = status == null ? "DRAFT" : status;
        if (!"PUBLISHED".equals(current)) {
            return current;
        }
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && startAt.isAfter(now)) {
            return "SCHEDULED";
        }
        if (endAt != null && endAt.isBefore(now)) {
            return "EXPIRED";
        }
        return "LIVE";
    }

    private boolean isValidSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        return startAt == null || endAt == null || !endAt.isBefore(startAt);
    }

    private boolean requiresReason(String rawStatus) {
        if (rawStatus == null) {
            return false;
        }
        return Set.of("PAUSED", "ARCHIVED", "INACTIVE", "DRAFT").contains(rawStatus.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isAllowedContentTransition(String currentStatus, String targetStatus) {
        String current = currentStatus == null ? "DRAFT" : currentStatus.toUpperCase(Locale.ROOT);
        if (current.equals(targetStatus)) {
            return true;
        }
        return switch (current) {
            case "DRAFT" -> Set.of("PUBLISHED", "PAUSED", "ARCHIVED").contains(targetStatus);
            case "PUBLISHED" -> Set.of("PAUSED", "ARCHIVED").contains(targetStatus);
            case "PAUSED" -> Set.of("PUBLISHED", "DRAFT", "ARCHIVED").contains(targetStatus);
            case "ARCHIVED" -> false;
            default -> false;
        };
    }

    private boolean isAllowedLaunchTransition(String currentStatus, String targetStatus) {
        String current = currentStatus == null ? "DRAFT" : currentStatus.toUpperCase(Locale.ROOT);
        if (current.equals(targetStatus)) {
            return true;
        }
        return switch (current) {
            case "DRAFT" -> Set.of("ACTIVE", "INACTIVE").contains(targetStatus);
            case "INACTIVE" -> Set.of("ACTIVE", "DRAFT").contains(targetStatus);
            case "ACTIVE" -> "INACTIVE".equals(targetStatus);
            default -> false;
        };
    }

    private String normalize(String value, Set<String> allowed) {
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

    private String resolveUsername(User currentUser) {
        return currentUser == null ? "unknown" : currentUser.getUsername();
    }

    private void logBatchRows(String action, List<String> codes, String status, String reason, User currentUser, String requestUri) {
        String message = "Batch updated " + codes.size() + " assets to " + status + (reason == null ? "" : " (" + reason + ")");
        for (String code : codes.stream().sorted(Comparator.naturalOrder()).toList()) {
            auditLogService.logOpsAudit(action, currentUser, null, code, true, message, requestUri);
        }
    }

    private Result<OpsBatchActionResultDto> batchError(String assetType, String message, User currentUser, String requestUri) {
        auditLogService.logOpsAudit("OPS_MARKETING_BATCH_STATUS", currentUser, null, assetType, false, message, requestUri);
        return Result.error(message);
    }
}
