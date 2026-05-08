package com.nexus.platform.service;

import com.nexus.platform.dto.OpsTaskCampaignDto;
import com.nexus.platform.dto.OpsTaskCampaignStatusRequest;
import com.nexus.platform.dto.OpsTaskCampaignUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsTaskCampaign;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsTaskCampaignRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsTaskCampaignService {
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "PAUSED", "ARCHIVED");
    private static final Set<String> AUDIENCES = Set.of("ALL", "NEW_USER", "RETURNING_USER", "DEVELOPER");
    private static final Set<String> TASK_TYPES = Set.of("LOGIN", "SHARE", "PLAY_GAME", "INVITE", "PAY");
    private static final Set<String> REWARD_TYPES = Set.of("COUPON", "POINTS", "GIFT_PACK", "BADGE");

    private final OpsTaskCampaignRepository repository;
    private final AuditLogService auditLogService;

    public Result<List<OpsTaskCampaignDto>> list() {
        return Result.success(repository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList());
    }

    @Transactional
    public Result<OpsTaskCampaignDto> create(OpsTaskCampaignUpsertRequest request, User currentUser, String requestUri) {
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_TASK_CREATE", currentUser, null, "TASK", false, error, requestUri);
            return Result.error(error);
        }
        OpsTaskCampaign entity = new OpsTaskCampaign();
        entity.setTaskCode(generateCode());
        applyRequest(entity, request, currentUser);
        OpsTaskCampaign saved = repository.save(entity);
        auditLogService.logOpsAudit("OPS_TASK_CREATE", currentUser, null, saved.getTaskCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsTaskCampaignDto> update(Long id, OpsTaskCampaignUpsertRequest request, User currentUser, String requestUri) {
        OpsTaskCampaign entity = repository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_TASK_UPDATE", currentUser, null, "TASK", false, "Task campaign not found", requestUri);
            return Result.error("Task campaign not found");
        }
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_TASK_UPDATE", currentUser, null, entity.getTaskCode(), false, error, requestUri);
            return Result.error(error);
        }
        applyRequest(entity, request, currentUser);
        OpsTaskCampaign saved = repository.save(entity);
        auditLogService.logOpsAudit("OPS_TASK_UPDATE", currentUser, null, saved.getTaskCode(), true, saved.getTitle(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<OpsTaskCampaignDto> updateStatus(Long id, OpsTaskCampaignStatusRequest request, User currentUser, String requestUri) {
        OpsTaskCampaign entity = repository.findById(id).orElse(null);
        if (entity == null) {
            auditLogService.logOpsAudit("OPS_TASK_STATUS_UPDATE", currentUser, null, "TASK", false, "Task campaign not found", requestUri);
            return Result.error("Task campaign not found");
        }
        String status = normalize(request == null ? null : request.status(), STATUSES);
        if (status == null) {
            auditLogService.logOpsAudit("OPS_TASK_STATUS_UPDATE", currentUser, null, entity.getTaskCode(), false, "Invalid task status", requestUri);
            return Result.error("Invalid task status");
        }
        entity.setStatus(status);
        entity.setUpdatedBy(resolveUsername(currentUser));
        OpsTaskCampaign saved = repository.save(entity);
        auditLogService.logOpsAudit("OPS_TASK_STATUS_UPDATE", currentUser, null, saved.getTaskCode(), true, status, requestUri);
        return Result.success(toDto(saved));
    }

    private void applyRequest(OpsTaskCampaign entity, OpsTaskCampaignUpsertRequest request, User currentUser) {
        entity.setTitle(request.title().trim());
        entity.setStatus(normalize(request.status(), STATUSES));
        entity.setAudience(normalize(request.audience(), AUDIENCES));
        entity.setTaskType(normalize(request.taskType(), TASK_TYPES));
        entity.setRewardType(normalize(request.rewardType(), REWARD_TYPES));
        entity.setRewardValue(request.rewardValue().trim());
        entity.setLandingUrl(trim(request.landingUrl(), 512));
        entity.setPriority(request.priority() == null ? 0 : Math.max(0, request.priority()));
        entity.setDailyLimit(request.dailyLimit() == null ? 1 : Math.max(1, request.dailyLimit()));
        entity.setNote(trim(request.note(), 256));
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setUpdatedBy(resolveUsername(currentUser));
    }

    private String validate(OpsTaskCampaignUpsertRequest request) {
        if (request == null) return "Request body is required";
        if (normalize(request.status(), STATUSES) == null) return "Invalid task status";
        if (normalize(request.audience(), AUDIENCES) == null) return "Invalid task audience";
        if (normalize(request.taskType(), TASK_TYPES) == null) return "Invalid task type";
        if (normalize(request.rewardType(), REWARD_TYPES) == null) return "Invalid reward type";
        String title = trim(request.title(), 128);
        if (title == null || title.length() < 2) return "Task title is invalid";
        String rewardValue = trim(request.rewardValue(), 128);
        if (rewardValue == null || rewardValue.length() < 1) return "Task reward value is invalid";
        if (!isValidSchedule(request.startAt(), request.endAt())) return "Task schedule is invalid";
        if (!isValidUrl(request.landingUrl())) return "landingUrl is invalid";
        return null;
    }

    private OpsTaskCampaignDto toDto(OpsTaskCampaign entity) {
        return new OpsTaskCampaignDto(
                entity.getId(),
                entity.getTaskCode(),
                entity.getTitle(),
                entity.getStatus(),
                deriveEffectiveStatus(entity),
                entity.getAudience(),
                entity.getTaskType(),
                entity.getRewardType(),
                entity.getRewardValue(),
                entity.getLandingUrl(),
                entity.getPriority(),
                entity.getDailyLimit(),
                entity.getNote(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String deriveEffectiveStatus(OpsTaskCampaign entity) {
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
        return "TASK_" + System.currentTimeMillis();
    }
}
