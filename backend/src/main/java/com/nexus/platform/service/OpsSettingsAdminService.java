package com.nexus.platform.service;

import com.nexus.platform.dto.OpsAdminDataScopeDto;
import com.nexus.platform.dto.OpsAdminDataScopeUpdateRequest;
import com.nexus.platform.dto.OpsApprovalTemplateDto;
import com.nexus.platform.dto.OpsApprovalTemplateUpsertRequest;
import com.nexus.platform.dto.OpsDictionaryEntryDto;
import com.nexus.platform.dto.OpsDictionaryEntryUpsertRequest;
import com.nexus.platform.dto.OpsMenuPermissionProfileDto;
import com.nexus.platform.dto.OpsMenuPermissionProfileUpsertRequest;
import com.nexus.platform.dto.OpsNoticeTemplateDto;
import com.nexus.platform.dto.OpsNoticeTemplateUpsertRequest;
import com.nexus.platform.dto.OpsPermissionMatrixDto;
import com.nexus.platform.dto.OpsRolePermissionUpdateRequest;
import com.nexus.platform.dto.OpsSensitivePolicyDto;
import com.nexus.platform.dto.OpsSensitivePolicyUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.OpsApprovalTemplate;
import com.nexus.platform.entity.OpsDictionaryEntry;
import com.nexus.platform.entity.OpsMenuPermissionProfile;
import com.nexus.platform.entity.OpsNoticeTemplate;
import com.nexus.platform.entity.OpsSensitivePolicy;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.OpsApprovalTemplateRepository;
import com.nexus.platform.repository.OpsDictionaryEntryRepository;
import com.nexus.platform.repository.OpsMenuPermissionProfileRepository;
import com.nexus.platform.repository.OpsNoticeTemplateRepository;
import com.nexus.platform.repository.OpsSensitivePolicyRepository;
import com.nexus.platform.security.Permission;
import com.nexus.platform.security.RolePermissionService;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsSettingsAdminService {
    private static final java.util.Set<String> NOTICE_CHANNELS = java.util.Set.of("INBOX", "EMAIL", "SMS", "PUSH");
    private static final java.util.Set<String> ENTRY_STATUSES = java.util.Set.of("ENABLED", "DISABLED");
    private static final java.util.Set<String> APPROVAL_BIZ_TYPES = java.util.Set.of("GAME_REVIEW", "DISCOVER_PUBLISH", "MARKETING_PUBLISH", "RUNTIME_CHANGE");
    private static final java.util.Set<String> APPROVAL_MODES = java.util.Set.of("SINGLE_REVIEWER", "DUAL_REVIEW", "OWNER_AND_APPROVER");
    private static final java.util.Set<String> ROLE_CODES = java.util.Set.of("ADMIN", "OPS", "REVIEWER", "DEVELOPER", "PLAYER");
    private static final java.util.Set<String> MENU_CODES = java.util.Set.of(
            "DASHBOARD", "REVIEWS", "GAMES", "RECOMMEND", "PUBLISH", "RUNTIME", "MARKETING", "DEVELOPERS", "RISK", "SETTINGS"
    );
    private static final java.util.Set<String> RISK_LEVELS = java.util.Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final java.util.Set<String> POLICY_SCOPE_TYPES = java.util.Set.of("ACTION", "MODULE", "RESOURCE");

    private final RolePermissionService rolePermissionService;
    private final OpsAdminDataScopeService opsAdminDataScopeService;
    private final OpsNoticeTemplateRepository opsNoticeTemplateRepository;
    private final OpsDictionaryEntryRepository opsDictionaryEntryRepository;
    private final OpsMenuPermissionProfileRepository opsMenuPermissionProfileRepository;
    private final OpsApprovalTemplateRepository opsApprovalTemplateRepository;
    private final OpsSensitivePolicyRepository opsSensitivePolicyRepository;
    private final AuditLogService auditLogService;

    public OpsPermissionMatrixDto getPermissionMatrix() {
        List<String> availablePermissions = rolePermissionService.getAllPermissions().stream()
                .map(Permission::name)
                .sorted()
                .toList();
        List<OpsPermissionMatrixDto.RolePermissionItem> roles = rolePermissionService.getRolePermissionsSnapshot().entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(entry -> new OpsPermissionMatrixDto.RolePermissionItem(
                        entry.getKey().name(),
                        entry.getValue().stream().map(Permission::name).sorted().toList(),
                        rolePermissionService.isRoleEditable(entry.getKey())
                ))
                .toList();
        return new OpsPermissionMatrixDto(availablePermissions, roles);
    }

    public Result<OpsPermissionMatrixDto.RolePermissionItem> updateRolePermissions(
            String roleCode,
            OpsRolePermissionUpdateRequest request,
            User currentUser
    ) {
        User.UserRole role;
        try {
            role = User.UserRole.valueOf(roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Result.error("Invalid role code");
        }
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        if (request != null && request.permissions() != null) {
            for (String raw : request.permissions()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                try {
                    permissions.add(Permission.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return Result.error("Invalid permission code: " + raw);
                }
            }
        }
        rolePermissionService.replaceRolePermissions(role, permissions, currentUser == null ? "unknown" : currentUser.getUsername());
        List<String> effectivePermissions = rolePermissionService.getRolePermissionsSnapshot().get(role).stream()
                .map(Permission::name)
                .sorted()
                .toList();
        return Result.success(new OpsPermissionMatrixDto.RolePermissionItem(role.name(), effectivePermissions, true));
    }

    public List<OpsAdminDataScopeDto> listAdminDataScopes() {
        return opsAdminDataScopeService.listAdminScopes();
    }

    public Result<OpsAdminDataScopeDto> updateAdminDataScope(Long adminUserId, OpsAdminDataScopeUpdateRequest request) {
        return opsAdminDataScopeService.updateAdminScope(adminUserId, request);
    }

    public List<OpsNoticeTemplateDto> listNoticeTemplates() {
        seedOpsConfigs();
        return opsNoticeTemplateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toNoticeTemplateDto)
                .toList();
    }

    public Result<OpsNoticeTemplateDto> upsertNoticeTemplate(Long id, OpsNoticeTemplateUpsertRequest request, User currentUser, String requestUri) {
        String code = normalizeCode(request == null ? null : request.templateCode(), 64);
        String name = trim(request == null ? null : request.templateName(), 128);
        String channel = normalizeEnum(request == null ? null : request.channelType(), NOTICE_CHANNELS);
        String languageTag = trim(request == null ? null : request.languageTag(), 16);
        String titleTemplate = trim(request == null ? null : request.titleTemplate(), 256);
        String bodyTemplate = trim(request == null ? null : request.bodyTemplate(), 2000);
        String status = normalizeEnum(request == null ? null : request.status(), ENTRY_STATUSES);
        if (code == null || name == null || channel == null || languageTag == null || titleTemplate == null || bodyTemplate == null || status == null) {
            return Result.error("Invalid notice template payload");
        }
        OpsNoticeTemplate row = id == null ? null : opsNoticeTemplateRepository.findById(id).orElse(null);
        if (row == null) {
            row = opsNoticeTemplateRepository.findByTemplateCodeAndLanguageTag(code, languageTag).orElseGet(OpsNoticeTemplate::new);
        }
        row.setTemplateCode(code);
        row.setTemplateName(name);
        row.setChannelType(channel);
        row.setLanguageTag(languageTag);
        row.setTitleTemplate(titleTemplate);
        row.setBodyTemplate(bodyTemplate);
        row.setStatus(status);
        row.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        OpsNoticeTemplate saved = opsNoticeTemplateRepository.save(row);
        auditLogService.logOpsAudit("OPS_NOTICE_TEMPLATE_UPSERT", currentUser, null, code, true, status, requestUri);
        return Result.success(toNoticeTemplateDto(saved));
    }

    public List<OpsDictionaryEntryDto> listDictionaryEntries() {
        seedOpsConfigs();
        return opsDictionaryEntryRepository.findAllByOrderByDictTypeAscSortOrderAscUpdatedAtDesc().stream()
                .map(this::toDictionaryEntryDto)
                .toList();
    }

    public Result<OpsDictionaryEntryDto> upsertDictionaryEntry(Long id, OpsDictionaryEntryUpsertRequest request, User currentUser, String requestUri) {
        String dictType = normalizeCode(request == null ? null : request.dictType(), 64);
        String dictKey = normalizeCode(request == null ? null : request.dictKey(), 64);
        String dictLabel = trim(request == null ? null : request.dictLabel(), 128);
        String dictValue = trim(request == null ? null : request.dictValue(), 256);
        String status = normalizeEnum(request == null ? null : request.status(), ENTRY_STATUSES);
        Integer sortOrder = request == null || request.sortOrder() == null ? 0 : Math.max(0, request.sortOrder());
        if (dictType == null || dictKey == null || dictLabel == null || status == null) {
            return Result.error("Invalid dictionary payload");
        }
        OpsDictionaryEntry row = id == null ? null : opsDictionaryEntryRepository.findById(id).orElse(null);
        if (row == null) {
            row = opsDictionaryEntryRepository.findByDictTypeAndDictKey(dictType, dictKey).orElseGet(OpsDictionaryEntry::new);
        }
        row.setDictType(dictType);
        row.setDictKey(dictKey);
        row.setDictLabel(dictLabel);
        row.setDictValue(dictValue);
        row.setSortOrder(sortOrder);
        row.setStatus(status);
        row.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        OpsDictionaryEntry saved = opsDictionaryEntryRepository.save(row);
        auditLogService.logOpsAudit("OPS_DICTIONARY_ENTRY_UPSERT", currentUser, null, dictType + ":" + dictKey, true, status, requestUri);
        return Result.success(toDictionaryEntryDto(saved));
    }

    public List<OpsMenuPermissionProfileDto> listMenuPermissionProfiles() {
        seedOpsConfigs();
        return opsMenuPermissionProfileRepository.findAllByOrderByRoleCodeAscSortOrderAscUpdatedAtDesc().stream()
                .map(this::toMenuPermissionProfileDto)
                .toList();
    }

    public Result<OpsMenuPermissionProfileDto> upsertMenuPermissionProfile(Long id, OpsMenuPermissionProfileUpsertRequest request, User currentUser, String requestUri) {
        String roleCode = normalizeEnum(request == null ? null : request.roleCode(), ROLE_CODES);
        String menuCode = normalizeEnum(request == null ? null : request.menuCode(), MENU_CODES);
        String menuLabel = trim(request == null ? null : request.menuLabel(), 128);
        Integer sortOrder = request == null || request.sortOrder() == null ? 0 : Math.max(0, request.sortOrder());
        if (roleCode == null || menuCode == null || menuLabel == null) {
            return Result.error("Invalid menu permission payload");
        }
        OpsMenuPermissionProfile row = id == null ? null : opsMenuPermissionProfileRepository.findById(id).orElse(null);
        if (row == null) {
            row = opsMenuPermissionProfileRepository.findByRoleCodeAndMenuCode(roleCode, menuCode).orElseGet(OpsMenuPermissionProfile::new);
        }
        row.setRoleCode(roleCode);
        row.setMenuCode(menuCode);
        row.setMenuLabel(menuLabel);
        row.setEnabled(request == null || request.enabled() == null ? Boolean.TRUE : request.enabled());
        row.setSortOrder(sortOrder);
        row.setNote(trim(request == null ? null : request.note(), 256));
        row.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        OpsMenuPermissionProfile saved = opsMenuPermissionProfileRepository.save(row);
        auditLogService.logOpsAudit("OPS_MENU_PERMISSION_UPSERT", currentUser, null, roleCode + ":" + menuCode, true, String.valueOf(saved.getEnabled()), requestUri);
        return Result.success(toMenuPermissionProfileDto(saved));
    }

    public List<OpsApprovalTemplateDto> listApprovalTemplates() {
        seedOpsConfigs();
        return opsApprovalTemplateRepository.findAllByOrderByBizTypeAscUpdatedAtDesc().stream()
                .map(this::toApprovalTemplateDto)
                .toList();
    }

    public Result<OpsApprovalTemplateDto> upsertApprovalTemplate(Long id, OpsApprovalTemplateUpsertRequest request, User currentUser, String requestUri) {
        String templateCode = normalizeCode(request == null ? null : request.templateCode(), 64);
        String templateName = trim(request == null ? null : request.templateName(), 128);
        String bizType = normalizeEnum(request == null ? null : request.bizType(), APPROVAL_BIZ_TYPES);
        String approvalMode = normalizeEnum(request == null ? null : request.approvalMode(), APPROVAL_MODES);
        String reviewerRole = normalizeEnum(request == null ? null : request.reviewerRole(), ROLE_CODES);
        String status = normalizeEnum(request == null ? null : request.status(), ENTRY_STATUSES);
        String stepConfig = trim(request == null ? null : request.stepConfig(), 2000);
        if (templateCode == null || templateName == null || bizType == null || approvalMode == null || reviewerRole == null || status == null) {
            return Result.error("Invalid approval template payload");
        }
        OpsApprovalTemplate row = id == null ? null : opsApprovalTemplateRepository.findById(id).orElse(null);
        if (row == null) {
            row = opsApprovalTemplateRepository.findByTemplateCode(templateCode).orElseGet(OpsApprovalTemplate::new);
        }
        row.setTemplateCode(templateCode);
        row.setTemplateName(templateName);
        row.setBizType(bizType);
        row.setApprovalMode(approvalMode);
        row.setReviewerRole(reviewerRole);
        row.setStepConfig(stepConfig);
        row.setStatus(status);
        row.setNote(trim(request == null ? null : request.note(), 256));
        row.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        OpsApprovalTemplate saved = opsApprovalTemplateRepository.save(row);
        auditLogService.logOpsAudit("OPS_APPROVAL_TEMPLATE_UPSERT", currentUser, null, templateCode, true, bizType + ":" + approvalMode, requestUri);
        return Result.success(toApprovalTemplateDto(saved));
    }

    public List<OpsSensitivePolicyDto> listSensitivePolicies() {
        seedOpsConfigs();
        return opsSensitivePolicyRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSensitivePolicyDto)
                .toList();
    }

    public Result<OpsSensitivePolicyDto> upsertSensitivePolicy(Long id, OpsSensitivePolicyUpsertRequest request, User currentUser, String requestUri) {
        String policyCode = normalizeCode(request == null ? null : request.policyCode(), 64);
        String policyName = trim(request == null ? null : request.policyName(), 128);
        String riskLevel = normalizeEnum(request == null ? null : request.riskLevel(), RISK_LEVELS);
        String scopeType = normalizeEnum(request == null ? null : request.scopeType(), POLICY_SCOPE_TYPES);
        String status = normalizeEnum(request == null ? null : request.status(), ENTRY_STATUSES);
        if (policyCode == null || policyName == null || riskLevel == null || scopeType == null || status == null) {
            return Result.error("Invalid sensitive policy payload");
        }
        OpsSensitivePolicy row = id == null ? null : opsSensitivePolicyRepository.findById(id).orElse(null);
        if (row == null) {
            row = opsSensitivePolicyRepository.findByPolicyCode(policyCode).orElseGet(OpsSensitivePolicy::new);
        }
        row.setPolicyCode(policyCode);
        row.setPolicyName(policyName);
        row.setRiskLevel(riskLevel);
        row.setConfirmRequired(request == null || request.confirmRequired() == null || request.confirmRequired());
        row.setAuditRequired(request == null || request.auditRequired() == null || request.auditRequired());
        row.setScopeType(scopeType);
        row.setTargetActions(trim(request == null ? null : request.targetActions(), 4000));
        row.setStatus(status);
        row.setNote(trim(request == null ? null : request.note(), 256));
        row.setUpdatedBy(currentUser == null ? "unknown" : currentUser.getUsername());
        OpsSensitivePolicy saved = opsSensitivePolicyRepository.save(row);
        auditLogService.logOpsAudit("OPS_SENSITIVE_POLICY_UPSERT", currentUser, null, policyCode, true, riskLevel, requestUri);
        return Result.success(toSensitivePolicyDto(saved));
    }

    private void seedOpsConfigs() {
        if (opsNoticeTemplateRepository.count() == 0) {
            OpsNoticeTemplate inbox = new OpsNoticeTemplate();
            inbox.setTemplateCode("REVIEW_RESULT");
            inbox.setTemplateName("审核结果通知");
            inbox.setChannelType("INBOX");
            inbox.setLanguageTag("zh-CN");
            inbox.setTitleTemplate("审核结果通知");
            inbox.setBodyTemplate("你的应用 {{appName}} 已完成审核，结果：{{result}}。");
            inbox.setStatus("ENABLED");
            inbox.setUpdatedBy("system");
            opsNoticeTemplateRepository.save(inbox);
        }
        if (opsDictionaryEntryRepository.count() == 0) {
            saveDictionarySeed("AUDIENCE_TYPE", "ALL", "全部用户", "ALL", 0);
            saveDictionarySeed("AUDIENCE_TYPE", "NEW_USER", "新用户", "NEW_USER", 1);
            saveDictionarySeed("RISK_LEVEL", "HIGH", "高风险", "HIGH", 0);
            saveDictionarySeed("RISK_LEVEL", "MEDIUM", "中风险", "MEDIUM", 1);
        }
        if (opsMenuPermissionProfileRepository.count() == 0) {
            saveMenuSeed("OPS", "DASHBOARD", "运营总览", 0);
            saveMenuSeed("OPS", "REVIEWS", "审核中心", 1);
            saveMenuSeed("OPS", "GAMES", "游戏治理", 2);
            saveMenuSeed("OPS", "RECOMMEND", "推荐运营", 3);
            saveMenuSeed("OPS", "PUBLISH", "发布中心", 4);
            saveMenuSeed("OPS", "RISK", "风控中心", 5);
        }
        if (opsApprovalTemplateRepository.count() == 0) {
            saveApprovalSeed("GAME_REVIEW_STANDARD", "游戏审核标准流", "GAME_REVIEW", "SINGLE_REVIEWER", "REVIEWER", "checklist,content,risk");
            saveApprovalSeed("DISCOVER_PUBLISH_DUAL", "推荐位发布双审", "DISCOVER_PUBLISH", "DUAL_REVIEW", "OPS", "owner,approver");
        }
        if (opsSensitivePolicyRepository.count() == 0) {
            saveSensitivePolicySeed("GAME_BLOCK_POLICY", "游戏封禁二次确认", "CRITICAL", true, true, "ACTION", "GAME_VISIBILITY_UPDATE,BATCH_GAME_VISIBILITY_UPDATE");
            saveSensitivePolicySeed("PUBLISH_EXECUTE_POLICY", "发布执行安全策略", "HIGH", true, true, "ACTION", "OPS_PUBLISH_ORDER_EXECUTE,OPS_PUBLISH_ORDER_CANCEL");
        }
    }

    private void saveDictionarySeed(String type, String key, String label, String value, int sortOrder) {
        OpsDictionaryEntry row = new OpsDictionaryEntry();
        row.setDictType(type);
        row.setDictKey(key);
        row.setDictLabel(label);
        row.setDictValue(value);
        row.setSortOrder(sortOrder);
        row.setStatus("ENABLED");
        row.setUpdatedBy("system");
        opsDictionaryEntryRepository.save(row);
    }

    private OpsNoticeTemplateDto toNoticeTemplateDto(OpsNoticeTemplate row) {
        return new OpsNoticeTemplateDto(
                row.getId(),
                row.getTemplateCode(),
                row.getTemplateName(),
                row.getChannelType(),
                row.getLanguageTag(),
                row.getTitleTemplate(),
                row.getBodyTemplate(),
                row.getStatus(),
                row.getUpdatedBy(),
                row.getUpdatedAt()
        );
    }

    private OpsDictionaryEntryDto toDictionaryEntryDto(OpsDictionaryEntry row) {
        return new OpsDictionaryEntryDto(
                row.getId(),
                row.getDictType(),
                row.getDictKey(),
                row.getDictLabel(),
                row.getDictValue(),
                row.getSortOrder(),
                row.getStatus(),
                row.getUpdatedBy(),
                row.getUpdatedAt()
        );
    }

    private OpsMenuPermissionProfileDto toMenuPermissionProfileDto(OpsMenuPermissionProfile row) {
        return new OpsMenuPermissionProfileDto(
                row.getId(),
                row.getRoleCode(),
                row.getMenuCode(),
                row.getMenuLabel(),
                row.getEnabled(),
                row.getSortOrder(),
                row.getNote(),
                row.getUpdatedBy(),
                row.getUpdatedAt()
        );
    }

    private OpsApprovalTemplateDto toApprovalTemplateDto(OpsApprovalTemplate row) {
        return new OpsApprovalTemplateDto(
                row.getId(),
                row.getTemplateCode(),
                row.getTemplateName(),
                row.getBizType(),
                row.getApprovalMode(),
                row.getReviewerRole(),
                row.getStepConfig(),
                row.getStatus(),
                row.getNote(),
                row.getUpdatedBy(),
                row.getUpdatedAt()
        );
    }

    private OpsSensitivePolicyDto toSensitivePolicyDto(OpsSensitivePolicy row) {
        return new OpsSensitivePolicyDto(
                row.getId(),
                row.getPolicyCode(),
                row.getPolicyName(),
                row.getRiskLevel(),
                row.getConfirmRequired(),
                row.getAuditRequired(),
                row.getScopeType(),
                row.getTargetActions(),
                row.getStatus(),
                row.getNote(),
                row.getUpdatedBy(),
                row.getUpdatedAt()
        );
    }

    private String normalizeEnum(String value, java.util.Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
    }

    private String normalizeCode(String value, int maxLength) {
        String trimmed = trim(value, maxLength);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT).replace('-', '_');
        return normalized.matches("^[A-Z0-9_]+$") ? normalized : null;
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

    private void saveMenuSeed(String roleCode, String menuCode, String menuLabel, int sortOrder) {
        OpsMenuPermissionProfile row = new OpsMenuPermissionProfile();
        row.setRoleCode(roleCode);
        row.setMenuCode(menuCode);
        row.setMenuLabel(menuLabel);
        row.setEnabled(true);
        row.setSortOrder(sortOrder);
        row.setUpdatedBy("system");
        opsMenuPermissionProfileRepository.save(row);
    }

    private void saveApprovalSeed(String code, String name, String bizType, String mode, String reviewerRole, String stepConfig) {
        OpsApprovalTemplate row = new OpsApprovalTemplate();
        row.setTemplateCode(code);
        row.setTemplateName(name);
        row.setBizType(bizType);
        row.setApprovalMode(mode);
        row.setReviewerRole(reviewerRole);
        row.setStepConfig(stepConfig);
        row.setStatus("ENABLED");
        row.setUpdatedBy("system");
        opsApprovalTemplateRepository.save(row);
    }

    private void saveSensitivePolicySeed(String code, String name, String riskLevel, boolean confirmRequired, boolean auditRequired, String scopeType, String targetActions) {
        OpsSensitivePolicy row = new OpsSensitivePolicy();
        row.setPolicyCode(code);
        row.setPolicyName(name);
        row.setRiskLevel(riskLevel);
        row.setConfirmRequired(confirmRequired);
        row.setAuditRequired(auditRequired);
        row.setScopeType(scopeType);
        row.setTargetActions(targetActions);
        row.setStatus("ENABLED");
        row.setUpdatedBy("system");
        opsSensitivePolicyRepository.save(row);
    }
}
