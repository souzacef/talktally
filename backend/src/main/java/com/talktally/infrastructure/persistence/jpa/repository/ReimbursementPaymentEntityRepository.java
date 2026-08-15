package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.ReimbursementPaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReimbursementPaymentEntityRepository
		extends JpaRepository<ReimbursementPaymentJpaEntity, UUID> {

	List<ReimbursementPaymentJpaEntity>
			findAllByClaimIdAndUserIdOrderByReceivedDateAscIdAsc(UUID claimId, UUID userId);

	boolean existsByUserIdAndReceiptTransactionId(UUID userId, UUID transactionId);
}
