package com.nexus.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexus.platform.dto.ErrorCodes;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.IdempotencyRecord;
import com.nexus.platform.repository.IdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private static final int MAX_KEY_LENGTH = 128;

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public <T> Result<T> execute(
            String scope,
            String idempotencyKey,
            Object requestPayload,
            Long actorId,
            String requestPath,
            JavaType resultType,
            Supplier<Result<T>> action
    ) {
        String normalizedKey = normalizeKey(idempotencyKey);
        if (normalizedKey == null) {
            return action.get();
        }
        if (normalizedKey.isEmpty()) {
            return Result.error(
                    ErrorCodes.IDEMPOTENCY_KEY_INVALID,
                    "Idempotency key must be 1-128 characters",
                    Map.of("scope", scope)
            );
        }
        String fingerprint = fingerprint(scope, requestPayload);
        IdempotencyRecord existing = findRecord(scope, normalizedKey);
        if (existing != null) {
            return replayExisting(existing, fingerprint, resultType, scope, normalizedKey);
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setScope(scope);
        record.setIdempotencyKey(normalizedKey);
        record.setRequestFingerprint(fingerprint);
        record.setRequestPath(trimToNull(requestPath, 256));
        record.setActorId(actorId);
        record.setRecordStatus("PROCESSING");
        record.setExpiresAt(LocalDateTime.now().plusHours(24));
        try {
            idempotencyRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ignored) {
            IdempotencyRecord concurrent = findRecord(scope, normalizedKey);
            if (concurrent != null) {
                return replayExisting(concurrent, fingerprint, resultType, scope, normalizedKey);
            }
            return Result.error(
                    ErrorCodes.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                    "Duplicate request is being processed",
                    Map.of("scope", scope)
            );
        }

        Result<T> result = action.get();
        record.setRecordStatus("COMPLETED");
        record.setResponseCode(result == null ? -1 : result.getCode());
        record.setResponseErrorCode(result != null && result.getError() != null ? result.getError().getErrorCode() : null);
        record.setResponsePayload(writeValueAsString(result));
        record.setCompletedAt(LocalDateTime.now());
        idempotencyRecordRepository.save(record);
        return result;
    }

    private <T> Result<T> replayExisting(
            IdempotencyRecord existing,
            String fingerprint,
            JavaType resultType,
            String scope,
            String idempotencyKey
    ) {
        if (!fingerprint.equals(existing.getRequestFingerprint())) {
            return Result.error(
                    ErrorCodes.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency key has already been used for a different request",
                    Map.of("scope", scope, "idempotencyKey", idempotencyKey)
            );
        }
        if ("PROCESSING".equals(existing.getRecordStatus())) {
            return Result.error(
                    ErrorCodes.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                    "Duplicate request is being processed",
                    Map.of("scope", scope, "idempotencyKey", idempotencyKey)
            );
        }
        if (existing.getResponsePayload() == null || existing.getResponsePayload().isBlank()) {
            return Result.error(
                    ErrorCodes.IDEMPOTENCY_REPLAY_UNAVAILABLE,
                    "Saved idempotent response is unavailable",
                    Map.of("scope", scope, "idempotencyKey", idempotencyKey)
            );
        }
        try {
            return objectMapper.readValue(existing.getResponsePayload(), resultType);
        } catch (JsonProcessingException e) {
            return Result.error(
                    ErrorCodes.IDEMPOTENCY_REPLAY_UNAVAILABLE,
                    "Saved idempotent response is unavailable",
                    Map.of("scope", scope, "idempotencyKey", idempotencyKey)
            );
        }
    }

    private IdempotencyRecord findRecord(String scope, String idempotencyKey) {
        return idempotencyRecordRepository.findByScopeAndIdempotencyKey(scope, idempotencyKey).orElse(null);
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_KEY_LENGTH) {
            return "";
        }
        return normalized;
    }

    private String fingerprint(String scope, Object payload) {
        String serialized = writeValueAsString(payload);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((scope + "|" + serialized).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString((scope + "|" + serialized).hashCode());
        }
    }

    private String writeValueAsString(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("serializationError", e.getMessage());
            return fallback.toString();
        }
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
}
