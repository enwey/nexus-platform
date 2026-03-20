package com.nexus.platform.service;

import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.repository.GameRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public Result<Game> uploadGame(MultipartFile file, String name, String description, Long developerId) {
        try {
            String appId = generateAppId();
            String fileName = appId + ".zip";
            
            String md5 = calculateMD5(file.getBytes());
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/zip")
                            .build()
            );

            String downloadUrl = "/api/v1/game/download/" + appId;

            Game game = new Game();
            game.setAppId(appId);
            game.setName(name);
            game.setDescription(description);
            game.setDownloadUrl(downloadUrl);
            game.setVersion("1.0.0");
            game.setMd5(md5);
            game.setStatus(Game.GameStatus.PENDING);
            game.setDeveloperId(developerId);

            Game savedGame = gameRepository.save(game);
            return Result.success(savedGame);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    public Result<List<Game>> getGameList() {
        List<Game> games = gameRepository.findByStatus(Game.GameStatus.APPROVED);
        return Result.success(games);
    }

    public Result<Game> getGameByAppId(String appId) {
        Game game = gameRepository.findByAppId(appId);
        if (game == null) {
            return Result.error("游戏不存在");
        }
        return Result.success(game);
    }

    public Result<List<Game>> getDeveloperGames(Long developerId) {
        List<Game> games = gameRepository.findByDeveloperId(developerId);
        return Result.success(games);
    }

    public Result<Void> approveGame(Long id) {
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            return Result.error("游戏不存在");
        }
        game.setStatus(Game.GameStatus.APPROVED);
        gameRepository.save(game);
        return Result.success();
    }

    public Result<Void> rejectGame(Long id) {
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            return Result.error("游戏不存在");
        }
        game.setStatus(Game.GameStatus.REJECTED);
        gameRepository.save(game);
        return Result.success();
    }

    private String generateAppId() {
        return "wx" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String calculateMD5(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
