package com.nexus.platform.dto;

import java.util.List;

public record ReferralRecordsResponse(
        ReferralSummaryDto summary,
        List<ReferralRecordDto> records
) {
}
