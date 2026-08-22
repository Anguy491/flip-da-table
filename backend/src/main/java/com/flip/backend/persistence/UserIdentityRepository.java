package com.flip.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, Long> {
    Optional<UserIdentityEntity> findByProviderAndSubject(String provider, String subject);
    Optional<UserIdentityEntity> findByUserIdAndProvider(Long userId, String provider);
}
