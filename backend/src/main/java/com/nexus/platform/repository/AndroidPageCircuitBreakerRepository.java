package com.nexus.platform.repository;

import com.nexus.platform.entity.AndroidPageCircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AndroidPageCircuitBreakerRepository extends JpaRepository<AndroidPageCircuitBreaker, Long> {
    Optional<AndroidPageCircuitBreaker> findByPageKey(String pageKey);

    List<AndroidPageCircuitBreaker> findAllByOrderByUpdatedAtDesc();
}
