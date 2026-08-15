package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.ReimbursementClaimJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReimbursementClaimEntityRepository
		extends JpaRepository<ReimbursementClaimJpaEntity, UUID> {

	Optional<ReimbursementClaimJpaEntity> findByIdAndUserId(UUID id, UUID userId);

	List<ReimbursementClaimJpaEntity> findAllByUserIdOrderByCreatedAtDescIdAsc(UUID userId);

	List<ReimbursementClaimJpaEntity>
			findAllByUserIdAndPersonIdOrderByCreatedAtDescIdAsc(UUID userId, UUID personId);

	boolean existsByUserIdAndExpenseTransactionId(UUID userId, UUID transactionId);
}
