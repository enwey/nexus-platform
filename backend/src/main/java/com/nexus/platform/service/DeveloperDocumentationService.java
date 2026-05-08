package com.nexus.platform.service;

import com.nexus.platform.dto.DeveloperDocArticleDto;
import com.nexus.platform.dto.DeveloperDocArticleUpsertRequest;
import com.nexus.platform.dto.DeveloperDocArticleVersionDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.DeveloperDocArticle;
import com.nexus.platform.entity.DeveloperDocArticleVersion;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.DeveloperDocArticleRepository;
import com.nexus.platform.repository.DeveloperDocArticleVersionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeveloperDocumentationService {
    private static final Set<String> DOC_TYPES = Set.of("GUIDE", "CHECKLIST", "CASE");
    private static final Set<String> ARTICLE_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    private final DeveloperDocArticleRepository articleRepository;
    private final DeveloperDocArticleVersionRepository versionRepository;
    private final AuditLogService auditLogService;

    public Result<List<DeveloperDocArticleDto>> listArticles(User currentUser, String docType) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        String normalizedType = normalizeEnum(docType, DOC_TYPES);
        List<DeveloperDocArticle> rows = normalizedType == null && docType != null && !docType.isBlank()
                ? List.of()
                : (normalizedType == null
                ? articleRepository.findByDeveloperIdOrderByUpdatedAtDesc(currentUser.getId())
                : articleRepository.findByDeveloperIdAndDocTypeOrderByUpdatedAtDesc(currentUser.getId(), normalizedType));
        return Result.success(rows.stream().map(this::toArticleDto).toList());
    }

    public Result<List<DeveloperDocArticleVersionDto>> listVersions(User currentUser, Long articleId) {
        if (currentUser == null) {
            return Result.error("Missing valid login token");
        }
        DeveloperDocArticle article = articleRepository.findByIdAndDeveloperId(articleId, currentUser.getId()).orElse(null);
        if (article == null) {
            return Result.error("Document not found");
        }
        return Result.success(versionRepository.findByArticleIdOrderByVersionNoDesc(articleId).stream().map(this::toVersionDto).toList());
    }

    @Transactional
    public Result<DeveloperDocArticleDto> createArticle(User currentUser, DeveloperDocArticleUpsertRequest request, String requestUri) {
        ValidationResult validation = validateRequest(request);
        if (!validation.success) {
            auditLogService.logOpsAudit("DEV_DOC_CREATE", currentUser, null, "developer:" + currentUser.getId(), false, validation.message, requestUri);
            return Result.error(validation.message);
        }
        DeveloperDocArticle article = new DeveloperDocArticle();
        article.setDeveloperId(currentUser.getId());
        article.setDocType(validation.docType);
        article.setTitle(validation.title);
        article.setCategory(validation.category);
        article.setArticleStatus(validation.articleStatus);
        article.setSummary(validation.summary);
        article.setTagsJson(validation.tagsCsv);
        article.setCurrentVersion(1);
        article.setLatestChangeNote(validation.changeNote);
        if ("PUBLISHED".equals(validation.articleStatus)) {
            article.setPublishedAt(LocalDateTime.now());
        }
        DeveloperDocArticle saved = articleRepository.save(article);
        versionRepository.save(buildVersion(saved, 1, validation.title, validation.contentMarkdown, validation.changeNote, currentUser.getId()));
        auditLogService.logOpsAudit("DEV_DOC_CREATE", currentUser, null, "doc:" + saved.getId(), true, saved.getTitle(), requestUri);
        return Result.success(toArticleDto(saved));
    }

    @Transactional
    public Result<DeveloperDocArticleDto> updateArticle(User currentUser, Long articleId, DeveloperDocArticleUpsertRequest request, String requestUri) {
        DeveloperDocArticle article = articleRepository.findByIdAndDeveloperId(articleId, currentUser.getId()).orElse(null);
        if (article == null) {
            auditLogService.logOpsAudit("DEV_DOC_UPDATE", currentUser, null, "doc:" + articleId, false, "Document not found", requestUri);
            return Result.error("Document not found");
        }
        ValidationResult validation = validateRequest(request);
        if (!validation.success) {
            auditLogService.logOpsAudit("DEV_DOC_UPDATE", currentUser, null, "doc:" + articleId, false, validation.message, requestUri);
            return Result.error(validation.message);
        }
        article.setDocType(validation.docType);
        article.setTitle(validation.title);
        article.setCategory(validation.category);
        article.setArticleStatus(validation.articleStatus);
        article.setSummary(validation.summary);
        article.setTagsJson(validation.tagsCsv);
        article.setLatestChangeNote(validation.changeNote);
        if ("PUBLISHED".equals(validation.articleStatus) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        if (!"PUBLISHED".equals(validation.articleStatus)) {
            article.setPublishedAt(null);
        }
        int nextVersion = (article.getCurrentVersion() == null ? 0 : article.getCurrentVersion()) + 1;
        article.setCurrentVersion(nextVersion);
        DeveloperDocArticle saved = articleRepository.save(article);
        versionRepository.save(buildVersion(saved, nextVersion, validation.title, validation.contentMarkdown, validation.changeNote, currentUser.getId()));
        auditLogService.logOpsAudit("DEV_DOC_UPDATE", currentUser, null, "doc:" + saved.getId(), true, saved.getTitle(), requestUri);
        return Result.success(toArticleDto(saved));
    }

    private ValidationResult validateRequest(DeveloperDocArticleUpsertRequest request) {
        String docType = normalizeEnum(request == null ? null : request.docType(), DOC_TYPES);
        if (docType == null) {
            return ValidationResult.error("Invalid document type");
        }
        String title = trim(request == null ? null : request.title(), 80);
        if (title == null || title.length() < 2) {
            return ValidationResult.error("Document title must be at least 2 characters");
        }
        String articleStatus = normalizeEnum(request == null ? null : request.articleStatus(), ARTICLE_STATUSES);
        if (articleStatus == null) {
            articleStatus = "DRAFT";
        }
        String category = trim(request == null ? null : request.category(), 64);
        String summary = trim(request == null ? null : request.summary(), 200);
        String contentMarkdown = request == null || request.contentMarkdown() == null ? null : request.contentMarkdown().trim();
        if (contentMarkdown == null || contentMarkdown.length() < 10) {
            return ValidationResult.error("Document content must be at least 10 characters");
        }
        if (contentMarkdown.length() > 20000) {
            return ValidationResult.error("Document content is too long");
        }
        String changeNote = trim(request == null ? null : request.changeNote(), 120);
        List<String> tags = normalizeTags(request == null ? null : request.tags());
        return ValidationResult.success(docType, title, category, articleStatus, summary, contentMarkdown, changeNote, String.join(",", tags));
    }

    private List<String> normalizeTags(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        for (String item : input) {
            String normalized = trim(item, 24);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }
            tags.add(normalized);
            if (tags.size() >= 10) {
                break;
            }
        }
        return new ArrayList<>(tags);
    }

    private DeveloperDocArticleVersion buildVersion(DeveloperDocArticle article, int versionNo, String title, String contentMarkdown, String changeNote, Long createdBy) {
        DeveloperDocArticleVersion version = new DeveloperDocArticleVersion();
        version.setArticleId(article.getId());
        version.setVersionNo(versionNo);
        version.setTitleSnapshot(title);
        version.setContentMarkdown(contentMarkdown);
        version.setChangeNote(changeNote);
        version.setCreatedBy(createdBy);
        return version;
    }

    private DeveloperDocArticleDto toArticleDto(DeveloperDocArticle article) {
        return new DeveloperDocArticleDto(
                article.getId(),
                article.getDocType(),
                article.getTitle(),
                article.getCategory(),
                article.getArticleStatus(),
                article.getSummary(),
                splitCsv(article.getTagsJson()),
                article.getCurrentVersion(),
                article.getLatestChangeNote(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private DeveloperDocArticleVersionDto toVersionDto(DeveloperDocArticleVersion version) {
        return new DeveloperDocArticleVersionDto(
                version.getId(),
                version.getArticleId(),
                version.getVersionNo(),
                version.getTitleSnapshot(),
                version.getContentMarkdown(),
                version.getChangeNote(),
                version.getCreatedBy(),
                version.getCreatedAt()
        );
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
    }

    private String normalizeEnum(String raw, Set<String> allowed) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    private static final class ValidationResult {
        private final boolean success;
        private final String message;
        private final String docType;
        private final String title;
        private final String category;
        private final String articleStatus;
        private final String summary;
        private final String contentMarkdown;
        private final String changeNote;
        private final String tagsCsv;

        private ValidationResult(boolean success, String message, String docType, String title, String category, String articleStatus, String summary, String contentMarkdown, String changeNote, String tagsCsv) {
            this.success = success;
            this.message = message;
            this.docType = docType;
            this.title = title;
            this.category = category;
            this.articleStatus = articleStatus;
            this.summary = summary;
            this.contentMarkdown = contentMarkdown;
            this.changeNote = changeNote;
            this.tagsCsv = tagsCsv;
        }

        private static ValidationResult error(String message) {
            return new ValidationResult(false, message, null, null, null, null, null, null, null, null);
        }

        private static ValidationResult success(String docType, String title, String category, String articleStatus, String summary, String contentMarkdown, String changeNote, String tagsCsv) {
            return new ValidationResult(true, null, docType, title, category, articleStatus, summary, contentMarkdown, changeNote, tagsCsv);
        }
    }
}
