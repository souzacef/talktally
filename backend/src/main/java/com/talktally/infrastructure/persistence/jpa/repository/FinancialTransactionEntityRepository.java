package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.FinancialTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FinancialTransactionEntityRepository
		extends JpaRepository<FinancialTransactionJpaEntity, UUID>,
		JpaSpecificationExecutor<FinancialTransactionJpaEntity> {

	Optional<FinancialTransactionJpaEntity> findByIdAndUserId(UUID id, UUID userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			delete from FinancialTransactionJpaEntity transaction
			where transaction.id = :transactionId
			  and transaction.userId = :userId
			""")
	int deleteByIdAndUserId(
			@Param("transactionId") UUID transactionId,
			@Param("userId") UUID userId);
}
