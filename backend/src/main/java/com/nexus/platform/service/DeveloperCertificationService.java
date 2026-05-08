package com.nexus.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.DeveloperCertificationProfileDto;
import com.nexus.platform.dto.DeveloperCertificationReviewRecordDto;
import com.nexus.platform.dto.DeveloperCertificationUpsertRequest;
import com.nexus.platform.dto.OpsDeveloperCertificationReviewRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.DeveloperCertificationProfile;
import com.nexus.platform.entity.DeveloperCertificationReviewRecord;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.DeveloperCertificationProfileRepository;
import com.nexus.platform.repository.DeveloperCertificationReviewRecordRepository;
import com.nexus.platform.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeveloperCertificationService {
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.{3,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{7,32}$");

    private final DeveloperCertificationProfileRepository profileRepository;
    private final DeveloperCertificationReviewRecordRepository reviewRecordRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public Result<DeveloperCertificationProfileDto> getMyProfile(User currentUser) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.DEVELOPER) {
            return Result.error("Developer account required");
        }
        return Result.success(toDto(loadOrInitProfile(currentUser), currentUser));
    }

    public Result<List<DeveloperCertificationReviewRecordDto>> getMyReviewRecords(User currentUser) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.DEVELOPER) {
            return Result.error("Developer account required");
        }
        return Result.success(listReviewRecords(currentUser.getId()));
    }

    @Transactional
    public Result<DeveloperCertificationProfileDto> saveMyProfile(
            User currentUser,
            DeveloperCertificationUpsertRequest request,
            String requestUri
    ) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.DEVELOPER) {
            return Result.error("Developer account required");
        }
        DeveloperCertificationProfile profile = loadOrInitProfile(currentUser);
        String currentStatus = normalizeProfileStatus(profile.getProfileStatus());
        boolean submit = request != null && Boolean.TRUE.equals(request.submit());
        if ("PENDING".equals(currentStatus)) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_SUBMIT", currentUser, null, "developer:" + currentUser.getId(), false, "Certification is already pending review", requestUri);
            return Result.error("Certification is already pending review");
        }

        ValidationResult validation = validateRequest(request, submit);
        if (validation.errorMessage != null) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_SUBMIT", currentUser, null, "developer:" + currentUser.getId(), false, validation.errorMessage, requestUri);
            return Result.error(validation.errorMessage);
        }

        String beforeStatus = normalizeProfileStatus(profile.getProfileStatus());
        applyProfile(profile, validation);

        if (submit) {
            profile.setProfileStatus("PENDING");
            profile.setSubmittedAt(LocalDateTime.now());
            profile.setReviewedAt(null);
            profile.setRejectionReason(null);
            currentUser.setCertificationStatus("PENDING");
        } else {
            profile.setProfileStatus("DRAFT");
            profile.setReviewedAt(null);
            currentUser.setCertificationStatus("UNVERIFIED");
        }

        profileRepository.save(profile);
        userRepository.save(currentUser);
        recordReview(profile, currentUser, submit ? "SUBMIT" : "SAVE_DRAFT", beforeStatus, profile.getProfileStatus(), validation.note, currentUser.getId());
        auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_SUBMIT", currentUser, null, "developer:" + currentUser.getId(), true, submit ? "Submitted certification profile" : "Saved certification draft", requestUri);
        return Result.success(toDto(profile, currentUser));
    }

    public Result<DeveloperCertificationProfileDto> getDeveloperProfile(Long developerId) {
        User developer = loadDeveloper(developerId);
        if (developer == null) {
            return Result.error("Developer not found");
        }
        return Result.success(toDto(loadOrInitProfile(developer), developer));
    }

    public Result<List<DeveloperCertificationReviewRecordDto>> getDeveloperReviewRecords(Long developerId) {
        if (loadDeveloper(developerId) == null) {
            return Result.error("Developer not found");
        }
        return Result.success(listReviewRecords(developerId));
    }

    @Transactional
    public Result<DeveloperCertificationProfileDto> reviewDeveloperProfile(
            Long developerId,
            OpsDeveloperCertificationReviewRequest request,
            User operator,
            String requestUri
    ) {
        User developer = loadDeveloper(developerId);
        if (developer == null) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_REVIEW", operator, null, "developer:" + developerId, false, "Developer not found", requestUri);
            return Result.error("Developer not found");
        }
        DeveloperCertificationProfile profile = profileRepository.findByDeveloperId(developerId).orElse(null);
        if (profile == null) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_REVIEW", operator, null, "developer:" + developerId, false, "Certification profile not found", requestUri);
            return Result.error("Certification profile not found");
        }
        String currentStatus = normalizeProfileStatus(profile.getProfileStatus());
        if (!"PENDING".equals(currentStatus)) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_REVIEW", operator, null, "developer:" + developerId, false, "Only pending profile can be reviewed", requestUri);
            return Result.error("Only pending profile can be reviewed");
        }

        String decision = normalizeReviewDecision(request == null ? null : request.decision());
        if (decision == null) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_REVIEW", operator, null, "developer:" + developerId, false, "Invalid review decision", requestUri);
            return Result.error("Invalid review decision");
        }
        String reason = trimToNull(request == null ? null : request.reason());
        if ("REJECTED".equals(decision) && (reason == null || reason.length() < 2)) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_REVIEW", operator, null, "developer:" + developerId, false, "Reject reason must be at least 2 chars", requestUri);
            return Result.error("Reject reason must be at least 2 chars");
        }
        if (reason != null && reason.length() > 256) {
            auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_REVIEW", operator, null, "developer:" + developerId, false, "Review reason is too long", requestUri);
            return Result.error("Review reason is too long");
        }

        profile.setProfileStatus(decision);
        profile.setReviewedAt(LocalDateTime.now());
        profile.setRejectionReason("REJECTED".equals(decision) ? reason : null);
        developer.setCertificationStatus(decision);

        profileRepository.save(profile);
        userRepository.save(developer);
        recordReview(profile, developer, "REVIEW_" + decision, currentStatus, decision, reason, operator == null ? null : operator.getId());
        auditLogService.logOpsAudit("DEVELOPER_CERTIFICATION_REVIEW", operator, null, "developer:" + developerId, true, decision + (reason == null ? "" : (": " + reason)), requestUri);
        return Result.success(toDto(profile, developer));
    }

    private User loadDeveloper(Long developerId) {
        if (developerId == null || developerId <= 0) {
            return null;
        }
        User user = userRepository.findById(developerId).orElse(null);
        if (user == null || user.getRole() != User.UserRole.DEVELOPER) {
            return null;
        }
        return user;
    }

    private DeveloperCertificationProfile loadOrInitProfile(User developer) {
        return profileRepository.findByDeveloperId(developer.getId()).orElseGet(() -> {
            DeveloperCertificationProfile profile = new DeveloperCertificationProfile();
            profile.setDeveloperId(developer.getId());
            profile.setProfileStatus(normalizeProfileStatus(developer.getCertificationStatus()).equals("VERIFIED") ? "VERIFIED" : "DRAFT");
            profile.setSubjectType("COMPANY");
            profile.setSubjectName(defaultSubjectName(developer));
            profile.setContactName(defaultSubjectName(developer));
            profile.setContactPhone(trimToNull(developer.getPhone()));
            return profile;
        });
    }

    private String defaultSubjectName(User developer) {
        return trimToNull(developer.getUsername()) == null ? "Developer" : developer.getUsername().trim();
    }

    private ValidationResult validateRequest(DeveloperCertificationUpsertRequest request, boolean submit) {
        if (request == null) {
            return new ValidationResult("Request body is required");
        }
        String subjectType = normalizeSubjectType(request.subjectType());
        if (subjectType == null) {
            return new ValidationResult("Invalid subject type");
        }
        String subjectName = trimWithLength(request.subjectName(), 128);
        String legalRepresentative = trimWithLength(request.legalRepresentative(), 64);
        String contactName = trimWithLength(request.contactName(), 64);
        String contactPhone = trimWithLength(request.contactPhone(), 32);
        String businessLicenseNo = trimWithLength(request.businessLicenseNo(), 64);
        String idDocumentNo = trimWithLength(request.idDocumentNo(), 64);
        String note = trimWithLength(request.note(), 256);
        if (request.subjectName() != null && subjectName == null) {
            return new ValidationResult("Subject name is too long");
        }
        if (request.legalRepresentative() != null && legalRepresentative == null) {
            return new ValidationResult("Legal representative is too long");
        }
        if (request.contactName() != null && contactName == null) {
            return new ValidationResult("Contact name is too long");
        }
        if (request.contactPhone() != null && contactPhone == null) {
            return new ValidationResult("Contact phone is too long");
        }
        if (request.businessLicenseNo() != null && businessLicenseNo == null) {
            return new ValidationResult("Business license no is too long");
        }
        if (request.idDocumentNo() != null && idDocumentNo == null) {
            return new ValidationResult("ID document no is too long");
        }
        if (request.note() != null && note == null) {
            return new ValidationResult("Note is too long");
        }

        List<String> assets = normalizeAssets(request.certificateAssets());
        if (assets == null) {
            return new ValidationResult("Invalid certificate asset urls");
        }

        if (submit) {
            if (subjectName == null || contactName == null || contactPhone == null) {
                return new ValidationResult("Subject name, contact name and phone are required");
            }
            if (!PHONE_PATTERN.matcher(contactPhone).matches()) {
                return new ValidationResult("Invalid contact phone");
            }
            if ("COMPANY".equals(subjectType) && businessLicenseNo == null) {
                return new ValidationResult("Business license no is required for company certification");
            }
            if ("INDIVIDUAL".equals(subjectType) && idDocumentNo == null) {
                return new ValidationResult("ID document no is required for individual certification");
            }
            if (assets.isEmpty()) {
                return new ValidationResult("At least one certificate asset is required");
            }
        } else if (contactPhone != null && !PHONE_PATTERN.matcher(contactPhone).matches()) {
            return new ValidationResult("Invalid contact phone");
        }

        return new ValidationResult(subjectType, subjectName, legalRepresentative, contactName, contactPhone, businessLicenseNo, idDocumentNo, assets, note);
    }

    private void applyProfile(DeveloperCertificationProfile profile, ValidationResult validation) {
        profile.setSubjectType(validation.subjectType);
        profile.setSubjectName(validation.subjectName);
        profile.setLegalRepresentative(validation.legalRepresentative);
        profile.setContactName(validation.contactName);
        profile.setContactPhone(validation.contactPhone);
        profile.setBusinessLicenseNo(validation.businessLicenseNo);
        profile.setIdDocumentNo(validation.idDocumentNo);
        profile.setCertificateAssetJson(writeAssets(validation.assets));
        profile.setNote(validation.note);
    }

    private List<DeveloperCertificationReviewRecordDto> listReviewRecords(Long developerId) {
        return reviewRecordRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId).stream()
                .map(record -> new DeveloperCertificationReviewRecordDto(
                        record.getId(),
                        record.getDeveloperId(),
                        record.getProfileId(),
                        record.getOperatorId(),
                        record.getActionType(),
                        record.getBeforeStatus(),
                        record.getAfterStatus(),
                        record.getReason(),
                        record.getSnapshotJson(),
                        record.getCreatedAt()
                ))
                .toList();
    }

    private DeveloperCertificationProfileDto toDto(DeveloperCertificationProfile profile, User developer) {
        return new DeveloperCertificationProfileDto(
                profile.getId(),
                developer.getId(),
                normalizeProfileStatus(profile.getProfileStatus()),
                normalizeCertificationStatus(developer.getCertificationStatus()),
                normalizeSubjectType(profile.getSubjectType()),
                profile.getSubjectName(),
                profile.getLegalRepresentative(),
                profile.getContactName(),
                profile.getContactPhone(),
                profile.getBusinessLicenseNo(),
                profile.getIdDocumentNo(),
                readAssets(profile.getCertificateAssetJson()),
                profile.getNote(),
                profile.getRejectionReason(),
                profile.getSubmittedAt(),
                profile.getReviewedAt(),
                profile.getUpdatedAt()
        );
    }

    private void recordReview(
            DeveloperCertificationProfile profile,
            User developer,
            String actionType,
            String beforeStatus,
            String afterStatus,
            String reason,
            Long operatorId
    ) {
        DeveloperCertificationReviewRecord record = new DeveloperCertificationReviewRecord();
        record.setDeveloperId(developer.getId());
        record.setProfileId(profile.getId());
        record.setOperatorId(operatorId);
        record.setActionType(actionType);
        record.setBeforeStatus(beforeStatus);
        record.setAfterStatus(afterStatus);
        record.setReason(reason);
        record.setSnapshotJson(writeSnapshot(profile, developer));
        reviewRecordRepository.save(record);
    }

    private String writeSnapshot(DeveloperCertificationProfile profile, User developer) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("profileStatus", normalizeProfileStatus(profile.getProfileStatus()));
        snapshot.put("certificationStatus", normalizeCertificationStatus(developer.getCertificationStatus()));
        snapshot.put("subjectType", normalizeSubjectType(profile.getSubjectType()));
        snapshot.put("subjectName", profile.getSubjectName());
        snapshot.put("contactName", profile.getContactName());
        snapshot.put("contactPhone", profile.getContactPhone());
        snapshot.put("assetCount", readAssets(profile.getCertificateAssetJson()).size());
        snapshot.put("submittedAt", profile.getSubmittedAt());
        snapshot.put("reviewedAt", profile.getReviewedAt());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private List<String> normalizeAssets(List<String> assets) {
        if (assets == null) {
            return List.of();
        }
        if (assets.size() > 6) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String item : assets) {
            String value = trimToNull(item);
            if (value == null) {
                continue;
            }
            if (value.length() > 512 || !URL_PATTERN.matcher(value).matches()) {
                return null;
            }
            normalized.add(value);
        }
        return normalized;
    }

    private String writeAssets(List<String> assets) {
        try {
            return objectMapper.writeValueAsString(assets == null ? List.of() : assets);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> readAssets(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalizeSubjectType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "COMPANY", "INDIVIDUAL" -> normalized;
            default -> null;
        };
    }

    private String normalizeProfileStatus(String value) {
        if (value == null || value.isBlank()) {
            return "DRAFT";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DRAFT", "PENDING", "VERIFIED", "REJECTED" -> normalized;
            case "UNVERIFIED" -> "DRAFT";
            default -> "DRAFT";
        };
    }

    private String normalizeCertificationStatus(String value) {
        if (value == null || value.isBlank()) {
            return "UNVERIFIED";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "UNVERIFIED", "PENDING", "VERIFIED", "REJECTED" -> normalized;
            default -> "UNVERIFIED";
        };
    }

    private String normalizeReviewDecision(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "VERIFIED", "REJECTED" -> normalized;
            default -> null;
        };
    }

    private String trimWithLength(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed != null && trimmed.length() > maxLength) {
            return null;
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class ValidationResult {
        private final String errorMessage;
        private final String subjectType;
        private final String subjectName;
        private final String legalRepresentative;
        private final String contactName;
        private final String contactPhone;
        private final String businessLicenseNo;
        private final String idDocumentNo;
        private final List<String> assets;
        private final String note;

        private ValidationResult(String errorMessage) {
            this.errorMessage = errorMessage;
            this.subjectType = null;
            this.subjectName = null;
            this.legalRepresentative = null;
            this.contactName = null;
            this.contactPhone = null;
            this.businessLicenseNo = null;
            this.idDocumentNo = null;
            this.assets = List.of();
            this.note = null;
        }

        private ValidationResult(
                String subjectType,
                String subjectName,
                String legalRepresentative,
                String contactName,
                String contactPhone,
                String businessLicenseNo,
                String idDocumentNo,
                List<String> assets,
                String note
        ) {
            this.errorMessage = null;
            this.subjectType = subjectType;
            this.subjectName = subjectName;
            this.legalRepresentative = legalRepresentative;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
            this.businessLicenseNo = businessLicenseNo;
            this.idDocumentNo = idDocumentNo;
            this.assets = assets;
            this.note = note;
        }
    }
}
