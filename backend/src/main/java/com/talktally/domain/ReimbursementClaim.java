package com.talktally.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ReimbursementClaim {

	private static final Comparator<ReimbursementPayment> PAYMENT_ORDER = Comparator
			.comparing(ReimbursementPayment::receivedDate)
			.thenComparing(payment -> payment.id().value());

	private final ReimbursementClaimId id;
	private final UserId ownerId;
	private final TransactionId expenseTransactionId;
	private final PersonId personId;
	private final Money originalAmount;
	private final String note;
	private final List<ReimbursementPayment> payments;

	private ReimbursementClaim(
			ReimbursementClaimId id,
			UserId ownerId,
			TransactionId expenseTransactionId,
			PersonId personId,
			Money originalAmount,
			String note,
			List<ReimbursementPayment> payments) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.ownerId = Objects.requireNonNull(ownerId, "owner id must not be null");
		this.expenseTransactionId = Objects.requireNonNull(
				expenseTransactionId, "expense transaction id must not be null");
		this.personId = Objects.requireNonNull(personId, "person id must not be null");
		this.originalAmount = Objects.requireNonNull(originalAmount, "original amount must not be null");
		if (!originalAmount.isPositive()) {
			throw new IllegalArgumentException("original amount must be greater than zero");
		}
		if (!"BRL".equals(originalAmount.currency().getCurrencyCode())) {
			throw new IllegalArgumentException("reimbursements must use BRL");
		}
		this.note = ReimbursementPayment.normalizeNote(note);
		this.payments = validateAndCopyPayments(payments, originalAmount);
	}

	public static ReimbursementClaim create(
			UserId ownerId,
			TransactionId expenseTransactionId,
			PersonId personId,
			Money originalAmount,
			String note) {
		return new ReimbursementClaim(
				ReimbursementClaimId.generate(),
				ownerId,
				expenseTransactionId,
				personId,
				originalAmount,
				note,
				List.of());
	}

	public static ReimbursementClaim reconstruct(
			ReimbursementClaimId id,
			UserId ownerId,
			TransactionId expenseTransactionId,
			PersonId personId,
			Money originalAmount,
			String note,
			List<ReimbursementPayment> payments) {
		return new ReimbursementClaim(
				id, ownerId, expenseTransactionId, personId, originalAmount, note, payments);
	}

	public ReimbursementClaim addPayment(ReimbursementPayment payment) {
		Objects.requireNonNull(payment, "payment must not be null");
		List<ReimbursementPayment> updated = new ArrayList<>(payments);
		updated.add(payment);
		return new ReimbursementClaim(
				id, ownerId, expenseTransactionId, personId, originalAmount, note, updated);
	}

	private static List<ReimbursementPayment> validateAndCopyPayments(
			List<ReimbursementPayment> payments,
			Money originalAmount) {
		Objects.requireNonNull(payments, "payments must not be null");
		List<ReimbursementPayment> copy = new ArrayList<>(payments.size());
		Set<ReimbursementPaymentId> paymentIds = new HashSet<>();
		Set<TransactionId> receiptIds = new HashSet<>();
		Money reimbursed = Money.zero(originalAmount.currency());
		for (ReimbursementPayment payment : payments) {
			ReimbursementPayment checked = Objects.requireNonNull(payment, "payment must not be null");
			if (!originalAmount.currency().equals(checked.amount().currency())) {
				throw new IllegalArgumentException("payment currency must match claim currency");
			}
			if (!paymentIds.add(checked.id())) {
				throw new IllegalArgumentException("payment ids must be unique");
			}
			if (!receiptIds.add(checked.receiptTransactionId())) {
				throw new IllegalArgumentException("receipt transaction ids must be unique");
			}
			reimbursed = reimbursed.add(checked.amount());
			if (reimbursed.amount().compareTo(originalAmount.amount()) > 0) {
				throw new IllegalArgumentException("payment total must not exceed original amount");
			}
			copy.add(checked);
		}
		copy.sort(PAYMENT_ORDER);
		return List.copyOf(copy);
	}

	public Money amountReimbursed() {
		Money result = Money.zero(originalAmount.currency());
		for (ReimbursementPayment payment : payments) {
			result = result.add(payment.amount());
		}
		return result;
	}

	public Money remainingAmount() {
		return originalAmount.subtract(amountReimbursed());
	}

	public ReimbursementStatus status() {
		Money reimbursed = amountReimbursed();
		if (reimbursed.isZero()) {
			return ReimbursementStatus.PENDING;
		}
		return reimbursed.equals(originalAmount)
				? ReimbursementStatus.PAID
				: ReimbursementStatus.PARTIALLY_PAID;
	}

	public ReimbursementClaimId id() {
		return id;
	}

	public UserId ownerId() {
		return ownerId;
	}

	public TransactionId expenseTransactionId() {
		return expenseTransactionId;
	}

	public PersonId personId() {
		return personId;
	}

	public Money originalAmount() {
		return originalAmount;
	}

	public String note() {
		return note;
	}

	public List<ReimbursementPayment> payments() {
		return payments;
	}
}
