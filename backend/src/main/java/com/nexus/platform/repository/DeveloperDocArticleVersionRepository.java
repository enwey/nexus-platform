package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperDocArticleVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeveloperDocArticleVersionRepository extends JpaRepository<DeveloperDocArticleVersion, Long> {
    List<DeveloperDocArticleVersion> findByArticleIdOrderByVersionNoDesc(Long articleId);
}
