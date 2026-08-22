package com.flip.backend.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetTokenEntity t where t.tokenHash = :hash")
    Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(@Param("hash") String hash);

    List<PasswordResetTokenEntity> findByUserIdAndUsedAtIsNull(Long userId);
    Optional<PasswordResetTokenEntity> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    @Modifying
    @Query("delete from PasswordResetTokenEntity t where t.expiresAt < :cutoff or t.usedAt is not null")
    int deleteExpiredOrUsed(@Param("cutoff") Instant cutoff);
}
