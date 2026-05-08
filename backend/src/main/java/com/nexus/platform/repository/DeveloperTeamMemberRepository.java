package com.nexus.platform.repository;

import com.nexus.platform.entity.DeveloperTeamMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeveloperTeamMemberRepository extends JpaRepository<DeveloperTeamMember, Long> {
    List<DeveloperTeamMember> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
    Optional<DeveloperTeamMember> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    boolean existsByOwnerUserIdAndMemberEmailIgnoreCaseAndIdNot(Long ownerUserId, String memberEmail, Long id);
    boolean existsByOwnerUserIdAndMemberEmailIgnoreCase(Long ownerUserId, String memberEmail);
}
