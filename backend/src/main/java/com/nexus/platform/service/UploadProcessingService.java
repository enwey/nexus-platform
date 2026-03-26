package com.nexus.platform.service;

import com.nexus.platform.entity.Game;
import com.nexus.platform.repository.GameRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import java.security.MessageDigest;
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
    private final MinioClient minioClient;

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
                        .object(game.getStorageKey())
                        .build())) {

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }

            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            game.setMd5(sb.toString());
            game.setVersion("1.0.0");
            game.setStatus(Game.GameStatus.PENDING);
            gameRepository.save(game);
        } catch (Exception e) {
            log.error("Upload async processing failed, gameId={}", gameId, e);
            game.setStatus(Game.GameStatus.REJECTED);
            gameRepository.save(game);
        }
    }
}

