package com.nexus.platform.service;

import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadProcessingService {
    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final MinioClient minioClient;
    private final SecureGamePackageService secureGamePackageService;
    private final HostedMiniAppPackageService hostedMiniAppPackageService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Async
    public void processUpload(Long gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getStatus() != Game.GameStatus.PROCESSING) {
            return;
        }
        game.setUploadProcessingStartedAt(java.time.LocalDateTime.now());
        game.setUploadProcessingFinishedAt(null);
        game.setUploadProcessingFailureReason(null);
        gameRepository.save(game);

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(game.getSourceStorageKey())
                        .build())) {
            byte[] sourceZipBytes = stream.readAllBytes();

            long versionCount = gameVersionRepository.countByGameId(game.getId());
            String versionName = "1.0." + versionCount;
            HostedMiniAppPackageService.NormalizedHostedMiniAppPackage normalizedPackage =
                    hostedMiniAppPackageService.normalize(sourceZipBytes, game.getAppId(), versionName);
            byte[] normalizedSourceZipBytes = normalizedPackage.zipBytes();
            SecureGamePackageService.SecurePackage securePackage =
                    secureGamePackageService.build(normalizedSourceZipBytes, normalizedPackage.descriptor().entryFile());
            HostedMiniAppPackageService.HostedMiniAppManifestReport manifestReport =
                    hostedMiniAppPackageService.inspectReport(normalizedSourceZipBytes);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(game.getSourceStorageKey())
                            .stream(new ByteArrayInputStream(normalizedSourceZipBytes), normalizedSourceZipBytes.length, -1)
                            .contentType("application/zip")
                            .build()
            );

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(game.getStorageKey())
                            .stream(new ByteArrayInputStream(securePackage.bytes()), securePackage.bytes().length, -1)
                            .contentType("application/zip")
                            .build()
            );

            GameVersion version = new GameVersion();
            version.setGameId(game.getId());
            version.setVersionName(versionName);
            version.setEntryFile(normalizedPackage.descriptor().entryFile());
            version.setStorageKey(game.getStorageKey());
            version.setSourceStorageKey(game.getSourceStorageKey());
            version.setDownloadUrl(game.getDownloadUrl());
            version.setMd5(securePackage.md5());
            version.setSourceMd5(secureGamePackageService.md5Hex(normalizedSourceZipBytes));
            version.setPackageFormat(securePackage.format());
            version.setPackageKeyCiphertext(securePackage.wrappedContentKey());
            version.setPackageKeyNonce(securePackage.wrappedContentKeyNonce());
            version.setForcedUpdate(false);
            version.setStatus(GameVersion.VersionStatus.DRAFT);
            gameVersionRepository.save(version);

            game.setMd5(securePackage.md5());
            game.setSourceMd5(version.getSourceMd5());
            game.setVersion(versionName);
            game.setPackageFormat(securePackage.format());
            game.setPackageKeyCiphertext(securePackage.wrappedContentKey());
            game.setPackageKeyNonce(securePackage.wrappedContentKeyNonce());
            if (manifestReport.name() != null && !manifestReport.name().isBlank()) {
                game.setName(manifestReport.name());
            }
            if (manifestReport.description() != null && !manifestReport.description().isBlank()) {
                game.setDescription(manifestReport.description());
            }
            game.setStatus(Game.GameStatus.DRAFT);
            game.setUploadProcessingFinishedAt(java.time.LocalDateTime.now());
            game.setUploadProcessingFailureReason(null);
            gameRepository.save(game);
        } catch (Exception e) {
            log.error("Upload async processing failed, gameId={}", gameId, e);
            game.setStatus(Game.GameStatus.REJECTED);
            game.setUploadProcessingFinishedAt(java.time.LocalDateTime.now());
            String failureReason = e.getMessage() == null ? "Upload processing failed" : e.getMessage().trim();
            game.setUploadProcessingFailureReason(failureReason.length() > 256 ? failureReason.substring(0, 256) : failureReason);
            gameRepository.save(game);
        }
    }

    @Async
    public void processVersionUpload(Long versionId) {
        GameVersion version = gameVersionRepository.findById(versionId).orElse(null);
        if (version == null || version.getStatus() != GameVersion.VersionStatus.PROCESSING) {
            return;
        }
        Game game = gameRepository.findById(version.getGameId()).orElse(null);
        if (game == null) {
            version.setStatus(GameVersion.VersionStatus.REJECTED);
            version.setAuditReason("Game not found during version processing");
            gameVersionRepository.save(version);
            return;
        }

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(version.getSourceStorageKey())
                        .build())) {
            byte[] sourceZipBytes = stream.readAllBytes();

            HostedMiniAppPackageService.NormalizedHostedMiniAppPackage normalizedPackage =
                    hostedMiniAppPackageService.normalize(sourceZipBytes, game.getAppId(), version.getVersionName());
            byte[] normalizedSourceZipBytes = normalizedPackage.zipBytes();
            SecureGamePackageService.SecurePackage securePackage =
                    secureGamePackageService.build(normalizedSourceZipBytes, normalizedPackage.descriptor().entryFile());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(version.getSourceStorageKey())
                            .stream(new ByteArrayInputStream(normalizedSourceZipBytes), normalizedSourceZipBytes.length, -1)
                            .contentType("application/zip")
                            .build()
            );

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(version.getStorageKey())
                            .stream(new ByteArrayInputStream(securePackage.bytes()), securePackage.bytes().length, -1)
                            .contentType("application/zip")
                            .build()
            );

            version.setEntryFile(normalizedPackage.descriptor().entryFile());
            version.setDownloadUrl(game.getDownloadUrl());
            version.setMd5(securePackage.md5());
            version.setSourceMd5(secureGamePackageService.md5Hex(normalizedSourceZipBytes));
            version.setPackageFormat(securePackage.format());
            version.setPackageKeyCiphertext(securePackage.wrappedContentKey());
            version.setPackageKeyNonce(securePackage.wrappedContentKeyNonce());
            version.setSubmitNote(null);
            version.setAuditReason(null);
            version.setAssignedReviewerId(null);
            version.setAssignedAt(null);
            version.setStatus(GameVersion.VersionStatus.DRAFT);
            gameVersionRepository.save(version);
        } catch (Exception e) {
            log.error("Version upload async processing failed, versionId={}", versionId, e);
            version.setStatus(GameVersion.VersionStatus.REJECTED);
            String failureReason = e.getMessage() == null ? "Version upload processing failed" : e.getMessage().trim();
            version.setAuditReason(failureReason.length() > 256 ? failureReason.substring(0, 256) : failureReason);
            version.setAssignedReviewerId(null);
            version.setAssignedAt(null);
            version.setUpdatedAt(LocalDateTime.now());
            gameVersionRepository.save(version);
        }
    }
}
