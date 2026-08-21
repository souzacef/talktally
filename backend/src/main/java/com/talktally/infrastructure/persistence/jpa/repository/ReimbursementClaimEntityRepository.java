package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.ReimbursementClaimJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReimbursementClaimEntityRepository
		extends JpaRepository<ReimbursementClaimJpaEntity, UUID> {

	Optional<ReimbursementClaimJpaEntity> findByIdAndUserId(UUID id, UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT claim
			FROM ReimbursementClaimJpaEntity claim
			WHERE claim.id = :id AND claim.userId = :userId
			""")
	Optional<ReimbursementClaimJpaEntity> findByIdAndUserIdForUpdate(
			@Param("id") UUID id,
			@Param("userId") UUID userId);

	List<ReimbursementClaimJpaEntity> findAllByUserIdOrderByCreatedAtDescIdAsc(UUID userId);

	List<ReimbursementClaimJpaEntity>
			findAllByUserIdAndPersonIdOrderByCreatedAtDescIdAsc(UUID userId, UUID personId);

	boolean existsByUserIdAndExpenseTransactionId(UUID userId, UUID transactionId);

	@Query("""
			SELECT claim.expenseTransactionId
			FROM ReimbursementClaimJpaEntity claim
			WHERE claim.userId = :userId
			  AND claim.expenseTransactionId IN :transactionIds
			""")
	List<UUID> findLinkedExpenseTransactionIds(
			@Param("userId") UUID userId,
			@Param("transactionIds") Collection<UUID> transactionIds);
}
