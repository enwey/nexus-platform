package com.nexus.platform.dto;

public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final String COMMON_BAD_REQUEST = "COMMON_BAD_REQUEST";
    public static final String COMMON_UNAUTHORIZED = "COMMON_UNAUTHORIZED";
    public static final String COMMON_FORBIDDEN = "COMMON_FORBIDDEN";
    public static final String COMMON_NOT_FOUND = "COMMON_NOT_FOUND";
    public static final String COMMON_INTERNAL_ERROR = "COMMON_INTERNAL_ERROR";
    public static final String COMMON_INVALID_JSON = "COMMON_INVALID_JSON";

    public static final String IDEMPOTENCY_KEY_INVALID = "IDEMPOTENCY_KEY_INVALID";
    public static final String IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED";
    public static final String IDEMPOTENCY_REQUEST_IN_PROGRESS = "IDEMPOTENCY_REQUEST_IN_PROGRESS";
    public static final String IDEMPOTENCY_REPLAY_UNAVAILABLE = "IDEMPOTENCY_REPLAY_UNAVAILABLE";
}
