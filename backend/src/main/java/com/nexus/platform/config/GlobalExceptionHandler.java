package com.nexus.platform.config;

import com.nexus.platform.dto.ErrorCodes;
import com.nexus.platform.dto.Result;
import com.nexus.platform.exception.PlatformException;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(PlatformException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlePlatformException(PlatformException e) {
        return Result.error(
                e.getHttpStatus().value(),
                e.getErrorCode(),
                e.getMessage(),
                e.isRetryable(),
                e.getDetails()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unhandled server exception, requestId={}", requestId, e);
        return Result.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCodes.COMMON_INTERNAL_ERROR,
                "Internal server error",
                false,
                Map.of("requestId", requestId)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCodes.COMMON_BAD_REQUEST,
                e.getMessage(),
                false,
                Map.of()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return Result.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCodes.COMMON_INVALID_JSON,
                "Request body format is invalid",
                false,
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Request validation failed");
        return Result.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCodes.COMMON_BAD_REQUEST,
                message,
                false,
                Map.of()
        );
    }
}
