package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperRuntimeIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeveloperRuntimeIssueRepository extends JpaRepository<DeveloperRuntimeIssue, Long> {
    List<DeveloperRuntimeIssue> findByDeveloperIdOrderByLastOccurredAtDesc(Long developerId);
}
