package com.nexus.platform.repository;

import com.nexus.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhone(String phone);
    List<User> findByRoleOrderByCreatedAtDesc(User.UserRole role);
    List<User> findByRoleAndAccountStatusOrderByCreatedAtDesc(User.UserRole role, String accountStatus);
    boolean existsByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);
}
