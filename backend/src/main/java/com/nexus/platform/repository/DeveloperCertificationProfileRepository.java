package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperCertificationProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeveloperCertificationProfileRepository extends JpaRepository<DeveloperCertificationProfile, Long> {
    Optional<DeveloperCertificationProfile> findByDeveloperId(Long developerId);
}
