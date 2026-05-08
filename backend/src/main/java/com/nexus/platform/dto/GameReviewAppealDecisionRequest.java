package com.nexus.platform.dto;

public record GameReviewAppealDecisionRequest(
        String decision,
        String reviewNote
) {
}
