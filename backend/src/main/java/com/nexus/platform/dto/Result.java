package com.nexus.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private ApiError error;

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data, null);
    }

    public static <T> Result<T> success() {
        return new Result<>(0, "success", null, null);
    }

    public static <T> Result<T> error(String message) {
        return error(ErrorCodes.COMMON_BAD_REQUEST, message);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, new ApiError(
                ErrorCodes.COMMON_BAD_REQUEST,
                message,
                java.util.UUID.randomUUID().toString(),
                false,
                java.util.Map.of()
        ));
    }

    public static <T> Result<T> error(String errorCode, String message) {
        return error(-1, errorCode, message, false, java.util.Map.of());
    }

    public static <T> Result<T> error(String errorCode, String message, java.util.Map<String, Object> details) {
        return error(-1, errorCode, message, false, details);
    }

    public static <T> Result<T> error(int code, String errorCode, String message) {
        return error(code, errorCode, message, false, java.util.Map.of());
    }

    public static <T> Result<T> error(
            int code,
            String errorCode,
            String message,
            boolean retryable,
            java.util.Map<String, Object> details
    ) {
        return new Result<>(code, message, null, new ApiError(
                errorCode,
                message,
                java.util.UUID.randomUUID().toString(),
                retryable,
                details == null ? java.util.Map.of() : details
        ));
    }
}
