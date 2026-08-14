package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.FinancialTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FinancialTransactionEntityRepository
		extends JpaRepository<FinancialTransactionJpaEntity, UUID> {

	Optional<FinancialTransactionJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
