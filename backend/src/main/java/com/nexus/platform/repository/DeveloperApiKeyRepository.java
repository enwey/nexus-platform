package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeveloperApiKeyRepository extends JpaRepository<DeveloperApiKey, Long> {
    List<DeveloperApiKey> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
    Optional<DeveloperApiKey> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    boolean existsByAccessKey(String accessKey);
}
