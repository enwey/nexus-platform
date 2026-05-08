package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsNoticeTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsNoticeTemplateRepository extends JpaRepository<OpsNoticeTemplate, Long> {
    List<OpsNoticeTemplate> findAllByOrderByUpdatedAtDesc();

    Optional<OpsNoticeTemplate> findByTemplateCodeAndLanguageTag(String templateCode, String languageTag);
}
