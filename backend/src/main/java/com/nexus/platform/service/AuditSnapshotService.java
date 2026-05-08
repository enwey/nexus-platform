package com.nexus.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.dto.AuditFieldDiffDto;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditSnapshotService {
    private final ObjectMapper objectMapper;

    public Map<String, Object> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
            );
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public List<AuditFieldDiffDto> buildDiffs(Map<String, Object> before, Map<String, Object> after) {
        Map<String, String> beforeFlat = new LinkedHashMap<>();
        Map<String, String> afterFlat = new LinkedHashMap<>();
        flatten("", before == null ? Map.of() : before, beforeFlat);
        flatten("", after == null ? Map.of() : after, afterFlat);

        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(beforeFlat.keySet());
        fields.addAll(afterFlat.keySet());

        List<AuditFieldDiffDto> diffs = new ArrayList<>();
        for (String field : fields) {
            String beforeValue = beforeFlat.get(field);
            String afterValue = afterFlat.get(field);
            if (normalize(beforeValue).equals(normalize(afterValue))) {
                continue;
            }
            diffs.add(new AuditFieldDiffDto(
                    field,
                    normalize(beforeValue),
                    normalize(afterValue),
                    resolveChangeType(beforeFlat.containsKey(field), afterFlat.containsKey(field))
            ));
        }
        return diffs;
    }

    private void flatten(String prefix, Object value, Map<String, String> target) {
        if (value instanceof Map<?, ?> mapValue) {
            if (mapValue.isEmpty() && !prefix.isBlank()) {
                target.put(prefix, "{}");
            }
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String path = prefix.isBlank() ? key : prefix + "." + key;
                flatten(path, entry.getValue(), target);
            }
            return;
        }
        if (value instanceof Collection<?> collectionValue) {
            if (collectionValue.isEmpty()) {
                target.put(prefix, "[]");
                return;
            }
            int index = 0;
            for (Object item : collectionValue) {
                flatten(prefix + "[" + index + "]", item, target);
                index += 1;
            }
            return;
        }
        if (value != null && value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            if (array.length == 0) {
                target.put(prefix, "[]");
                return;
            }
            for (int index = 0; index < array.length; index += 1) {
                flatten(prefix + "[" + index + "]", array[index], target);
            }
            return;
        }
        target.put(prefix, normalize(value == null ? null : String.valueOf(value)));
    }

    private String resolveChangeType(boolean hadBefore, boolean hadAfter) {
        if (!hadBefore && hadAfter) {
            return "ADDED";
        }
        if (hadBefore && !hadAfter) {
            return "REMOVED";
        }
        return "CHANGED";
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
