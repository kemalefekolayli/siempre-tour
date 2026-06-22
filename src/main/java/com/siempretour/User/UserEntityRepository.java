package com.siempretour.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByGoogleId(String googleId);

    Optional<UserEntity> findByPasswordResetTokenHash(String passwordResetTokenHash);

    boolean existsByEmail(String email);
}
