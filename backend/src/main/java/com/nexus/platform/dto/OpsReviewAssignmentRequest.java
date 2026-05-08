package com.nexus.platform.dto;

public record OpsReviewAssignmentRequest(
        Long reviewerId,
        String note
) {
}
