package com.nexus.platform.service;

import com.nexus.platform.entity.AuditLog;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.AuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final GameRepository gameRepository;

    public void logGameAudit(String action, User operator, Game targetGame, boolean success, String reason, String requestUri) {
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
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs(int limit) {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
        List<AuditLog> logs = auditLogRepository.findTop200ByOrderByCreatedAtDesc();
        return logs.stream().limit(limit).toList();
    }

    public Game getGameById(Long id) {
        return gameRepository.findById(id).orElse(null);
    }
}
