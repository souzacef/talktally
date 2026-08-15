package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountEntityRepository extends JpaRepository<UserAccountJpaEntity, UUID> {

	Optional<UserAccountJpaEntity> findByEmail(String email);

	boolean existsByEmail(String email);
}
