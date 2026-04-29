package com.nexus.platform.repository;

import com.nexus.platform.entity.LaunchAd;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaunchAdRepository extends JpaRepository<LaunchAd, Long> {
    List<LaunchAd> findAllByOrderByUpdatedAtDesc();
    Optional<LaunchAd> findFirstByStatusOrderByUpdatedAtDesc(String status);
}
