package com.nexus.platform.dto;

import java.util.List;

public record OpsReviewOverviewDto(
        Integer pending,
        Integer assigned,
        Integer unassigned,
        Integer overdue,
        Integer dueSoon,
        Integer approved,
        Integer rejected,
        Integer pendingAppeals,
        Double avgPendingHours,
        List<ReviewerLoad> reviewerLoads,
        List<BacklogBucket> backlogBuckets
) {
    public record ReviewerLoad(
            Long reviewerId,
            String reviewerName,
            Integer assignedPending,
            Integer overduePending
    ) {
    }

    public record BacklogBucket(
            String bucketCode,
            String bucketLabel,
            Integer count
    ) {
    }
}
