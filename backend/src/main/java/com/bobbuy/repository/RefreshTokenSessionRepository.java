package com.bobbuy.repository;

import com.bobbuy.model.RefreshTokenSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {
    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

    List<RefreshTokenSession> findAllByFamilyIdAndRevokedAtIsNull(String familyId);
}
