package com.nexus.platform.service;

import com.nexus.platform.dto.GameReviewAppealCreateRequest;
import com.nexus.platform.dto.GameReviewAppealDecisionRequest;
import com.nexus.platform.dto.GameReviewAppealDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameReviewAppeal;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameReviewAppealRepository;
import com.nexus.platform.repository.GameVersionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameReviewAppealService {
    private final GameRepository gameRepository;
    private final GameVersionRepository gameVersionRepository;
    private final GameReviewAppealRepository gameReviewAppealRepository;
    private final AuditLogService auditLogService;

    public Result<List<GameReviewAppealDto>> listDeveloperAppeals(User currentUser) {
        if (currentUser == null) {
            return Result.error("User not found");
        }
        return Result.success(gameReviewAppealRepository.findByDeveloperIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(this::toDto)
                .toList());
    }

    public Result<List<GameReviewAppealDto>> listOpsAppeals() {
        return Result.success(gameReviewAppealRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .toList());
    }

    @Transactional
    public Result<GameReviewAppealDto> createAppeal(
            Long gameId,
            Long versionId,
            GameReviewAppealCreateRequest request,
            User currentUser,
            String requestUri
    ) {
        if (currentUser == null) {
            return Result.error("User not found");
        }
        String appealReason = request == null || request.appealReason() == null ? null : request.appealReason().trim();
        if (appealReason == null || appealReason.length() < 2) {
            return Result.error("Appeal reason must be at least 2 chars");
        }
        if (appealReason.length() > 512) {
            return Result.error("Appeal reason is too long");
        }
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || !currentUser.getId().equals(game.getDeveloperId())) {
            auditLogService.logOpsAudit("GAME_REVIEW_APPEAL_CREATE", currentUser, gameId, null, false, "No permission to appeal this game", requestUri);
            return Result.error("No permission to appeal this game");
        }
        GameVersion version = gameVersionRepository.findByIdAndGameId(versionId, gameId).orElse(null);
        if (version == null) {
            auditLogService.logOpsAudit("GAME_REVIEW_APPEAL_CREATE", currentUser, gameId, game.getAppId(), false, "Version not found", requestUri);
            return Result.error("Version not found");
        }
        if (version.getStatus() != GameVersion.VersionStatus.REJECTED) {
            auditLogService.logOpsAudit("GAME_REVIEW_APPEAL_CREATE", currentUser, gameId, game.getAppId(), false, "Only rejected versions can be appealed", requestUri);
            return Result.error("Only rejected versions can be appealed");
        }
        if (gameReviewAppealRepository.existsByVersionIdAndAppealStatus(versionId, "SUBMITTED")) {
            auditLogService.logOpsAudit("GAME_REVIEW_APPEAL_CREATE", currentUser, gameId, game.getAppId(), false, "Appeal is already pending", requestUri);
            return Result.error("Appeal is already pending");
        }

        GameReviewAppeal appeal = new GameReviewAppeal();
        appeal.setGameId(gameId);
        appeal.setVersionId(versionId);
        appeal.setDeveloperId(currentUser.getId());
        appeal.setAppealStatus("SUBMITTED");
        appeal.setAppealReason(appealReason);
        appeal.setRejectionSnapshot(version.getAuditReason());
        GameReviewAppeal saved = gameReviewAppealRepository.save(appeal);
        auditLogService.logOpsAudit("GAME_REVIEW_APPEAL_CREATE", currentUser, gameId, game.getAppId(), true, "version=" + version.getVersionName(), requestUri);
        return Result.success(toDto(saved));
    }

    @Transactional
    public Result<GameReviewAppealDto> reviewAppeal(
            Long appealId,
            GameReviewAppealDecisionRequest request,
            User currentUser,
            String requestUri
    ) {
        if (currentUser == null || currentUser.getRole() != User.UserRole.ADMIN) {
            return Result.error("No permission to review appeal");
        }
        GameReviewAppeal appeal = gameReviewAppealRepository.findById(appealId).orElse(null);
        if (appeal == null) {
            return Result.error("Appeal not found");
        }
        if (!"SUBMITTED".equals(appeal.getAppealStatus())) {
            return Result.error("Only submitted appeals can be reviewed");
        }
        String decision = request == null || request.decision() == null ? null : request.decision().trim().toUpperCase();
        String note = request == null || request.reviewNote() == null ? null : request.reviewNote().trim();
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) {
            return Result.error("Invalid appeal decision");
        }
        if (note == null || note.length() < 2) {
            return Result.error("Review note must be at least 2 chars");
        }
        if (note.length() > 512) {
            return Result.error("Review note is too long");
        }

        Game game = gameRepository.findById(appeal.getGameId()).orElse(null);
        GameVersion version = gameVersionRepository.findByIdAndGameId(appeal.getVersionId(), appeal.getGameId()).orElse(null);
        if (game == null || version == null) {
            return Result.error("Appeal target not found");
        }
        if ("APPROVE".equals(decision)) {
            if (version.getStatus() != GameVersion.VersionStatus.REJECTED) {
                return Result.error("Only rejected versions can re-enter review");
            }
            version.setStatus(GameVersion.VersionStatus.SUBMITTED);
            version.setAssignedReviewerId(null);
            version.setAssignedAt(null);
            version.setAuditReason("Appeal approved: " + note);
            gameVersionRepository.save(version);
            game.setStatus(Game.GameStatus.PENDING);
            gameRepository.save(game);
        }

        appeal.setAppealStatus("APPROVE".equals(decision) ? "APPROVED" : "REJECTED");
        appeal.setReviewNote(note);
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewedBy(currentUser.getId());
        GameReviewAppeal saved = gameReviewAppealRepository.save(appeal);
        auditLogService.logOpsAudit("GAME_REVIEW_APPEAL_DECISION", currentUser, game.getId(), game.getAppId(), true, decision + ":" + note, requestUri);
        return Result.success(toDto(saved));
    }

    private GameReviewAppealDto toDto(GameReviewAppeal row) {
        Game game = gameRepository.findById(row.getGameId()).orElse(null);
        GameVersion version = gameVersionRepository.findByIdAndGameId(row.getVersionId(), row.getGameId()).orElse(null);
        return new GameReviewAppealDto(
                row.getId(),
                row.getGameId(),
                row.getVersionId(),
                row.getDeveloperId(),
                game == null ? null : game.getAppId(),
                game == null ? null : game.getName(),
                version == null ? null : version.getVersionName(),
                version == null || version.getStatus() == null ? null : version.getStatus().name(),
                row.getAppealStatus(),
                row.getAppealReason(),
                row.getRejectionSnapshot(),
                row.getReviewNote(),
                row.getSubmittedAt(),
                row.getReviewedAt(),
                row.getReviewedBy(),
                row.getUpdatedAt()
        );
    }
}
