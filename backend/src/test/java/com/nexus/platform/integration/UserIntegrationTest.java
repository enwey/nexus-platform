package com.nexus.platform.integration;

import com.nexus.platform.dto.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserIntegrationTest {

    @Test
    void shouldBuildSuccessResult() {
        Result<String> result = Result.success("ok");

        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("ok", result.getData());
    }

    @Test
    void shouldBuildErrorResult() {
        Result<Void> result = Result.error("用户名已存在");

        assertEquals(-1, result.getCode());
        assertEquals("用户名已存在", result.getMessage());
        assertNull(result.getData());
    }
}
