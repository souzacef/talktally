package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.PersonJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonEntityRepository extends JpaRepository<PersonJpaEntity, UUID> {

	Optional<PersonJpaEntity> findByIdAndUserId(UUID id, UUID userId);

	Optional<PersonJpaEntity> findByUserIdAndNormalizedName(UUID userId, String normalizedName);

	List<PersonJpaEntity> findAllByUserIdOrderByNormalizedNameAscIdAsc(UUID userId);
}
