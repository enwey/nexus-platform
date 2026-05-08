package com.nexus.platform.dto;

import java.util.List;

public record DeveloperCertificationUpsertRequest(
        String subjectType,
        String subjectName,
        String legalRepresentative,
        String contactName,
        String contactPhone,
        String businessLicenseNo,
        String idDocumentNo,
        List<String> certificateAssets,
        String note,
        Boolean submit
) {
}
