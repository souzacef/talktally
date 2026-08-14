package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.TransactionOccurrenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionOccurrenceEntityRepository
		extends JpaRepository<TransactionOccurrenceJpaEntity, UUID> {

	List<TransactionOccurrenceJpaEntity> findAllByTransactionIdAndUserIdOrderBySequenceNumberAsc(
			UUID transactionId,
			UUID userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			delete from TransactionOccurrenceJpaEntity occurrence
			where occurrence.transactionId = :transactionId
			  and occurrence.userId = :userId
			""")
	int deleteByTransactionIdAndUserId(
			@Param("transactionId") UUID transactionId,
			@Param("userId") UUID userId);
}
