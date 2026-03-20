package com.nexus.platform.controller;

import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.service.GameService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostMapping("/upload")
    public Result<Game> uploadGame(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("developerId") Long developerId) {
        return gameService.uploadGame(file, name, description, developerId);
    }

    @GetMapping("/list")
    public Result<java.util.List<Game>> getGameList() {
        return gameService.getGameList();
    }

    @GetMapping("/{appId}")
    public Result<Game> getGame(@PathVariable String appId) {
        return gameService.getGameByAppId(appId);
    }

    @GetMapping("/developer/{developerId}")
    public Result<java.util.List<Game>> getDeveloperGames(@PathVariable Long developerId) {
        return gameService.getDeveloperGames(developerId);
    }

    @GetMapping("/download/{appId}")
    public ResponseEntity<InputStreamResource> downloadGame(@PathVariable String appId) {
        try {
            String fileName = appId + ".zip";
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/approve/{id}")
    public Result<Void> approveGame(@PathVariable Long id) {
        return gameService.approveGame(id);
    }

    @PostMapping("/reject/{id}")
    public Result<Void> rejectGame(@PathVariable Long id) {
        return gameService.rejectGame(id);
    }
}
