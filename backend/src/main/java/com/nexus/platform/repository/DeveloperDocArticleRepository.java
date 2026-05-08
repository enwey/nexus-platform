package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperDocArticle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeveloperDocArticleRepository extends JpaRepository<DeveloperDocArticle, Long> {
    List<DeveloperDocArticle> findByDeveloperIdOrderByUpdatedAtDesc(Long developerId);

    List<DeveloperDocArticle> findByDeveloperIdAndDocTypeOrderByUpdatedAtDesc(Long developerId, String docType);

    Optional<DeveloperDocArticle> findByIdAndDeveloperId(Long id, Long developerId);
}
