package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.AssistantMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssistantMessageEntityRepository extends JpaRepository<AssistantMessageJpaEntity, Long> {

	List<AssistantMessageJpaEntity> findByUserId(UUID userId, Pageable pageable);

	void deleteAllByUserId(UUID userId);
}
