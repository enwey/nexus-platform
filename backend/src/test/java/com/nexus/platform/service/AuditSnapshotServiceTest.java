package com.nexus.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.AuditFieldDiffDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditSnapshotServiceTest {

    private final AuditSnapshotService auditSnapshotService = new AuditSnapshotService(new ObjectMapper());

    @Test
    void buildDiffs_flattensNestedSnapshotChanges() {
        List<AuditFieldDiffDto> diffs = auditSnapshotService.buildDiffs(
                Map.of(
                        "status", "DRAFT",
                        "config", Map.of("rollout", 10, "channel", "cn")
                ),
                Map.of(
                        "status", "PUBLISHED",
                        "config", Map.of("rollout", 30, "channel", "cn"),
                        "receipt", "ok"
                )
        );

        assertEquals(3, diffs.size());
        assertTrue(diffs.stream().anyMatch(item -> "status".equals(item.field()) && "CHANGED".equals(item.changeType())));
        assertTrue(diffs.stream().anyMatch(item -> "config.rollout".equals(item.field()) && "30".equals(item.afterValue())));
        assertTrue(diffs.stream().anyMatch(item -> "receipt".equals(item.field()) && "ADDED".equals(item.changeType())));
    }

    @Test
    void parseObject_returnsEmptyMapForInvalidJson() {
        assertTrue(auditSnapshotService.parseObject("{invalid").isEmpty());
    }
}
