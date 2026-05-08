package com.nexus.platform.exception;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PlatformException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final boolean retryable;
    private final Map<String, Object> details;

    public PlatformException(String errorCode, String message, HttpStatus httpStatus) {
        this(errorCode, message, httpStatus, false, Map.of());
    }

    public PlatformException(
            String errorCode,
            String message,
            HttpStatus httpStatus,
            boolean retryable,
            Map<String, Object> details
    ) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
        this.details = details == null ? Map.of() : details;
    }
}
