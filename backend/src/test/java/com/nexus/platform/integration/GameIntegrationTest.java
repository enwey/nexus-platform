package com.nexus.platform.integration;

import com.nexus.platform.dto.Result;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GameIntegrationTest {

    @Test
    void shouldBuildSuccessResultForListPayload() {
        Result<List<String>> result = Result.success(List.of("demo-game"));

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("demo-game", result.getData().get(0));
    }

    @Test
    void shouldBuildEmptySuccessResult() {
        Result<Void> result = Result.success();

        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }
}
