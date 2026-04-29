package com.nexus.platform.service;

import com.nexus.platform.dto.LaunchAdDtos.LaunchAdAdminItem;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdAdminListResponse;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdPublicResponse;
import com.nexus.platform.dto.LaunchAdDtos.LaunchAdUpsertRequest;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.LaunchAd;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.LaunchAdRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LaunchAdService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final LaunchAdRepository launchAdRepository;

    @Transactional
    public Result<LaunchAdAdminListResponse> listAdminAds() {
        LaunchAdPublicResponse active = launchAdRepository.findFirstByStatusOrderByUpdatedAtDesc(STATUS_ACTIVE)
                .map(this::toPublicResponse)
                .orElse(null);
        List<LaunchAdAdminItem> items = launchAdRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toAdminItem)
                .toList();
        return Result.success(new LaunchAdAdminListResponse(active, items));
    }

    @Transactional
    public Result<LaunchAdAdminItem> create(LaunchAdUpsertRequest request, User currentUser) {
        String validationError = validate(request);
        if (validationError != null) {
            return Result.error(validationError);
        }
        LaunchAd entity = new LaunchAd();
        applyRequest(entity, request, currentUser, true);
        LaunchAd saved = launchAdRepository.save(entity);
        if (STATUS_ACTIVE.equals(saved.getStatus())) {
            deactivateOthers(saved.getId());
        }
        return Result.success(toAdminItem(saved));
    }

    @Transactional
    public Result<LaunchAdAdminItem> update(Long id, LaunchAdUpsertRequest request, User currentUser) {
        String validationError = validate(request);
        if (validationError != null) {
            return Result.error(validationError);
        }
        LaunchAd entity = launchAdRepository.findById(id).orElse(null);
        if (entity == null) {
            return Result.error("Launch ad not found");
        }
        applyRequest(entity, request, currentUser, false);
        LaunchAd saved = launchAdRepository.save(entity);
        if (STATUS_ACTIVE.equals(saved.getStatus())) {
            deactivateOthers(saved.getId());
        }
        return Result.success(toAdminItem(saved));
    }

    @Transactional
    public Result<LaunchAdAdminItem> activate(Long id, User currentUser) {
        LaunchAd entity = launchAdRepository.findById(id).orElse(null);
        if (entity == null) {
            return Result.error("Launch ad not found");
        }
        deactivateOthers(id);
        entity.setStatus(STATUS_ACTIVE);
        entity.setUpdatedBy(resolveUsername(currentUser));
        LaunchAd saved = launchAdRepository.save(entity);
        return Result.success(toAdminItem(saved));
    }

    @Transactional
    public Result<LaunchAdPublicResponse> currentActive() {
        LaunchAd entity = launchAdRepository.findFirstByStatusOrderByUpdatedAtDesc(STATUS_ACTIVE).orElse(null);
        return Result.success(entity == null ? null : toPublicResponse(entity));
    }

    private void deactivateOthers(Long activeId) {
        List<LaunchAd> activeRows = launchAdRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> STATUS_ACTIVE.equals(item.getStatus()))
                .filter(item -> item.getId() != null && item.getId().equals(activeId) == false)
                .toList();
        for (LaunchAd row : activeRows) {
            row.setStatus(STATUS_INACTIVE);
            launchAdRepository.save(row);
        }
    }

    private void applyRequest(LaunchAd entity, LaunchAdUpsertRequest request, User currentUser, boolean creating) {
        String nextImageUrl = request.imageUrl().trim();
        boolean imageChanged = creating || entity.getImageUrl() == null || entity.getImageUrl().equals(nextImageUrl) == false;
        entity.setImageUrl(nextImageUrl);
        entity.setTargetUrl(request.targetUrl().trim());
        entity.setDisplaySeconds(request.displaySeconds() == null ? 4 : Math.max(1, request.displaySeconds()));
        entity.setSponsorZhCn(trimToEmpty(request.sponsorZhCn()));
        entity.setSponsorZhTw(trimToEmpty(request.sponsorZhTw()));
        entity.setSponsorEn(trimToEmpty(request.sponsorEn()));
        entity.setTitleZhCn(trimToEmpty(request.titleZhCn()));
        entity.setTitleZhTw(trimToEmpty(request.titleZhTw()));
        entity.setTitleEn(trimToEmpty(request.titleEn()));
        entity.setDescriptionZhCn(trimToEmpty(request.descriptionZhCn()));
        entity.setDescriptionZhTw(trimToEmpty(request.descriptionZhTw()));
        entity.setDescriptionEn(trimToEmpty(request.descriptionEn()));
        entity.setCtaZhCn(trimToEmpty(request.ctaZhCn()));
        entity.setCtaZhTw(trimToEmpty(request.ctaZhTw()));
        entity.setCtaEn(trimToEmpty(request.ctaEn()));
        entity.setFooterZhCn(trimToEmpty(request.footerZhCn()));
        entity.setFooterZhTw(trimToEmpty(request.footerZhTw()));
        entity.setFooterEn(trimToEmpty(request.footerEn()));
        entity.setUpdatedBy(resolveUsername(currentUser));

        String requestedStatus = normalizeStatus(request.status());
        if (requestedStatus != null) {
            entity.setStatus(requestedStatus);
        } else if (creating && (entity.getStatus() == null || entity.getStatus().isBlank())) {
            entity.setStatus(STATUS_DRAFT);
        }

        if (request.imageVersion() != null && request.imageVersion().isBlank() == false) {
            entity.setImageVersion(request.imageVersion().trim());
        } else if (creating || imageChanged) {
            entity.setImageVersion(String.valueOf(System.currentTimeMillis()));
        }
    }

    private String validate(LaunchAdUpsertRequest request) {
        if (request == null) {
            return "Request body is required";
        }
        if (request.imageUrl() == null || request.imageUrl().isBlank()) {
            return "imageUrl is required";
        }
        if (request.targetUrl() == null || request.targetUrl().isBlank()) {
            return "targetUrl is required";
        }
        if (request.titleZhCn() == null || request.titleZhCn().isBlank()) {
            return "titleZhCn is required";
        }
        if (request.titleZhTw() == null || request.titleZhTw().isBlank()) {
            return "titleZhTw is required";
        }
        if (request.titleEn() == null || request.titleEn().isBlank()) {
            return "titleEn is required";
        }
        return null;
    }

    private LaunchAdPublicResponse toPublicResponse(LaunchAd entity) {
        return new LaunchAdPublicResponse(
                entity.getId(),
                entity.getCode(),
                entity.getImageUrl(),
                entity.getTargetUrl(),
                entity.getImageVersion(),
                entity.getDisplaySeconds(),
                entity.getSponsorZhCn(),
                entity.getSponsorZhTw(),
                entity.getSponsorEn(),
                entity.getTitleZhCn(),
                entity.getTitleZhTw(),
                entity.getTitleEn(),
                entity.getDescriptionZhCn(),
                entity.getDescriptionZhTw(),
                entity.getDescriptionEn(),
                entity.getCtaZhCn(),
                entity.getCtaZhTw(),
                entity.getCtaEn(),
                entity.getFooterZhCn(),
                entity.getFooterZhTw(),
                entity.getFooterEn(),
                entity.getUpdatedAt()
        );
    }

    private LaunchAdAdminItem toAdminItem(LaunchAd entity) {
        return new LaunchAdAdminItem(
                entity.getId(),
                entity.getCode(),
                entity.getImageUrl(),
                entity.getTargetUrl(),
                entity.getImageVersion(),
                entity.getDisplaySeconds(),
                entity.getSponsorZhCn(),
                entity.getSponsorZhTw(),
                entity.getSponsorEn(),
                entity.getTitleZhCn(),
                entity.getTitleZhTw(),
                entity.getTitleEn(),
                entity.getDescriptionZhCn(),
                entity.getDescriptionZhTw(),
                entity.getDescriptionEn(),
                entity.getCtaZhCn(),
                entity.getCtaZhTw(),
                entity.getCtaEn(),
                entity.getFooterZhCn(),
                entity.getFooterZhTw(),
                entity.getFooterEn(),
                entity.getStatus(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        if (STATUS_ACTIVE.equals(normalized) || STATUS_DRAFT.equals(normalized) || STATUS_INACTIVE.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String resolveUsername(User currentUser) {
        return currentUser == null ? "unknown" : currentUser.getUsername();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
