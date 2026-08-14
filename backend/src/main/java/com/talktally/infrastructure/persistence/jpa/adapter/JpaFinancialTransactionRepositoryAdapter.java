package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.domain.CategoryId;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.Money;
import com.talktally.domain.TransactionId;
import com.talktally.domain.TransactionOccurrence;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.FinancialTransactionJpaEntity;
import com.talktally.infrastructure.persistence.jpa.entity.TransactionOccurrenceJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.FinancialTransactionEntityRepository;
import com.talktally.infrastructure.persistence.jpa.repository.TransactionOccurrenceEntityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaFinancialTransactionRepositoryAdapter implements FinancialTransactionRepository {

	private final FinancialTransactionEntityRepository transactionRepository;
	private final TransactionOccurrenceEntityRepository occurrenceRepository;

	public JpaFinancialTransactionRepositoryAdapter(
			FinancialTransactionEntityRepository transactionRepository,
			TransactionOccurrenceEntityRepository occurrenceRepository) {
		this.transactionRepository = transactionRepository;
		this.occurrenceRepository = occurrenceRepository;
	}

	@Override
	@Transactional
	public FinancialTransaction save(FinancialTransaction transaction) {
		Objects.requireNonNull(transaction, "transaction must not be null");
		UUID transactionId = transaction.id().value();
		UUID ownerId = transaction.ownerId().value();
		Instant now = Instant.now();

		FinancialTransactionJpaEntity entity = transactionRepository
				.findByIdAndUserId(transactionId, ownerId)
				.map(existing -> update(existing, transaction, now))
				.orElseGet(() -> toNewEntity(transaction, now));

		transactionRepository.saveAndFlush(entity);
		occurrenceRepository.deleteByTransactionIdAndUserId(transactionId, ownerId);
		occurrenceRepository.saveAllAndFlush(toOccurrenceEntities(transaction));

		return findById(transaction.ownerId(), transaction.id())
				.orElseThrow(() -> new IllegalStateException("saved transaction could not be reloaded"));
	}

	@Override
	public Optional<FinancialTransaction> findById(UserId ownerId, TransactionId transactionId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(transactionId, "transaction id must not be null");

		return transactionRepository
				.findByIdAndUserId(transactionId.value(), ownerId.value())
				.map(entity -> toDomain(
						entity,
						occurrenceRepository
								.findAllByTransactionIdAndUserIdOrderBySequenceNumberAsc(
										transactionId.value(), ownerId.value())));
	}

	private static FinancialTransactionJpaEntity update(
			FinancialTransactionJpaEntity entity,
			FinancialTransaction transaction,
			Instant updatedAt) {
		entity.update(
				transaction.kind(),
				transaction.description(),
				transaction.totalAmount().amount(),
				transaction.totalAmount().currency().getCurrencyCode(),
				transaction.categoryId().value(),
				transaction.eventDate(),
				transaction.source(),
				updatedAt);
		return entity;
	}

	private static FinancialTransactionJpaEntity toNewEntity(
			FinancialTransaction transaction,
			Instant now) {
		return new FinancialTransactionJpaEntity(
				transaction.id().value(),
				transaction.ownerId().value(),
				transaction.kind(),
				transaction.description(),
				transaction.totalAmount().amount(),
				transaction.totalAmount().currency().getCurrencyCode(),
				transaction.categoryId().value(),
				transaction.eventDate(),
				transaction.source(),
				now,
				now);
	}

	private static List<TransactionOccurrenceJpaEntity> toOccurrenceEntities(
			FinancialTransaction transaction) {
		return transaction.occurrences().stream()
				.map(occurrence -> new TransactionOccurrenceJpaEntity(
						UUID.randomUUID(),
						transaction.id().value(),
						transaction.ownerId().value(),
						occurrence.sequenceNumber(),
						occurrence.effectiveDate(),
						occurrence.amount().amount(),
						occurrence.amount().currency().getCurrencyCode()))
				.toList();
	}

	private static FinancialTransaction toDomain(
			FinancialTransactionJpaEntity entity,
			List<TransactionOccurrenceJpaEntity> occurrenceEntities) {
		Currency currency = Currency.getInstance(entity.getCurrency());
		List<TransactionOccurrence> occurrences = occurrenceEntities.stream()
				.map(occurrence -> new TransactionOccurrence(
						occurrence.getSequenceNumber(),
						occurrence.getEffectiveDate(),
						Money.of(
								occurrence.getAmount(),
								Currency.getInstance(occurrence.getCurrency()))))
				.toList();

		return FinancialTransaction.reconstruct(
				TransactionId.from(entity.getId()),
				UserId.from(entity.getUserId()),
				entity.getKind(),
				entity.getDescription(),
				Money.of(entity.getTotalAmount(), currency),
				CategoryId.from(entity.getCategoryId()),
				entity.getEventDate(),
				entity.getSource(),
				occurrences);
	}
}
