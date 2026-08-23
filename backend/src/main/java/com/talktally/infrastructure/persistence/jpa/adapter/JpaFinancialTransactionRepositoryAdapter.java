package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.domain.CategoryId;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionPage;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.FinancialTransactionSearchCriteria;
import com.talktally.domain.Money;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.TransactionId;
import com.talktally.domain.TransactionOccurrence;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.FinancialTransactionJpaEntity;
import com.talktally.infrastructure.persistence.jpa.entity.TransactionOccurrenceJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.FinancialTransactionEntityRepository;
import com.talktally.infrastructure.persistence.jpa.repository.TransactionOccurrenceEntityRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaFinancialTransactionRepositoryAdapter implements FinancialTransactionRepository {

	private final FinancialTransactionEntityRepository transactionRepository;
	private final TransactionOccurrenceEntityRepository occurrenceRepository;
	private final ReimbursementClaimRepository reimbursementClaimRepository;

	public JpaFinancialTransactionRepositoryAdapter(
			FinancialTransactionEntityRepository transactionRepository,
			TransactionOccurrenceEntityRepository occurrenceRepository,
			ReimbursementClaimRepository reimbursementClaimRepository) {
		this.transactionRepository = transactionRepository;
		this.occurrenceRepository = occurrenceRepository;
		this.reimbursementClaimRepository = Objects.requireNonNull(
				reimbursementClaimRepository, "reimbursement repository must not be null");
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

	@Override
	public FinancialTransactionPage search(
			UserId ownerId,
			FinancialTransactionSearchCriteria criteria) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(criteria, "criteria must not be null");

		PageRequest pageable = PageRequest.of(
				criteria.page(),
				criteria.size(),
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		Page<FinancialTransactionJpaEntity> page = transactionRepository.findAll(
				searchSpecification(ownerId.value(), criteria), pageable);
		List<FinancialTransaction> transactions = page.getContent().stream()
				.map(entity -> toDomain(
						entity,
						occurrenceRepository
								.findAllByTransactionIdAndUserIdOrderBySequenceNumberAsc(
										entity.getId(), ownerId.value())))
				.toList();

		return new FinancialTransactionPage(
				transactions,
				page.getNumber(),
				page.getSize(),
				page.getTotalElements());
	}

	@Override
	@Transactional
	public boolean deleteById(UserId ownerId, TransactionId transactionId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(transactionId, "transaction id must not be null");
		return transactionRepository.deleteByIdAndUserId(
				transactionId.value(), ownerId.value()) == 1;
	}

	@Override
	public boolean isLinkedToReimbursement(UserId ownerId, TransactionId transactionId) {
		return reimbursementClaimRepository.isTransactionLinked(ownerId, transactionId);
	}

	@Override
	public Set<TransactionId> findReimbursementManagedTransactionIds(
			UserId ownerId,
			Collection<TransactionId> transactionIds) {
		return reimbursementClaimRepository.findLinkedTransactionIds(ownerId, transactionIds);
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

	private static Specification<FinancialTransactionJpaEntity> searchSpecification(
			UUID ownerId,
			FinancialTransactionSearchCriteria criteria) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("userId"), ownerId));
			criteria.kind().ifPresent(kind ->
					predicates.add(builder.equal(root.get("kind"), kind)));
			criteria.categoryId().ifPresent(categoryId ->
					predicates.add(builder.equal(root.get("categoryId"), categoryId.value())));
			criteria.searchText().ifPresent(searchText -> predicates.add(builder.like(
					builder.lower(root.get("description")),
					"%" + escapeLike(searchText.toLowerCase(Locale.ROOT)) + "%",
					'\\')));

			if (criteria.effectiveDateFrom().isPresent()
					|| criteria.effectiveDateTo().isPresent()) {
				Subquery<Integer> occurrenceExists = query.subquery(Integer.class);
				Root<TransactionOccurrenceJpaEntity> occurrence = occurrenceExists.from(
						TransactionOccurrenceJpaEntity.class);
				List<Predicate> occurrencePredicates = new ArrayList<>();
				occurrencePredicates.add(builder.equal(
						occurrence.get("transactionId"), root.get("id")));
				occurrencePredicates.add(builder.equal(occurrence.get("userId"), ownerId));
				criteria.effectiveDateFrom().ifPresent(from -> occurrencePredicates.add(
						builder.greaterThanOrEqualTo(occurrence.get("effectiveDate"), from)));
				criteria.effectiveDateTo().ifPresent(to -> occurrencePredicates.add(
						builder.lessThanOrEqualTo(occurrence.get("effectiveDate"), to)));
				occurrenceExists.select(builder.literal(1)).where(
						occurrencePredicates.toArray(Predicate[]::new));
				predicates.add(builder.exists(occurrenceExists));
			}

			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String escapeLike(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
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
				occurrences,
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
