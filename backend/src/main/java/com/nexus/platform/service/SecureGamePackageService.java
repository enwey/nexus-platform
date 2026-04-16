package com.nexus.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.config.GamePackageProperties;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class SecureGamePackageService {
    public static final String PACKAGE_FORMAT = "NEXUS_SECURE_ZIP_V1";
    private static final String MANIFEST_FILE = "nexus-package.json";
    private static final String PAYLOAD_FILE = "payload.enc";

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] masterKey;

    public SecureGamePackageService(
            ObjectMapper objectMapper,
            GamePackageProperties gamePackageProperties
    ) {
        this.objectMapper = objectMapper;
        this.masterKey = sha256(gamePackageProperties.getMasterKey().getBytes(StandardCharsets.UTF_8));
    }

    public SecurePackage build(byte[] sourceZipBytes, String entryFile) throws Exception {
        byte[] contentKey = randomBytes(32);
        byte[] contentNonce = randomBytes(12);
        byte[] encryptedPayload = encrypt(sourceZipBytes, contentKey, contentNonce);

        byte[] wrapNonce = randomBytes(12);
        byte[] wrappedContentKey = encrypt(contentKey, masterKey, wrapNonce);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("format", PACKAGE_FORMAT);
        manifest.put("entryFile", entryFile);
        manifest.put("payloadFile", PAYLOAD_FILE);
        manifest.put("sourceMd5", md5Hex(sourceZipBytes));
        manifest.put("sourceSize", sourceZipBytes.length);
        manifest.put("encryption", Map.of(
                "algorithm", "AES-256-GCM",
                "nonce", Base64.getEncoder().encodeToString(contentNonce)
        ));

        byte[] manifestBytes = objectMapper.writeValueAsBytes(manifest);
        ByteArrayOutputStream packageBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(packageBytes, StandardCharsets.UTF_8)) {
            zos.putNextEntry(new ZipEntry(MANIFEST_FILE));
            zos.write(manifestBytes);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry(PAYLOAD_FILE));
            zos.write(encryptedPayload);
            zos.closeEntry();
        }

        byte[] finalPackage = packageBytes.toByteArray();
        return new SecurePackage(
                finalPackage,
                md5Hex(finalPackage),
                PACKAGE_FORMAT,
                Base64.getEncoder().encodeToString(wrappedContentKey),
                Base64.getEncoder().encodeToString(wrapNonce)
        );
    }

    public String unwrapContentKey(String wrappedKeyBase64, String wrapNonceBase64) throws Exception {
        byte[] wrappedKey = Base64.getDecoder().decode(wrappedKeyBase64);
        byte[] wrapNonce = Base64.getDecoder().decode(wrapNonceBase64);
        byte[] contentKey = decrypt(wrappedKey, masterKey, wrapNonce);
        return Base64.getEncoder().encodeToString(contentKey);
    }

    private byte[] randomBytes(int size) {
        byte[] output = new byte[size];
        secureRandom.nextBytes(output);
        return output;
    }

    private byte[] encrypt(byte[] plain, byte[] key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        return cipher.doFinal(plain);
    }

    private byte[] decrypt(byte[] cipherText, byte[] key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        return cipher.doFinal(cipherText);
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize package master key", e);
        }
    }

    public String md5Hex(byte[] input) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(input);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public record SecurePackage(
            byte[] bytes,
            String md5,
            String format,
            String wrappedContentKey,
            String wrappedContentKeyNonce
    ) {
    }
}
