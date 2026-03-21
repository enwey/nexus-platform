package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.GameService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return gameService.uploadGame(file, name, description, currentUser);
    }

    @GetMapping("/list")
    public Result<java.util.List<Game>> getGameList(
            @RequestAttribute(value = AuthInterceptor.AUTH_USER_ATTRIBUTE, required = false) User currentUser) {
        return gameService.getGameList(currentUser);
    }

    @GetMapping("/{appId}")
    public Result<Game> getGame(@PathVariable String appId) {
        return gameService.getGameByAppId(appId);
    }

    @GetMapping("/developer/{developerId}")
    public Result<java.util.List<Game>> getDeveloperGames(
            @PathVariable Long developerId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return gameService.getDeveloperGames(developerId, currentUser);
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
