package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.ReimbursementPaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReimbursementPaymentEntityRepository
		extends JpaRepository<ReimbursementPaymentJpaEntity, UUID> {

	List<ReimbursementPaymentJpaEntity>
			findAllByClaimIdAndUserIdOrderByReceivedDateAscIdAsc(UUID claimId, UUID userId);

	boolean existsByUserIdAndReceiptTransactionId(UUID userId, UUID transactionId);

	@Query("""
			SELECT payment.receiptTransactionId
			FROM ReimbursementPaymentJpaEntity payment
			WHERE payment.userId = :userId
			  AND payment.receiptTransactionId IN :transactionIds
			""")
	List<UUID> findLinkedReceiptTransactionIds(
			@Param("userId") UUID userId,
			@Param("transactionIds") Collection<UUID> transactionIds);
}
