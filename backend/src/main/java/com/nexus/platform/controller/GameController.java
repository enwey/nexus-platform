package com.nexus.platform.controller;

import com.nexus.platform.config.AuthInterceptor;
import com.nexus.platform.dto.PageResult;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

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

    @GetMapping("/public/list")
    public Result<java.util.List<Game>> getPublicGameList() {
        return gameService.getGameList(null);
    }

    @GetMapping("/list/page")
    public Result<PageResult<Game>> getGameListPaged(
            @RequestAttribute(value = AuthInterceptor.AUTH_USER_ATTRIBUTE, required = false) User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return gameService.getGameListPaged(currentUser, page, size);
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

    @GetMapping("/developer/{developerId}/page")
    public Result<PageResult<Game>> getDeveloperGamesPaged(
            @PathVariable Long developerId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return gameService.getDeveloperGamesPaged(developerId, currentUser, page, size);
    }

    @GetMapping("/{gameId}/versions")
    public Result<java.util.List<com.nexus.platform.entity.GameVersion>> getGameVersions(
            @PathVariable Long gameId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser) {
        return gameService.getGameVersions(gameId, currentUser);
    }

    @GetMapping("/download-url/{appId}")
    public Result<String> getDownloadUrl(
            @PathVariable String appId,
            @RequestAttribute(value = AuthInterceptor.AUTH_USER_ATTRIBUTE, required = false) User currentUser) {
        return gameService.getPresignedDownloadUrl(appId, currentUser);
    }

    @GetMapping("/download/{appId}")
    public ResponseEntity<Void> downloadGame(@PathVariable String appId,
                                             @RequestAttribute(value = AuthInterceptor.AUTH_USER_ATTRIBUTE, required = false) User currentUser) {
        Result<String> result = gameService.getPresignedDownloadUrl(appId, currentUser);
        if (result.getCode() != 0 || result.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, result.getData()).build();
    }

    @PostMapping("/approve/{id}")
    public Result<Void> approveGame(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) AuditDecisionRequest decision) {
        String reason = decision == null ? null : decision.reason();
        return gameService.approveGame(id, currentUser, request.getRequestURI(), reason);
    }

    @PostMapping("/submit/{id}")
    public Result<Void> submitGame(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) SubmitAuditRequest decision) {
        String note = decision == null ? null : decision.note();
        return gameService.submitGameForAudit(id, currentUser, request.getRequestURI(), note);
    }

    @PostMapping("/{gameId}/submit-version/{versionId}")
    public Result<Void> submitGameVersion(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) SubmitAuditRequest decision) {
        String note = decision == null ? null : decision.note();
        return gameService.submitGameVersionForAudit(gameId, versionId, currentUser, request.getRequestURI(), note);
    }

    @PostMapping("/{gameId}/rollback/{versionId}")
    public Result<Void> rollbackVersion(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) RollbackVersionRequest decision) {
        String reason = decision == null ? null : decision.reason();
        return gameService.rollbackToVersion(gameId, versionId, currentUser, request.getRequestURI(), reason);
    }

    @PostMapping("/reject/{id}")
    public Result<Void> rejectGame(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) User currentUser,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) AuditDecisionRequest decision) {
        String reason = decision == null ? null : decision.reason();
        return gameService.rejectGame(id, currentUser, request.getRequestURI(), reason);
    }
}

record AuditDecisionRequest(String reason) {
}

record SubmitAuditRequest(String note) {
}

record RollbackVersionRequest(String reason) {
}
