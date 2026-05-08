package com.nexus.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.ErrorCodes;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.IdempotencyRecord;
import com.nexus.platform.repository.IdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void execute_replaysCompletedResponseForSameRequest() throws Exception {
        IdempotencyService service = new IdempotencyService(idempotencyRecordRepository, objectMapper);
        IdempotencyRecord record = new IdempotencyRecord();
        record.setScope("publish");
        record.setIdempotencyKey("fixed-key");
        Map<String, Object> payload = Map.of("target", "discover");
        record.setRequestFingerprint(fingerprint("publish", payload));
        record.setRecordStatus("COMPLETED");
        record.setCompletedAt(LocalDateTime.now());
        record.setResponsePayload(objectMapper.writeValueAsString(Result.success("ok")));

        when(idempotencyRecordRepository.findByScopeAndIdempotencyKey("publish", "fixed-key"))
                .thenReturn(Optional.of(record));

        Result<String> result = service.execute(
                "publish",
                "fixed-key",
                payload,
                1L,
                "/admin/ops/publish",
                objectMapper.getTypeFactory().constructParametricType(Result.class, String.class),
                () -> Result.success("new")
        );

        assertEquals(0, result.getCode());
        assertEquals("ok", result.getData());
        verify(idempotencyRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void execute_rejectsReusedKeyForDifferentPayload() {
        IdempotencyService service = new IdempotencyService(idempotencyRecordRepository, objectMapper);
        IdempotencyRecord record = new IdempotencyRecord();
        record.setScope("publish");
        record.setIdempotencyKey("fixed-key");
        record.setRequestFingerprint("different-fingerprint");
        record.setRecordStatus("COMPLETED");
        record.setResponsePayload("{\"code\":0}");

        when(idempotencyRecordRepository.findByScopeAndIdempotencyKey("publish", "fixed-key"))
                .thenReturn(Optional.of(record));

        Result<String> result = service.execute(
                "publish",
                "fixed-key",
                Map.of("target", "discover"),
                1L,
                "/admin/ops/publish",
                objectMapper.getTypeFactory().constructParametricType(Result.class, String.class),
                () -> Result.success("new")
        );

        assertEquals(-1, result.getCode());
        assertNotNull(result.getError());
        assertEquals(ErrorCodes.IDEMPOTENCY_KEY_REUSED, result.getError().getErrorCode());
    }

    private String fingerprint(String scope, Object payload) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest((scope + "|" + objectMapper.writeValueAsString(payload)).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }
}
