package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsSensitivePolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsSensitivePolicyRepository extends JpaRepository<OpsSensitivePolicy, Long> {
    List<OpsSensitivePolicy> findAllByOrderByUpdatedAtDesc();

    Optional<OpsSensitivePolicy> findByPolicyCode(String policyCode);
}
