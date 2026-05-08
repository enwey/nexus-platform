package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.AuditLogItemDto;
import com.nexus.platform.dto.AuditFieldDiffDto;
import com.nexus.platform.entity.AuditLog;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.AuditLogRepository;
import com.nexus.platform.repository.GameRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final GameRepository gameRepository;
    private final AuditSnapshotService auditSnapshotService;
    private final ObjectMapper objectMapper;

    public void logGameAudit(String action, User operator, Game targetGame, boolean success, String reason, String requestUri) {
        logGameAudit(action, operator, targetGame, success, reason, requestUri, null, null, null);
    }

    public void logGameAudit(
            String action,
            User operator,
            Game targetGame,
            boolean success,
            String reason,
            String requestUri,
            String snapshotType,
            java.util.Map<String, Object> beforeSnapshot,
            java.util.Map<String, Object> afterSnapshot
    ) {
        if (operator == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setOperatorId(operator.getId());
        log.setOperatorRole(operator.getRole().name());
        if (targetGame != null) {
            log.setTargetGameId(targetGame.getId());
            log.setTargetAppId(targetGame.getAppId());
        }
        log.setSuccess(success);
        log.setReason(reason);
        log.setRequestUri(requestUri);
        applySnapshotPayload(log, snapshotType, beforeSnapshot, afterSnapshot);
        auditLogRepository.save(log);
    }

    public void logOpsAudit(
            String action,
            User operator,
            Long targetGameId,
            String targetAppId,
            boolean success,
            String reason,
            String requestUri
    ) {
        logOpsAudit(action, operator, targetGameId, targetAppId, success, reason, requestUri, null, null, null);
    }

    public void logOpsAudit(
            String action,
            User operator,
            Long targetGameId,
            String targetAppId,
            boolean success,
            String reason,
            String requestUri,
            String snapshotType,
            java.util.Map<String, Object> beforeSnapshot,
            java.util.Map<String, Object> afterSnapshot
    ) {
        if (operator == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setOperatorId(operator.getId());
        log.setOperatorRole(operator.getRole().name());
        log.setTargetGameId(targetGameId);
        log.setTargetAppId(targetAppId);
        log.setSuccess(success);
        log.setReason(reason);
        log.setRequestUri(requestUri);
        applySnapshotPayload(log, snapshotType, beforeSnapshot, afterSnapshot);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs(int limit) {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    public List<AuditLogItemDto> searchLogs(
            String action,
            Boolean success,
            Long operatorId,
            String targetAppId,
            int limit
    ) {
        int safeLimit = limit <= 0 || limit > 200 ? 50 : limit;
        String normalizedAction = normalizeAction(action);
        String normalizedAppId = normalizeAppId(targetAppId);
        if (operatorId != null && operatorId <= 0) {
            return List.of();
        }

        Specification<AuditLog> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedAction != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), normalizedAction));
            }
            if (success != null) {
                predicates.add(criteriaBuilder.equal(root.get("success"), success));
            }
            if (operatorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("operatorId"), operatorId));
            }
            if (normalizedAppId != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("targetAppId")),
                        normalizedAppId.toLowerCase(Locale.ROOT)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(
                        specification,
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .map(this::toItem)
                .toList();
    }

    public Game getGameById(Long id) {
        return gameRepository.findById(id).orElse(null);
    }

    private AuditLogItemDto toItem(AuditLog row) {
        java.util.Map<String, Object> beforeSnapshot = auditSnapshotService.parseObject(row.getBeforeSnapshotJson());
        java.util.Map<String, Object> afterSnapshot = auditSnapshotService.parseObject(row.getAfterSnapshotJson());
        List<AuditFieldDiffDto> fieldDiffs = parseDiffs(row.getDiffJson(), beforeSnapshot, afterSnapshot);
        return new AuditLogItemDto(
                row.getId(),
                row.getAction(),
                row.getOperatorId(),
                row.getOperatorRole(),
                row.getTargetGameId(),
                row.getTargetAppId(),
                row.isSuccess(),
                row.getReason(),
                row.getRequestUri(),
                row.getSnapshotType(),
                beforeSnapshot,
                afterSnapshot,
                fieldDiffs,
                row.getCreatedAt()
        );
    }

    private void applySnapshotPayload(
            AuditLog log,
            String snapshotType,
            java.util.Map<String, Object> beforeSnapshot,
            java.util.Map<String, Object> afterSnapshot
    ) {
        if ((beforeSnapshot == null || beforeSnapshot.isEmpty()) && (afterSnapshot == null || afterSnapshot.isEmpty())) {
            return;
        }
        log.setSnapshotType(snapshotType);
        log.setBeforeSnapshotJson(auditSnapshotService.toJson(beforeSnapshot == null ? java.util.Map.of() : beforeSnapshot));
        log.setAfterSnapshotJson(auditSnapshotService.toJson(afterSnapshot == null ? java.util.Map.of() : afterSnapshot));
        log.setDiffJson(auditSnapshotService.toJson(auditSnapshotService.buildDiffs(beforeSnapshot, afterSnapshot)));
    }

    private List<AuditFieldDiffDto> parseDiffs(
            String diffJson,
            java.util.Map<String, Object> beforeSnapshot,
            java.util.Map<String, Object> afterSnapshot
    ) {
        if (diffJson == null || diffJson.isBlank()) {
            return auditSnapshotService.buildDiffs(beforeSnapshot, afterSnapshot);
        }
        try {
            return objectMapper.readValue(
                    diffJson,
                    new TypeReference<List<AuditFieldDiffDto>>() {
                    }
            );
        } catch (Exception exception) {
            return auditSnapshotService.buildDiffs(beforeSnapshot, afterSnapshot);
        }
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        String value = action.trim().toUpperCase(Locale.ROOT);
        if (value.length() > 64) {
            return null;
        }
        return value;
    }

    private String normalizeAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        String value = appId.trim();
        if (value.length() > 64) {
            return null;
        }
        return value;
    }
}
