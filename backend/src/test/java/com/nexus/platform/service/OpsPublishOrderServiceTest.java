package com.nexus.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.OpsPublishOrderPreviewDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.OpsContentItem;
import com.nexus.platform.entity.OpsPublishOrder;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.LaunchAdRepository;
import com.nexus.platform.repository.OpsContentItemRepository;
import com.nexus.platform.repository.OpsMessageNoticeRepository;
import com.nexus.platform.repository.OpsPublishOrderRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OpsPublishOrderServiceTest {

    @Test
    void getOrderPreviewShouldExposeDriftAndExecutionSnapshots() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuditSnapshotService auditSnapshotService = new AuditSnapshotService(objectMapper);
        OpsPublishOrderRepository orderRepository = mock(OpsPublishOrderRepository.class);
        OpsContentItemRepository contentRepository = mock(OpsContentItemRepository.class);
        LaunchAdRepository launchAdRepository = mock(LaunchAdRepository.class);
        OpsMessageNoticeRepository noticeRepository = mock(OpsMessageNoticeRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        GameVersionRepository gameVersionRepository = mock(GameVersionRepository.class);

        OpsPublishOrderService service = new OpsPublishOrderService(
                orderRepository,
                contentRepository,
                launchAdRepository,
                noticeRepository,
                gameRepository,
                gameVersionRepository,
                mock(GameService.class),
                mock(AuditLogService.class),
                auditSnapshotService,
                mock(IdempotencyService.class),
                objectMapper
        );

        OpsPublishOrder order = new OpsPublishOrder();
        order.setId(11L);
        order.setAssetType("CONTENT_ITEM");
        order.setTargetId(21L);
        order.setAssetCode("21");
        order.setAssetName("Hero Banner");
        order.setDesiredAction("PUBLISH");
        order.setOrderStatus("APPROVED");
        order.setRolloutMode("FULL");
        order.setSnapshotJson("{\"status\":\"DRAFT\",\"title\":\"Hero Banner\",\"startAt\":\"2026-05-07T09:00:00\"}");
        order.setDesiredSnapshotJson("{\"status\":\"PUBLISHED\",\"title\":\"Hero Banner\",\"startAt\":\"2026-05-07T09:00:00\"}");
        order.setExecutionBeforeSnapshotJson("{\"status\":\"DRAFT\"}");
        order.setExecutionAfterSnapshotJson("{\"status\":\"PUBLISHED\"}");

        OpsContentItem item = new OpsContentItem();
        item.setId(21L);
        item.setGameId(31L);
        item.setTitle("Hero Banner");
        item.setStatus("PUBLISHED");
        item.setStartAt(LocalDateTime.parse("2026-05-08T09:00:00"));

        Game game = new Game();
        game.setId(31L);
        game.setAppId("hero-app");

        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(contentRepository.findById(21L)).thenReturn(Optional.of(item));
        when(gameRepository.findById(31L)).thenReturn(Optional.of(game));

        Result<OpsPublishOrderPreviewDto> result = service.getOrderPreview(11L);

        assertEquals(0, result.getCode());
        OpsPublishOrderPreviewDto preview = result.getData();
        assertEquals("DRAFT", String.valueOf(preview.submittedSnapshot().get("status")));
        assertEquals("PUBLISHED", String.valueOf(preview.currentSnapshot().get("status")));
        assertFalse(preview.configDriftDiffs().isEmpty());
        assertTrue(preview.configDriftDiffs().stream().anyMatch(diff -> diff.field().equals("status")));
        assertFalse(preview.executionDiffs().isEmpty());
        assertTrue(preview.executionDiffs().stream().anyMatch(diff ->
                diff.field().equals("status")
                        && diff.beforeValue().equals("DRAFT")
                        && diff.afterValue().equals("PUBLISHED")));
    }
}
