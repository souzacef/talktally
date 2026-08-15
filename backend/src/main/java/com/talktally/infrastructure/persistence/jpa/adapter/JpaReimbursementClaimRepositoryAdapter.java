package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.domain.Money;
import com.talktally.domain.PersonId;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementClaimPage;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.ReimbursementClaimSearchCriteria;
import com.talktally.domain.ReimbursementPayment;
import com.talktally.domain.ReimbursementPaymentId;
import com.talktally.domain.TransactionId;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.ReimbursementClaimJpaEntity;
import com.talktally.infrastructure.persistence.jpa.entity.ReimbursementPaymentJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.ReimbursementClaimEntityRepository;
import com.talktally.infrastructure.persistence.jpa.repository.ReimbursementPaymentEntityRepository;
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
public class JpaReimbursementClaimRepositoryAdapter implements ReimbursementClaimRepository {

	private final ReimbursementClaimEntityRepository claimRepository;
	private final ReimbursementPaymentEntityRepository paymentRepository;

	public JpaReimbursementClaimRepositoryAdapter(
			ReimbursementClaimEntityRepository claimRepository,
			ReimbursementPaymentEntityRepository paymentRepository) {
		this.claimRepository = Objects.requireNonNull(
				claimRepository, "claim repository must not be null");
		this.paymentRepository = Objects.requireNonNull(
				paymentRepository, "payment repository must not be null");
	}

	@Override
	@Transactional
	public ReimbursementClaim save(ReimbursementClaim claim) {
		Objects.requireNonNull(claim, "claim must not be null");
		Instant now = Instant.now();
		ReimbursementClaimJpaEntity entity = claimRepository
				.findByIdAndUserId(claim.id().value(), claim.ownerId().value())
				.map(existing -> {
					existing.update(
							claim.personId().value(),
							claim.originalAmount().amount(),
							claim.originalAmount().currency().getCurrencyCode(),
							claim.note(),
							now);
					return existing;
				})
				.orElseGet(() -> new ReimbursementClaimJpaEntity(
						claim.id().value(),
						claim.ownerId().value(),
						claim.expenseTransactionId().value(),
						claim.personId().value(),
						claim.originalAmount().amount(),
						claim.originalAmount().currency().getCurrencyCode(),
						claim.note(),
						now,
						now));
		claimRepository.saveAndFlush(entity);
		paymentRepository.saveAllAndFlush(claim.payments().stream()
				.map(payment -> toEntity(claim, payment, now))
				.toList());
		return findById(claim.ownerId(), claim.id())
				.orElseThrow(() -> new IllegalStateException("saved claim could not be reloaded"));
	}

	@Override
	public Optional<ReimbursementClaim> findById(
			UserId ownerId,
			ReimbursementClaimId claimId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(claimId, "claim id must not be null");
		return claimRepository.findByIdAndUserId(claimId.value(), ownerId.value())
				.map(this::toDomain);
	}

	@Override
	public ReimbursementClaimPage search(
			UserId ownerId,
			ReimbursementClaimSearchCriteria criteria) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(criteria, "criteria must not be null");
		List<ReimbursementClaimJpaEntity> entities = criteria.personId()
				.map(personId -> claimRepository
						.findAllByUserIdAndPersonIdOrderByCreatedAtDescIdAsc(
								ownerId.value(), personId.value()))
				.orElseGet(() -> claimRepository
						.findAllByUserIdOrderByCreatedAtDescIdAsc(ownerId.value()));
		List<ReimbursementClaim> matching = entities.stream()
				.map(this::toDomain)
				.filter(claim -> criteria.status()
						.map(status -> claim.status() == status)
						.orElse(true))
				.toList();
		long offset = (long) criteria.page() * criteria.size();
		int from = (int) Math.min(offset, matching.size());
		int to = Math.min(from + criteria.size(), matching.size());
		return new ReimbursementClaimPage(
				matching.subList(from, to),
				criteria.page(),
				criteria.size(),
				matching.size());
	}

	@Override
	public List<ReimbursementClaim> findAllByPerson(UserId ownerId, PersonId personId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(personId, "person id must not be null");
		return claimRepository.findAllByUserIdAndPersonIdOrderByCreatedAtDescIdAsc(
						ownerId.value(), personId.value())
				.stream()
				.map(this::toDomain)
				.toList();
	}

	@Override
	public boolean isTransactionLinked(UserId ownerId, TransactionId transactionId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(transactionId, "transaction id must not be null");
		UUID owner = ownerId.value();
		UUID transaction = transactionId.value();
		return claimRepository.existsByUserIdAndExpenseTransactionId(owner, transaction)
				|| paymentRepository.existsByUserIdAndReceiptTransactionId(owner, transaction);
	}

	private ReimbursementClaim toDomain(ReimbursementClaimJpaEntity entity) {
		List<ReimbursementPayment> payments = paymentRepository
				.findAllByClaimIdAndUserIdOrderByReceivedDateAscIdAsc(
						entity.getId(), entity.getUserId())
				.stream()
				.map(JpaReimbursementClaimRepositoryAdapter::toDomain)
				.toList();
		return ReimbursementClaim.reconstruct(
				ReimbursementClaimId.from(entity.getId()),
				UserId.from(entity.getUserId()),
				TransactionId.from(entity.getExpenseTransactionId()),
				PersonId.from(entity.getPersonId()),
				Money.of(entity.getOriginalAmount(), Currency.getInstance(entity.getCurrency())),
				entity.getNote(),
				payments);
	}

	private static ReimbursementPayment toDomain(ReimbursementPaymentJpaEntity entity) {
		return new ReimbursementPayment(
				ReimbursementPaymentId.from(entity.getId()),
				Money.of(entity.getAmount(), Currency.getInstance(entity.getCurrency())),
				entity.getReceivedDate(),
				TransactionId.from(entity.getReceiptTransactionId()),
				entity.getNote());
	}

	private static ReimbursementPaymentJpaEntity toEntity(
			ReimbursementClaim claim,
			ReimbursementPayment payment,
			Instant createdAt) {
		return new ReimbursementPaymentJpaEntity(
				payment.id().value(),
				claim.ownerId().value(),
				claim.id().value(),
				payment.receiptTransactionId().value(),
				payment.amount().amount(),
				payment.amount().currency().getCurrencyCode(),
				payment.receivedDate(),
				payment.note(),
				createdAt);
	}
}
