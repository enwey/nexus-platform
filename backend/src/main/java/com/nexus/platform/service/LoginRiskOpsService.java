package com.nexus.platform.service;

import com.nexus.platform.dto.OpsAccessControlRuleDto;
import com.nexus.platform.dto.OpsAccessControlRuleUpsertRequest;
import com.nexus.platform.dto.OpsLoginRiskEventDto;
import com.nexus.platform.dto.OpsRiskRuleConfigDto;
import com.nexus.platform.dto.OpsRiskRuleConfigUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsAccessControlRule;
import com.nexus.platform.entity.OpsLoginRiskEvent;
import com.nexus.platform.entity.OpsRiskRuleConfig;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsAccessControlRuleRepository;
import com.nexus.platform.repository.OpsLoginRiskEventRepository;
import com.nexus.platform.repository.OpsRiskRuleConfigRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginRiskOpsService {
    private static final Set<String> RULE_TYPES = Set.of("IP_BLOCK", "DEVICE_BLOCK", "IP_WATCH", "DEVICE_WATCH");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> ACTION_TYPES = Set.of("ALERT", "LOCK", "ESCALATE");
    private static final String RULE_LOGIN_FAILURE_SPIKE = "LOGIN_FAILURE_SPIKE";
    private static final String RULE_LOGIN_IP_BURST = "LOGIN_IP_BURST";
    private static final String RULE_LOGIN_WATCH_HIT = "LOGIN_WATCH_HIT";

    private final OpsLoginRiskEventRepository eventRepository;
    private final OpsAccessControlRuleRepository ruleRepository;
    private final OpsRiskRuleConfigRepository riskRuleConfigRepository;
    private final AuditLogService auditLogService;

    @PostConstruct
    void seedDefaults() {
        seedRule(RULE_LOGIN_FAILURE_SPIKE, "Login Failure Spike", "HIGH", 5, 15, "LOCK", "Lock account+IP tuple after too many failures");
        seedRule(RULE_LOGIN_IP_BURST, "Login IP Burst", "HIGH", 10, 15, "ALERT", "Alert on repeated login attempts from same IP");
        seedRule(RULE_LOGIN_WATCH_HIT, "Watch Rule Hit", "CRITICAL", 1, 60, "ESCALATE", "Escalate when watched IP/device logs in");
    }

    public void recordEvent(String loginId, Long userId, String clientIp, String deviceId, String userAgent, String eventType, String severity, String result, String reason) {
        OpsLoginRiskEvent event = new OpsLoginRiskEvent();
        event.setLoginId(trim(loginId, 128));
        event.setUserId(userId);
        event.setClientIp(trim(clientIp, 64));
        event.setDeviceId(trim(deviceId, 128));
        event.setUserAgent(trim(userAgent, 512));
        event.setEventType(normalize(eventType, 32));
        event.setSeverity(normalizeSeverity(severity));
        event.setResult(normalize(result, 16));
        event.setReason(trim(reason, 256));
        eventRepository.save(event);
    }

    public Result<List<OpsLoginRiskEventDto>> listEvents() {
        return Result.success(eventRepository.findTop200ByOrderByCreatedAtDesc().stream().map(this::toEventDto).toList());
    }

    public Result<List<OpsAccessControlRuleDto>> listRules() {
        return Result.success(ruleRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toRuleDto).toList());
    }

    public Result<List<OpsRiskRuleConfigDto>> listRiskRules() {
        return Result.success(riskRuleConfigRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toRiskRuleDto).toList());
    }

    public Result<OpsAccessControlRuleDto> upsertRule(OpsAccessControlRuleUpsertRequest request, User currentUser, String requestUri) {
        String ruleType = normalizeEnum(request == null ? null : request.ruleType(), RULE_TYPES);
        if (ruleType == null) {
            auditLogService.logOpsAudit("ACCESS_RULE_UPSERT", currentUser, null, "ACCESS_RULE", false, "Invalid rule type", requestUri);
            return Result.error("Invalid rule type");
        }
        String targetValue = trim(request == null ? null : request.targetValue(), 128);
        if (targetValue == null || targetValue.length() < 2) {
            auditLogService.logOpsAudit("ACCESS_RULE_UPSERT", currentUser, null, "ACCESS_RULE", false, "Invalid target value", requestUri);
            return Result.error("Invalid target value");
        }
        String status = normalizeEnum(request == null ? null : request.status(), STATUSES);
        if (status == null) {
            status = "ACTIVE";
        }
        String riskLevel = normalizeEnum(request == null ? null : request.riskLevel(), RISK_LEVELS);
        if (riskLevel == null) {
            riskLevel = "HIGH";
        }
        String note = trim(request == null ? null : request.note(), 256);
        OpsAccessControlRule entity = ruleRepository.findByRuleTypeAndTargetValue(ruleType, targetValue).orElseGet(OpsAccessControlRule::new);
        entity.setRuleType(ruleType);
        entity.setTargetValue(targetValue);
        entity.setStatus(status);
        entity.setRiskLevel(riskLevel);
        entity.setNote(note);
        entity.setExpiresAt(request == null ? null : request.expiresAt());
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        OpsAccessControlRule saved = ruleRepository.save(entity);
        auditLogService.logOpsAudit("ACCESS_RULE_UPSERT", currentUser, null, "ACCESS_RULE:" + saved.getId(), true, ruleType + ":" + targetValue, requestUri);
        return Result.success(toRuleDto(saved));
    }

    public Result<OpsRiskRuleConfigDto> upsertRiskRule(OpsRiskRuleConfigUpsertRequest request, User currentUser, String requestUri) {
        String ruleCode = normalizeRiskRuleCode(request == null ? null : request.ruleCode());
        if (ruleCode == null) {
            auditLogService.logOpsAudit("RISK_RULE_UPSERT", currentUser, null, "RISK_RULE", false, "Invalid rule code", requestUri);
            return Result.error("Invalid rule code");
        }
        String ruleName = trim(request == null ? null : request.ruleName(), 128);
        if (ruleName == null || ruleName.length() < 2) {
            auditLogService.logOpsAudit("RISK_RULE_UPSERT", currentUser, null, "RISK_RULE", false, "Invalid rule name", requestUri);
            return Result.error("Invalid rule name");
        }
        Integer thresholdCount = request == null ? null : request.thresholdCount();
        Integer windowMinutes = request == null ? null : request.windowMinutes();
        if (thresholdCount == null || thresholdCount < 1 || windowMinutes == null || windowMinutes < 1) {
            auditLogService.logOpsAudit("RISK_RULE_UPSERT", currentUser, null, "RISK_RULE", false, "Invalid threshold or window", requestUri);
            return Result.error("Invalid threshold or window");
        }
        String severity = normalizeSeverity(request == null ? null : request.severity());
        String actionType = normalizeEnum(request == null ? null : request.actionType(), ACTION_TYPES);
        if (actionType == null) {
            actionType = "ALERT";
        }
        OpsRiskRuleConfig entity = riskRuleConfigRepository.findByRuleCode(ruleCode).orElseGet(OpsRiskRuleConfig::new);
        entity.setRuleCode(ruleCode);
        entity.setRuleName(ruleName);
        entity.setEnabled(request == null || request.enabled() == null ? Boolean.TRUE : request.enabled());
        entity.setSeverity(severity);
        entity.setThresholdCount(thresholdCount);
        entity.setWindowMinutes(windowMinutes);
        entity.setActionType(actionType);
        entity.setNote(trim(request == null ? null : request.note(), 256));
        entity.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        OpsRiskRuleConfig saved = riskRuleConfigRepository.save(entity);
        auditLogService.logOpsAudit("RISK_RULE_UPSERT", currentUser, null, "RISK_RULE:" + saved.getRuleCode(), true, saved.getRuleName(), requestUri);
        return Result.success(toRiskRuleDto(saved));
    }

    public String resolveAccessBlockReason(String clientIp, String deviceId) {
        LocalDateTime now = LocalDateTime.now();
        List<OpsAccessControlRule> rules = ruleRepository.findAllByOrderByUpdatedAtDesc();
        for (OpsAccessControlRule rule : rules) {
            if (!"ACTIVE".equals(rule.getStatus())) {
                continue;
            }
            if (rule.getExpiresAt() != null && rule.getExpiresAt().isBefore(now)) {
                continue;
            }
            if ("IP_BLOCK".equals(rule.getRuleType()) && clientIp != null && clientIp.equals(rule.getTargetValue())) {
                return "IP access is blocked by risk control";
            }
            if ("DEVICE_BLOCK".equals(rule.getRuleType()) && deviceId != null && deviceId.equals(rule.getTargetValue())) {
                return "Device access is blocked by risk control";
            }
        }
        return null;
    }

    public boolean isWatched(String clientIp, String deviceId) {
        LocalDateTime now = LocalDateTime.now();
        return ruleRepository.findAllByOrderByUpdatedAtDesc().stream().anyMatch(rule ->
                "ACTIVE".equals(rule.getStatus())
                        && (rule.getExpiresAt() == null || !rule.getExpiresAt().isBefore(now))
                        && (("IP_WATCH".equals(rule.getRuleType()) && clientIp != null && clientIp.equals(rule.getTargetValue()))
                        || ("DEVICE_WATCH".equals(rule.getRuleType()) && deviceId != null && deviceId.equals(rule.getTargetValue())))
        );
    }

    public boolean isHighFrequencyIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        OpsRiskRuleConfig config = getRiskRule(RULE_LOGIN_IP_BURST);
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return false;
        }
        return eventRepository.countByClientIpAndCreatedAtAfter(
                clientIp,
                LocalDateTime.now().minusMinutes(config.getWindowMinutes())
        ) >= config.getThresholdCount();
    }

    public int getFailureThreshold() {
        OpsRiskRuleConfig config = getRiskRule(RULE_LOGIN_FAILURE_SPIKE);
        return config == null || !Boolean.TRUE.equals(config.getEnabled()) ? 5 : Math.max(config.getThresholdCount(), 1);
    }

    public Duration getFailureWindow() {
        OpsRiskRuleConfig config = getRiskRule(RULE_LOGIN_FAILURE_SPIKE);
        int minutes = config == null || !Boolean.TRUE.equals(config.getEnabled()) ? 15 : Math.max(config.getWindowMinutes(), 1);
        return Duration.ofMinutes(minutes);
    }

    public String getFailureSeverity() {
        OpsRiskRuleConfig config = getRiskRule(RULE_LOGIN_FAILURE_SPIKE);
        return config == null ? "HIGH" : config.getSeverity();
    }

    public String getWatchSeverity() {
        OpsRiskRuleConfig config = getRiskRule(RULE_LOGIN_WATCH_HIT);
        return config == null ? "CRITICAL" : config.getSeverity();
    }

    public void recordHighFrequencyAlert(String loginId, Long userId, String clientIp, String deviceId, String userAgent) {
        OpsRiskRuleConfig config = getRiskRule(RULE_LOGIN_IP_BURST);
        if (config == null || !Boolean.TRUE.equals(config.getEnabled()) || clientIp == null || clientIp.isBlank()) {
            return;
        }
        long count = eventRepository.countByClientIpAndCreatedAtAfter(
                clientIp,
                LocalDateTime.now().minusMinutes(config.getWindowMinutes())
        );
        if (count == config.getThresholdCount()) {
            recordEvent(loginId, userId, clientIp, deviceId, userAgent, "LOGIN_IP_BURST", config.getSeverity(), "ALERT", "High-frequency login attempts from same IP");
        }
    }

    private OpsLoginRiskEventDto toEventDto(OpsLoginRiskEvent row) {
        return new OpsLoginRiskEventDto(row.getId(), row.getLoginId(), row.getUserId(), row.getClientIp(), row.getDeviceId(), row.getUserAgent(), row.getEventType(), row.getSeverity(), row.getResult(), row.getReason(), row.getCreatedAt());
    }

    private OpsAccessControlRuleDto toRuleDto(OpsAccessControlRule row) {
        return new OpsAccessControlRuleDto(row.getId(), row.getRuleType(), row.getTargetValue(), row.getStatus(), row.getRiskLevel(), row.getNote(), row.getExpiresAt(), row.getUpdatedBy(), row.getUpdatedAt());
    }

    private OpsRiskRuleConfigDto toRiskRuleDto(OpsRiskRuleConfig row) {
        return new OpsRiskRuleConfigDto(
                row.getId(),
                row.getRuleCode(),
                row.getRuleName(),
                row.getEnabled(),
                row.getSeverity(),
                row.getThresholdCount(),
                row.getWindowMinutes(),
                row.getActionType(),
                row.getNote(),
                row.getUpdatedBy(),
                row.getUpdatedAt()
        );
    }

    private OpsRiskRuleConfig getRiskRule(String ruleCode) {
        return riskRuleConfigRepository.findByRuleCode(ruleCode).orElse(null);
    }

    private String normalizeRiskRuleCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z0-9_]{4,64}") ? normalized : null;
    }

    private String normalizeEnum(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
    }

    private String normalizeSeverity(String value) {
        String normalized = normalizeEnum(value, RISK_LEVELS);
        return normalized == null ? "MEDIUM" : normalized;
    }

    private String normalize(String value, int maxLength) {
        String trimmed = trim(value, maxLength);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trim(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private void seedRule(String ruleCode, String ruleName, String severity, int thresholdCount, int windowMinutes, String actionType, String note) {
        if (riskRuleConfigRepository.findByRuleCode(ruleCode).isPresent()) {
            return;
        }
        OpsRiskRuleConfig config = new OpsRiskRuleConfig();
        config.setRuleCode(ruleCode);
        config.setRuleName(ruleName);
        config.setEnabled(true);
        config.setSeverity(severity);
        config.setThresholdCount(thresholdCount);
        config.setWindowMinutes(windowMinutes);
        config.setActionType(actionType);
        config.setNote(note);
        config.setUpdatedBy("system");
        riskRuleConfigRepository.save(config);
    }
}
