package com.nexus.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.platform.config.GamePackageProperties;
import com.nexus.platform.dto.DeveloperVersionPreflightDto;
import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.entity.GameVersion;
import com.nexus.platform.entity.User;
import com.nexus.platform.repository.DeveloperGameMetricDailyRepository;
import com.nexus.platform.repository.DeveloperRuntimeIssueRepository;
import com.nexus.platform.repository.GameRepository;
import com.nexus.platform.repository.GameVersionRepository;
import com.nexus.platform.repository.OpsGameCategoryRepository;
import io.minio.MinioClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GameServicePreflightTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameVersionRepository gameVersionRepository;
    @Mock
    private OpsGameCategoryRepository opsGameCategoryRepository;
    @Mock
    private DeveloperGameMetricDailyRepository developerGameMetricDailyRepository;
    @Mock
    private DeveloperRuntimeIssueRepository developerRuntimeIssueRepository;
    @Mock
    private MinioClient minioClient;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UploadProcessingService uploadProcessingService;
    @Mock
    private SecureGamePackageService secureGamePackageService;
    @Mock
    private HostedMiniAppPackageService hostedMiniAppPackageService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = spy(new GameService(
                gameRepository,
                gameVersionRepository,
                opsGameCategoryRepository,
                developerGameMetricDailyRepository,
                developerRuntimeIssueRepository,
                minioClient,
                auditLogService,
                uploadProcessingService,
                secureGamePackageService,
                hostedMiniAppPackageService,
                stringRedisTemplate,
                new ObjectMapper(),
                new GamePackageProperties()
        ));
        ReflectionTestUtils.setField(gameService, "publicBaseUrl", "http://127.0.0.1:8080/api/v1");
        Mockito.lenient().doAnswer(invocation -> {
            GameVersion version = invocation.getArgument(0);
            version.setHostedManifestValid(Boolean.TRUE);
            version.setHostedManifestSummary("manifest ok");
            return null;
        }).when(gameService).attachHostedManifestReport(any(GameVersion.class));
    }

    @Test
    void getDeveloperVersionPreflight_rejectsOtherDeveloperAccess() {
        User currentUser = developer(99L);

        Result<DeveloperVersionPreflightDto> result = gameService.getDeveloperVersionPreflight(1L, 10L, 100L, currentUser, false);

        assertEquals(-1, result.getCode());
        assertEquals("No permission to view other developer preflight data", result.getMessage());
    }

    @Test
    void submitGameVersionForAudit_blocksDuplicatePackage() {
        Game game = game(10L, 1L);
        GameVersion version = version(100L, 10L, "1.0.1", "md5-same");
        User currentUser = developer(1L);

        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(gameVersionRepository.findByIdAndGameId(100L, 10L)).thenReturn(Optional.of(version));
        when(gameVersionRepository.existsByGameIdAndStatus(10L, GameVersion.VersionStatus.SUBMITTED)).thenReturn(false);
        when(gameVersionRepository.existsByGameIdAndSourceMd5AndIdNot(10L, "md5-same", 100L)).thenReturn(true);
        when(gameVersionRepository.findByGameIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(version));

        Result<Void> result = gameService.submitGameVersionForAudit(10L, 100L, currentUser, "/game/10/submit-version/100", "ship it", false);

        assertEquals(-1, result.getCode());
        assertEquals("Another version already uses the same uploaded package", result.getMessage());
        verify(gameVersionRepository, never()).save(any(GameVersion.class));
    }

    @Test
    void submitGameVersionForAudit_updatesStateWhenPreflightPasses() {
        Game game = game(10L, 1L);
        GameVersion version = version(100L, 10L, "1.0.1", "md5-unique");
        User currentUser = developer(1L);

        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(gameVersionRepository.findByIdAndGameId(100L, 10L)).thenReturn(Optional.of(version));
        when(gameVersionRepository.existsByGameIdAndStatus(10L, GameVersion.VersionStatus.SUBMITTED)).thenReturn(false);
        when(gameVersionRepository.existsByGameIdAndSourceMd5AndIdNot(10L, "md5-unique", 100L)).thenReturn(false);
        when(gameVersionRepository.findByGameIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(version));

        Result<Void> result = gameService.submitGameVersionForAudit(10L, 100L, currentUser, "/game/10/submit-version/100", "ship it", false);

        assertEquals(0, result.getCode());
        assertEquals(GameVersion.VersionStatus.SUBMITTED, version.getStatus());
        assertEquals(Game.GameStatus.PENDING, game.getStatus());
        assertTrue(Boolean.FALSE.equals(version.getForcedUpdate()));
    }

    private User developer(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole(User.UserRole.DEVELOPER);
        user.setUsername("dev-" + id);
        return user;
    }

    private Game game(Long gameId, Long developerId) {
        Game game = new Game();
        game.setId(gameId);
        game.setDeveloperId(developerId);
        game.setAppId("app-" + gameId);
        game.setName("Game " + gameId);
        game.setStatus(Game.GameStatus.DRAFT);
        return game;
    }

    private GameVersion version(Long versionId, Long gameId, String versionName, String sourceMd5) {
        GameVersion version = new GameVersion();
        version.setId(versionId);
        version.setGameId(gameId);
        version.setVersionName(versionName);
        version.setSourceMd5(sourceMd5);
        version.setMd5("runtime-md5");
        version.setStorageKey("games/runtime/" + versionName + ".zip");
        version.setSourceStorageKey("games/source/" + versionName + ".zip");
        version.setStatus(GameVersion.VersionStatus.DRAFT);
        version.setForcedUpdate(Boolean.FALSE);
        return version;
    }

}
