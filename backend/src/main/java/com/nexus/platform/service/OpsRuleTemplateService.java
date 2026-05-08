package com.nexus.platform.service;

import com.nexus.platform.dto.OpsRuleTemplateDto;
import com.nexus.platform.dto.OpsRuleTemplateUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsRuleTemplate;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsRuleTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsRuleTemplateService {
    private static final String STATUS_ENABLED = "ENABLED";

    private final OpsRuleTemplateRepository repository;
    private final AuditLogService auditLogService;

    public Result<List<OpsRuleTemplateDto>> list() {
        seedDefaults();
        return Result.success(repository.findByStatusOrderByTemplateTypeAscSortOrderAscUpdatedAtDesc(STATUS_ENABLED)
                .stream()
                .map(this::toDto)
                .toList());
    }

    public Result<List<OpsRuleTemplateDto>> create(OpsRuleTemplateUpsertRequest request, User currentUser, String requestUri) {
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_RULE_TEMPLATE_CREATE", currentUser, null, null, false, error, requestUri);
            return Result.error(error);
        }
        OpsRuleTemplate row = new OpsRuleTemplate();
        apply(row, request);
        repository.save(row);
        auditLogService.logOpsAudit("OPS_RULE_TEMPLATE_CREATE", currentUser, null, null, true, "Template created: " + row.getTitle(), requestUri);
        return list();
    }

    public Result<List<OpsRuleTemplateDto>> update(Long id, OpsRuleTemplateUpsertRequest request, User currentUser, String requestUri) {
        OpsRuleTemplate row = repository.findById(id).orElse(null);
        if (row == null || !STATUS_ENABLED.equals(row.getStatus())) {
            auditLogService.logOpsAudit("OPS_RULE_TEMPLATE_UPDATE", currentUser, null, null, false, "Template not found", requestUri);
            return Result.error("Template not found");
        }
        String error = validate(request);
        if (error != null) {
            auditLogService.logOpsAudit("OPS_RULE_TEMPLATE_UPDATE", currentUser, null, null, false, error, requestUri);
            return Result.error(error);
        }
        apply(row, request);
        repository.save(row);
        auditLogService.logOpsAudit("OPS_RULE_TEMPLATE_UPDATE", currentUser, null, null, true, "Template updated: " + row.getTitle(), requestUri);
        return list();
    }

    public Result<List<OpsRuleTemplateDto>> delete(Long id, User currentUser, String requestUri) {
        OpsRuleTemplate row = repository.findById(id).orElse(null);
        if (row == null || !STATUS_ENABLED.equals(row.getStatus())) {
            auditLogService.logOpsAudit("OPS_RULE_TEMPLATE_DELETE", currentUser, null, null, false, "Template not found", requestUri);
            return Result.error("Template not found");
        }
        row.setStatus("DELETED");
        repository.save(row);
        auditLogService.logOpsAudit("OPS_RULE_TEMPLATE_DELETE", currentUser, null, null, true, "Template deleted: " + row.getTitle(), requestUri);
        return list();
    }

    private void seedDefaults() {
        if (!repository.findByStatusOrderByTemplateTypeAscSortOrderAscUpdatedAtDesc(STATUS_ENABLED).isEmpty()) {
            return;
        }
        repository.save(seed("APPROVAL", "审核通过模板", "素材合法、玩法清晰、无阻断问题。", 0));
        repository.save(seed("REJECTION", "驳回原因模板", "请补齐包体结构、素材版权、敏感内容或入口异常说明。", 1));
        repository.save(seed("PRELAUNCH", "上线前检查模板", "确认推荐位素材、运行时配置、强更策略与回滚预案。", 2));
    }

    private OpsRuleTemplate seed(String type, String title, String content, int sortOrder) {
        OpsRuleTemplate row = new OpsRuleTemplate();
        row.setTemplateType(type);
        row.setTitle(title);
        row.setContent(content);
        row.setSortOrder(sortOrder);
        row.setStatus(STATUS_ENABLED);
        return row;
    }

    private String validate(OpsRuleTemplateUpsertRequest request) {
        if (request == null) {
            return "Request body is required";
        }
        String type = normalizeType(request.templateType());
        if (type == null) {
            return "Template type is invalid";
        }
        if (request.title() == null || request.title().trim().length() < 2 || request.title().trim().length() > 128) {
            return "Template title length must be between 2 and 128";
        }
        if (request.content() == null || request.content().trim().length() < 4 || request.content().trim().length() > 2000) {
            return "Template content length must be between 4 and 2000";
        }
        return null;
    }

    private void apply(OpsRuleTemplate row, OpsRuleTemplateUpsertRequest request) {
        row.setTemplateType(normalizeType(request.templateType()));
        row.setTitle(request.title().trim());
        row.setContent(request.content().trim());
        row.setSortOrder(request.sortOrder() == null ? 0 : Math.max(0, request.sortOrder()));
        row.setStatus(STATUS_ENABLED);
    }

    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "APPROVAL", "REJECTION", "PRELAUNCH" -> normalized;
            default -> null;
        };
    }

    private OpsRuleTemplateDto toDto(OpsRuleTemplate row) {
        return new OpsRuleTemplateDto(
                row.getId(),
                row.getTemplateType(),
                row.getTitle(),
                row.getContent(),
                row.getStatus(),
                row.getSortOrder(),
                row.getUpdatedAt()
        );
    }
}
