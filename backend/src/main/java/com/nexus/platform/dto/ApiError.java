package com.nexus.platform.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String errorCode;
    private String userMessage;
    private String requestId;
    private boolean retryable;
    private Map<String, Object> details;
}
