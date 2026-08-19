package com.talktally.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class FinancialTransaction {

	private final TransactionId id;
	private final UserId ownerId;
	private final TransactionKind kind;
	private final String description;
	private final Money totalAmount;
	private final CategoryId categoryId;
	private final LocalDate eventDate;
	private final TransactionSource source;
	private final List<TransactionOccurrence> occurrences;

	private FinancialTransaction(
			TransactionId id,
			UserId ownerId,
			TransactionKind kind,
			String description,
			Money totalAmount,
			CategoryId categoryId,
			LocalDate eventDate,
			TransactionSource source,
			List<TransactionOccurrence> occurrences) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.ownerId = Objects.requireNonNull(ownerId, "owner id must not be null");
		this.kind = Objects.requireNonNull(kind, "kind must not be null");
		Objects.requireNonNull(description, "description must not be null");
		this.description = description.strip();
		if (this.description.isEmpty()) {
			throw new IllegalArgumentException("description must not be blank");
		}
		this.totalAmount = Objects.requireNonNull(totalAmount, "total amount must not be null");
		if (!totalAmount.isPositive()) {
			throw new IllegalArgumentException("transaction total must be greater than zero");
		}
		this.categoryId = Objects.requireNonNull(categoryId, "category id must not be null");
		this.eventDate = Objects.requireNonNull(eventDate, "event date must not be null");
		this.source = Objects.requireNonNull(source, "source must not be null");
		this.occurrences = validateAndCopyOccurrences(occurrences, totalAmount);
	}

	public static FinancialTransaction createSingleOccurrence(
			UserId ownerId,
			TransactionKind kind,
			String description,
			Money totalAmount,
			CategoryId categoryId,
			LocalDate eventDate,
			TransactionSource source) {
		return new FinancialTransaction(
				TransactionId.generate(),
				ownerId,
				kind,
				description,
				totalAmount,
				categoryId,
				eventDate,
				source,
				List.of(new TransactionOccurrence(1, eventDate, totalAmount)));
	}

	public static FinancialTransaction createInstallment(
			UserId ownerId,
			TransactionKind kind,
			String description,
			Money totalAmount,
			CategoryId categoryId,
			LocalDate eventDate,
			TransactionSource source,
			int installmentCount,
			LocalDate firstEffectiveDate) {
		return new FinancialTransaction(
				TransactionId.generate(),
				ownerId,
				kind,
				description,
				totalAmount,
				categoryId,
				eventDate,
				source,
				InstallmentSchedule.allocate(totalAmount, installmentCount, firstEffectiveDate));
	}

	public static FinancialTransaction reconstruct(
			TransactionId id,
			UserId ownerId,
			TransactionKind kind,
			String description,
			Money totalAmount,
			CategoryId categoryId,
			LocalDate eventDate,
			TransactionSource source,
			List<TransactionOccurrence> occurrences) {
		return new FinancialTransaction(
				id,
				ownerId,
				kind,
				description,
				totalAmount,
				categoryId,
				eventDate,
				source,
				occurrences);
	}

	private static List<TransactionOccurrence> validateAndCopyOccurrences(
			List<TransactionOccurrence> occurrences,
			Money totalAmount) {
		Objects.requireNonNull(occurrences, "occurrences must not be null");
		if (occurrences.isEmpty()) {
			throw new IllegalArgumentException("transaction must have at least one occurrence");
		}

		List<TransactionOccurrence> copy = new ArrayList<>(occurrences.size());
		for (TransactionOccurrence occurrence : occurrences) {
			copy.add(Objects.requireNonNull(occurrence, "occurrence must not be null"));
		}
		copy.sort(Comparator.comparingInt(TransactionOccurrence::sequenceNumber));

		Money sum = Money.zero(totalAmount.currency());
		for (int index = 0; index < copy.size(); index++) {
			TransactionOccurrence occurrence = copy.get(index);
			int expectedSequence = index + 1;
			if (occurrence.sequenceNumber() != expectedSequence) {
				throw new IllegalArgumentException(
						"occurrence sequence numbers must be unique and contiguous from 1");
			}
			if (!totalAmount.currency().equals(occurrence.amount().currency())) {
				throw new IllegalArgumentException("occurrence currency must match transaction currency");
			}
			sum = sum.add(occurrence.amount());
		}

		if (!sum.equals(totalAmount)) {
			throw new IllegalArgumentException("occurrence amounts must sum exactly to transaction total");
		}
		return List.copyOf(copy);
	}

	public TransactionId id() {
		return id;
	}

	public UserId ownerId() {
		return ownerId;
	}

	public TransactionKind kind() {
		return kind;
	}

	public String description() {
		return description;
	}

	public Money totalAmount() {
		return totalAmount;
	}

	public CategoryId categoryId() {
		return categoryId;
	}

	public LocalDate eventDate() {
		return eventDate;
	}

	public LocalDate firstOccurrenceDate() {
		return occurrences.getFirst().effectiveDate();
	}

	public TransactionSource source() {
		return source;
	}

	public List<TransactionOccurrence> occurrences() {
		return occurrences;
	}
}
