package com.nexus.platform.repository;

import com.nexus.platform.entity.VerificationCodeLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationCodeLogRepository extends JpaRepository<VerificationCodeLog, Long> {
    @Query("""
            select l from VerificationCodeLog l
            where (:account is null or l.account = :account)
              and (:purpose is null or l.purpose = :purpose)
              and (:requestSource is null or l.requestSource = :requestSource)
            order by l.createdAt desc
            """)
    List<VerificationCodeLog> findRecent(
            @Param("account") String account,
            @Param("purpose") String purpose,
            @Param("requestSource") String requestSource,
            Pageable pageable
    );
}
