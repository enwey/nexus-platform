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

/**
 * 游戏控制器，处理游戏相关的HTTP请求
 */
@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 上传游戏
     * @param file 游戏文件
     * @param name 游戏名称
     * @param description 游戏描述
     * @param developerId 开发者ID
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<Game> uploadGame(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("developerId") Long developerId) {
        return gameService.uploadGame(file, name, description, developerId);
    }

    /**
     * 获取游戏列表
     * @return 游戏列表
     */
    @GetMapping("/list")
    public Result<java.util.List<Game>> getGameList() {
        return gameService.getGameList();
    }

    /**
     * 根据应用ID获取游戏信息
     * @param appId 应用ID
     * @return 游戏信息
     */
    @GetMapping("/{appId}")
    public Result<Game> getGame(@PathVariable String appId) {
        return gameService.getGameByAppId(appId);
    }

    /**
     * 获取开发者的游戏列表
     * @param developerId 开发者ID
     * @return 游戏列表
     */
    @GetMapping("/developer/{developerId}")
    public Result<java.util.List<Game>> getDeveloperGames(@PathVariable Long developerId) {
        return gameService.getDeveloperGames(developerId);
    }

    /**
     * 下载游戏文件
     * @param appId 应用ID
     * @return 游戏文件流
     */
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

    /**
     * 审核通过游戏
     * @param id 游戏ID
     * @return 操作结果
     */
    @PostMapping("/approve/{id}")
    public Result<Void> approveGame(@PathVariable Long id) {
        return gameService.approveGame(id);
    }

    /**
     * 拒绝游戏
     * @param id 游戏ID
     * @return 操作结果
     */
    @PostMapping("/reject/{id}")
    public Result<Void> rejectGame(@PathVariable Long id) {
        return gameService.rejectGame(id);
    }
}
