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

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Async
    public void processUpload(Long gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getStatus() != Game.GameStatus.PROCESSING) {
            return;
        }

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(game.getSourceStorageKey())
                        .build())) {
            byte[] sourceZipBytes = stream.readAllBytes();

            long versionCount = gameVersionRepository.countByGameId(game.getId());
            String versionName = "1.0." + versionCount;
            SecureGamePackageService.SecurePackage securePackage =
                    secureGamePackageService.build(sourceZipBytes, "index.html");

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
            version.setEntryFile("index.html");
            version.setStorageKey(game.getStorageKey());
            version.setSourceStorageKey(game.getSourceStorageKey());
            version.setDownloadUrl(game.getDownloadUrl());
            version.setMd5(securePackage.md5());
            version.setSourceMd5(secureGamePackageService.md5Hex(sourceZipBytes));
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
            game.setStatus(Game.GameStatus.DRAFT);
            gameRepository.save(game);
        } catch (Exception e) {
            log.error("Upload async processing failed, gameId={}", gameId, e);
            game.setStatus(Game.GameStatus.REJECTED);
            gameRepository.save(game);
        }
    }
}
