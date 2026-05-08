package com.nexus.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class HostedMiniAppPackageService {
    private static final Set<String> ALLOWED_KINDS = Set.of("html5-mini-game", "html5-mini-app", "html5");
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public HostedMiniAppPackageService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HostedMiniAppDescriptor inspect(byte[] zipBytes) throws Exception {
        Map<String, byte[]> entries = readZipEntries(zipBytes);
        byte[] manifestBytes = entries.get("manifest.json");
        if (manifestBytes == null) {
            throw new IllegalArgumentException("Zip missing manifest.json");
        }

        LinkedHashMap<String, Object> manifest = objectMapper.readValue(manifestBytes, MAP_TYPE);
        String entryFile = normalizePath(resolveString(manifest, "entry", nestedString(manifest, "metadata", "entry")));
        if (entryFile == null || entryFile.isBlank()) {
            throw new IllegalArgumentException("manifest.json missing entry");
        }
        if (!entries.containsKey(entryFile)) {
            throw new IllegalArgumentException("manifest entry file not found: " + entryFile);
        }

        String kind = resolveString(manifest, "kind", nestedString(manifest, "metadata", "kind"));
        if (kind == null || kind.isBlank()) {
            kind = "html5-mini-game";
        }
        kind = kind.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_KINDS.contains(kind)) {
            throw new IllegalArgumentException("manifest kind is not supported");
        }

        String name = resolveString(manifest, "name", nestedString(manifest, "metadata", "name"));
        String description = resolveString(manifest, "description", nestedString(manifest, "metadata", "description"));
        return new HostedMiniAppDescriptor(entryFile, kind, name, description);
    }

    public NormalizedHostedMiniAppPackage normalize(byte[] zipBytes, String appId, String version) throws Exception {
        Map<String, byte[]> entries = readZipEntries(zipBytes);
        HostedMiniAppDescriptor descriptor = inspect(zipBytes);

        LinkedHashMap<String, Object> manifest = objectMapper.readValue(entries.get("manifest.json"), MAP_TYPE);
        manifest.put("appId", appId);
        manifest.put("version", version);
        manifest.put("entry", descriptor.entryFile());
        manifest.put("kind", descriptor.kind());

        LinkedHashMap<String, Object> metadata = nestedMap(manifest, "metadata");
        applyDefaultLocalePresentation(manifest, metadata);
        metadata.put("kind", descriptor.kind());
        manifest.put("metadata", metadata);

        entries.put("manifest.json", objectMapper.writeValueAsBytes(manifest));
        return new NormalizedHostedMiniAppPackage(writeZipEntries(entries), descriptor);
    }

    public HostedMiniAppManifestReport inspectReport(byte[] zipBytes) throws Exception {
        Map<String, byte[]> entries = readZipEntries(zipBytes);
        byte[] manifestBytes = entries.get("manifest.json");
        if (manifestBytes == null) {
            throw new IllegalArgumentException("Zip missing manifest.json");
        }
        LinkedHashMap<String, Object> manifest = objectMapper.readValue(manifestBytes, MAP_TYPE);
        HostedMiniAppDescriptor descriptor = inspect(zipBytes);
        String appId = resolveString(manifest, "appId", nestedString(manifest, "metadata", "appId"));
        String version = resolveString(manifest, "version", nestedString(manifest, "metadata", "version"));
        Map<String, HostedMiniAppLocale> locales = readLocales(manifest);
        return new HostedMiniAppManifestReport(
                appId,
                version,
                descriptor.entryFile(),
                descriptor.kind(),
                descriptor.name(),
                descriptor.description(),
                locales
        );
    }

    private Map<String, byte[]> readZipEntries(byte[] zipBytes) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        Set<String> seenEntries = new LinkedHashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String normalizedName = normalizePath(entry.getName());
                if (normalizedName == null || normalizedName.isBlank()) {
                    throw new IllegalArgumentException("Zip contains invalid file path");
                }
                if (!seenEntries.add(normalizedName)) {
                    throw new IllegalArgumentException("Zip contains duplicate file path: " + normalizedName);
                }
                entries.put(normalizedName, zis.readAllBytes());
            }
        }
        return entries;
    }

    private byte[] writeZipEntries(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private String normalizePath(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replace("\\", "/");
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String resolveString(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> nestedMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> mapValue) {
            LinkedHashMap<String, Object> output = new LinkedHashMap<>();
            mapValue.forEach((nestedKey, nestedValue) -> {
                if (nestedKey instanceof String stringKey) {
                    output.put(stringKey, nestedValue);
                }
            });
            return output;
        }
        return new LinkedHashMap<>();
    }

    private String nestedString(Map<String, Object> source, String objectKey, String valueKey) {
        Map<String, Object> nested = nestedMap(source, objectKey);
        Object value = nested.get(valueKey);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue.trim();
        }
        return null;
    }

    private void applyDefaultLocalePresentation(Map<String, Object> manifest, Map<String, Object> metadata) {
        Map<String, HostedMiniAppLocale> locales = readLocales(manifest);
        if (locales.isEmpty()) {
            return;
        }

        String defaultLocale = resolveString(metadata, "defaultLocale", null);
        HostedMiniAppLocale preferred = pickDefaultLocale(locales, defaultLocale);
        if (preferred == null) {
            return;
        }

        if (preferred.name() != null && !preferred.name().isBlank()) {
            manifest.put("name", preferred.name());
        }
        if (preferred.description() != null && !preferred.description().isBlank()) {
            manifest.put("description", preferred.description());
        }
    }

    private HostedMiniAppLocale pickDefaultLocale(Map<String, HostedMiniAppLocale> locales, String defaultLocale) {
        if (defaultLocale != null && !defaultLocale.isBlank()) {
            HostedMiniAppLocale exact = locales.get(defaultLocale);
            if (exact != null) {
                return exact;
            }
            String normalized = defaultLocale.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, HostedMiniAppLocale> entry : locales.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                if (key.equals(normalized) || key.startsWith(normalized + "-")) {
                    return entry.getValue();
                }
            }
        }
        return locales.values().stream().findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, HostedMiniAppLocale> readLocales(Map<String, Object> manifest) {
        LinkedHashMap<String, HostedMiniAppLocale> output = new LinkedHashMap<>();
        collectLocales(output, manifest.get("locales"));
        Map<String, Object> metadata = nestedMap(manifest, "metadata");
        collectLocales(output, metadata.get("locales"));
        return output;
    }

    @SuppressWarnings("unchecked")
    private void collectLocales(Map<String, HostedMiniAppLocale> output, Object rawLocales) {
        if (!(rawLocales instanceof Map<?, ?> localeMap)) {
            return;
        }
        for (Map.Entry<?, ?> entry : localeMap.entrySet()) {
            if (!(entry.getKey() instanceof String localeKey) || !(entry.getValue() instanceof Map<?, ?> localeValue)) {
                continue;
            }
            String name = null;
            String description = null;
            Object rawName = localeValue.get("name");
            if (rawName instanceof String stringValue && !stringValue.isBlank()) {
                name = stringValue.trim();
            }
            Object rawDescription = localeValue.get("description");
            if (rawDescription instanceof String stringValue && !stringValue.isBlank()) {
                description = stringValue.trim();
            }
            if (name != null || description != null) {
                output.put(localeKey, new HostedMiniAppLocale(name, description));
            }
        }
    }

    public record HostedMiniAppDescriptor(
            String entryFile,
            String kind,
            String name,
            String description
    ) {
    }

    public record NormalizedHostedMiniAppPackage(
            byte[] zipBytes,
            HostedMiniAppDescriptor descriptor
    ) {
    }

    public record HostedMiniAppManifestReport(
            String appId,
            String version,
            String entryFile,
            String kind,
            String name,
            String description,
            Map<String, HostedMiniAppLocale> locales
    ) {
    }

    public record HostedMiniAppLocale(
            String name,
            String description
    ) {
    }
}
