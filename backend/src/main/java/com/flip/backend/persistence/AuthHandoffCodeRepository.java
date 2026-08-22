package com.flip.backend.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthHandoffCodeRepository extends JpaRepository<AuthHandoffCodeEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AuthHandoffCodeEntity c where c.codeHash = :hash")
    Optional<AuthHandoffCodeEntity> findByCodeHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("delete from AuthHandoffCodeEntity c where c.expiresAt < :cutoff or c.consumedAt is not null")
    int deleteExpiredOrConsumed(@Param("cutoff") Instant cutoff);
}
