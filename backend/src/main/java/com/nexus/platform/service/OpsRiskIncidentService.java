package com.nexus.platform.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.OpsRiskIncidentDto;
import com.nexus.platform.dto.OpsRiskIncidentRecordDto;
import com.nexus.platform.dto.OpsRiskIncidentStatusRequest;
import com.nexus.platform.dto.OpsRiskIncidentUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsRiskIncident;
import com.nexus.platform.entity.OpsRiskIncidentRecord;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsRiskIncidentRecordRepository;
import com.nexus.platform.repository.OpsRiskIncidentRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsRiskIncidentService {
    private final OpsRiskIncidentRepository repository;
    private final OpsRiskIncidentRecordRepository recordRepository;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public Result<List<OpsRiskIncidentDto>> list() {
        seedDefaults();
        return Result.success(repository.findAll().stream().sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt())).map(this::toDto).toList());
    }

    public Result<List<OpsRiskIncidentDto>> create(
            OpsRiskIncidentUpsertRequest request,
            User currentUser,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, OpsRiskIncidentDto.class);
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, listType);
        return idempotencyService.execute(
                "OPS_RISK_INCIDENT_CREATE",
                idempotencyKey,
                request,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> createInternal(request, currentUser, requestUri)
        );
    }

    private Result<List<OpsRiskIncidentDto>> createInternal(OpsRiskIncidentUpsertRequest request, User currentUser, String requestUri) {
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("RISK_INCIDENT_CREATE", currentUser, request == null ? null : request.targetGameId(), request == null ? null : request.targetAppId(), false, error, requestUri);
            return Result.error(error);
        }
        OpsRiskIncident row = new OpsRiskIncident();
        apply(row, request);
        repository.save(row);
        appendRecord(row, currentUser, "CREATE", null, row.getStatus(), row.getOwnerNote());
        auditLogService.logOpsAudit(
                "RISK_INCIDENT_CREATE",
                currentUser,
                row.getTargetGameId(),
                row.getTargetAppId(),
                true,
                "Risk incident created: " + row.getTitle(),
                requestUri,
                "RISK_INCIDENT",
                Map.of(),
                snapshot(row)
        );
        return list();
    }

    public Result<List<OpsRiskIncidentDto>> updateStatus(
            Long id,
            OpsRiskIncidentStatusRequest request,
            User currentUser,
            String requestUri,
            String idempotencyKey
    ) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, OpsRiskIncidentDto.class);
        JavaType resultType = objectMapper.getTypeFactory().constructParametricType(Result.class, listType);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", id);
        payload.put("request", request);
        return idempotencyService.execute(
                "OPS_RISK_INCIDENT_STATUS_UPDATE",
                idempotencyKey,
                payload,
                currentUser == null ? null : currentUser.getId(),
                requestUri,
                resultType,
                () -> updateStatusInternal(id, request, currentUser, requestUri)
        );
    }

    private Result<List<OpsRiskIncidentDto>> updateStatusInternal(Long id, OpsRiskIncidentStatusRequest request, User currentUser, String requestUri) {
        OpsRiskIncident row = repository.findById(id).orElse(null);
        if (row == null) {
            auditLogService.logOpsAudit("RISK_INCIDENT_STATUS_UPDATE", currentUser, null, null, false, "Incident not found", requestUri);
            return Result.error("Incident not found");
        }
        String normalizedStatus = normalizeStatus(request == null ? null : request.status());
        if (normalizedStatus == null) {
            auditLogService.logOpsAudit("RISK_INCIDENT_STATUS_UPDATE", currentUser, row.getTargetGameId(), row.getTargetAppId(), false, "Invalid status", requestUri);
            return Result.error("Invalid status");
        }
        String ownerNote = trimToNull(request == null ? null : request.ownerNote());
        if (ownerNote != null && ownerNote.length() > 256) {
            auditLogService.logOpsAudit("RISK_INCIDENT_STATUS_UPDATE", currentUser, row.getTargetGameId(), row.getTargetAppId(), false, "Owner note is too long", requestUri);
            return Result.error("Owner note is too long");
        }
        String assignee = trimToNull(request == null ? null : request.assignee());
        if (assignee != null && assignee.length() > 128) {
            auditLogService.logOpsAudit("RISK_INCIDENT_STATUS_UPDATE", currentUser, row.getTargetGameId(), row.getTargetAppId(), false, "Assignee is too long", requestUri);
            return Result.error("Assignee is too long");
        }
        String resolutionSummary = trimToNull(request == null ? null : request.resolutionSummary());
        if (resolutionSummary != null && resolutionSummary.length() > 256) {
            auditLogService.logOpsAudit("RISK_INCIDENT_STATUS_UPDATE", currentUser, row.getTargetGameId(), row.getTargetAppId(), false, "Resolution summary is too long", requestUri);
            return Result.error("Resolution summary is too long");
        }
        if ("RESOLVED".equals(normalizedStatus) && (resolutionSummary == null || resolutionSummary.length() < 4)) {
            auditLogService.logOpsAudit("RISK_INCIDENT_STATUS_UPDATE", currentUser, row.getTargetGameId(), row.getTargetAppId(), false, "Resolution summary is required", requestUri);
            return Result.error("Resolution summary is required");
        }
        Map<String, Object> beforeSnapshot = snapshot(row);
        String fromStatus = row.getStatus();
        row.setStatus(normalizedStatus);
        row.setOwnerNote(ownerNote);
        row.setAssignee(assignee);
        row.setResolutionSummary(resolutionSummary);
        repository.save(row);
        appendRecord(row, currentUser, "STATUS_UPDATE", fromStatus, normalizedStatus, ownerNote == null ? resolutionSummary : ownerNote);
        auditLogService.logOpsAudit(
                "RISK_INCIDENT_STATUS_UPDATE",
                currentUser,
                row.getTargetGameId(),
                row.getTargetAppId(),
                true,
                "Risk incident set to " + normalizedStatus,
                requestUri,
                "RISK_INCIDENT",
                beforeSnapshot,
                snapshot(row)
        );
        return list();
    }

    public Result<List<OpsRiskIncidentRecordDto>> listRecords(Long incidentId) {
        if (incidentId == null || incidentId <= 0) {
            return Result.error("Invalid incident id");
        }
        return Result.success(
                recordRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                        .map(row -> new OpsRiskIncidentRecordDto(
                                row.getId(),
                                row.getIncidentId(),
                                row.getOperatorId(),
                                row.getActionType(),
                                row.getFromStatus(),
                                row.getToStatus(),
                                row.getNote(),
                                row.getCreatedAt()
                        ))
                        .toList()
        );
    }

    private void seedDefaults() {
        if (repository.count() > 0) {
            return;
        }
        repository.save(seed("ACCOUNT_RISK", "HIGH", "可疑开发者封禁申诉待处理", "开发者反馈误封，需要复核最近治理动作。", "pending-review-app", null));
        repository.save(seed("CONTENT_RISK", "MEDIUM", "发现页素材过期待替换", "发现页顶部投放素材即将到期，需确认替换计划。", "discover-hero", null));
    }

    private OpsRiskIncident seed(String type, String severity, String title, String description, String targetAppId, Long targetGameId) {
        OpsRiskIncident row = new OpsRiskIncident();
        row.setIncidentType(type);
        row.setSeverity(severity);
        row.setStatus("OPEN");
        row.setTitle(title);
        row.setDescription(description);
        row.setTargetAppId(targetAppId);
        row.setTargetGameId(targetGameId);
        return row;
    }

    private String validate(OpsRiskIncidentUpsertRequest request) {
        if (request == null) {
            return "Request body is required";
        }
        if (normalizeType(request.incidentType()) == null) {
            return "Incident type is invalid";
        }
        if (normalizeSeverity(request.severity()) == null) {
            return "Severity is invalid";
        }
        if (request.title() == null || request.title().trim().length() < 2 || request.title().trim().length() > 128) {
            return "Incident title length must be between 2 and 128";
        }
        if (request.description() == null || request.description().trim().length() < 4 || request.description().trim().length() > 2000) {
            return "Incident description length must be between 4 and 2000";
        }
        String ownerNote = trimToNull(request.ownerNote());
        if (ownerNote != null && ownerNote.length() > 256) {
            return "Owner note is too long";
        }
        return null;
    }

    private void apply(OpsRiskIncident row, OpsRiskIncidentUpsertRequest request) {
        row.setIncidentType(normalizeType(request.incidentType()));
        row.setSeverity(normalizeSeverity(request.severity()));
        row.setTitle(request.title().trim());
        row.setDescription(request.description().trim());
        row.setTargetAppId(trimToNull(request.targetAppId()));
        row.setTargetGameId(request.targetGameId());
        row.setOwnerNote(trimToNull(request.ownerNote()));
        row.setAssignee(trimToNull(request.assignee()));
        row.setHandlingDeadline(request.handlingDeadline());
        if (row.getStatus() == null || row.getStatus().isBlank()) {
            row.setStatus("OPEN");
        }
    }

    private OpsRiskIncidentDto toDto(OpsRiskIncident row) {
        return new OpsRiskIncidentDto(
                row.getId(),
                row.getIncidentType(),
                row.getSeverity(),
                row.getStatus(),
                row.getTitle(),
                row.getDescription(),
                row.getTargetAppId(),
                row.getTargetGameId(),
                row.getOwnerNote(),
                row.getAssignee(),
                row.getHandlingDeadline(),
                row.getResolutionSummary(),
                row.getUpdatedAt()
        );
    }

    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "ACCOUNT_RISK", "CONTENT_RISK", "RUNTIME_RISK", "SECURITY_RISK" -> normalized;
            default -> null;
        };
    }

    private String normalizeSeverity(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> normalized;
            default -> null;
        };
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "OPEN", "MITIGATING", "RESOLVED" -> normalized;
            default -> null;
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void appendRecord(OpsRiskIncident incident, User operator, String actionType, String fromStatus, String toStatus, String note) {
        OpsRiskIncidentRecord record = new OpsRiskIncidentRecord();
        record.setIncidentId(incident.getId());
        record.setOperatorId(operator == null ? null : operator.getId());
        record.setActionType(actionType);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setNote(trimToNull(note));
        recordRepository.save(record);
    }

    private Map<String, Object> snapshot(OpsRiskIncident row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", row.getId());
        snapshot.put("incidentType", row.getIncidentType());
        snapshot.put("severity", row.getSeverity());
        snapshot.put("status", row.getStatus());
        snapshot.put("title", row.getTitle());
        snapshot.put("targetAppId", row.getTargetAppId());
        snapshot.put("targetGameId", row.getTargetGameId());
        snapshot.put("ownerNote", row.getOwnerNote());
        snapshot.put("assignee", row.getAssignee());
        snapshot.put("handlingDeadline", row.getHandlingDeadline());
        snapshot.put("resolutionSummary", row.getResolutionSummary());
        snapshot.put("descriptionPreview", row.getDescription() == null
                ? null
                : row.getDescription().substring(0, Math.min(120, row.getDescription().length())));
        return snapshot;
    }
}
