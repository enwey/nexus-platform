package com.nexus.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LaunchAdDtos {
    public record LaunchAdPublicResponse(
            Long id,
            String code,
            String imageUrl,
            String targetUrl,
            String imageVersion,
            Integer displaySeconds,
            String status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String sponsorZhCn,
            String sponsorZhTw,
            String sponsorEn,
            String titleZhCn,
            String titleZhTw,
            String titleEn,
            String descriptionZhCn,
            String descriptionZhTw,
            String descriptionEn,
            String ctaZhCn,
            String ctaZhTw,
            String ctaEn,
            String footerZhCn,
            String footerZhTw,
            String footerEn,
            String effectiveState,
            LocalDateTime updatedAt
    ) {}

    public record LaunchAdAdminItem(
            Long id,
            String code,
            String imageUrl,
            String targetUrl,
            String imageVersion,
            Integer displaySeconds,
            String sponsorZhCn,
            String sponsorZhTw,
            String sponsorEn,
            String titleZhCn,
            String titleZhTw,
            String titleEn,
            String descriptionZhCn,
            String descriptionZhTw,
            String descriptionEn,
            String ctaZhCn,
            String ctaZhTw,
            String ctaEn,
            String footerZhCn,
            String footerZhTw,
            String footerEn,
            String status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String effectiveState,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record LaunchAdUpsertRequest(
            String imageUrl,
            String targetUrl,
            String imageVersion,
            Integer displaySeconds,
            String sponsorZhCn,
            String sponsorZhTw,
            String sponsorEn,
            String titleZhCn,
            String titleZhTw,
            String titleEn,
            String descriptionZhCn,
            String descriptionZhTw,
            String descriptionEn,
            String ctaZhCn,
            String ctaZhTw,
            String ctaEn,
            String footerZhCn,
            String footerZhTw,
            String footerEn,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String status
    ) {}

    public record LaunchAdAdminListResponse(
            LaunchAdPublicResponse active,
            List<LaunchAdAdminItem> items
    ) {}
}
