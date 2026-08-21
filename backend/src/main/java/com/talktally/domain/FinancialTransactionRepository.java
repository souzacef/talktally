package com.talktally.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public interface FinancialTransactionRepository {

	FinancialTransaction save(FinancialTransaction transaction);

	Optional<FinancialTransaction> findById(UserId ownerId, TransactionId transactionId);

	FinancialTransactionPage search(
			UserId ownerId,
			FinancialTransactionSearchCriteria criteria);

	boolean deleteById(UserId ownerId, TransactionId transactionId);

	default boolean isLinkedToReimbursement(UserId ownerId, TransactionId transactionId) {
		return false;
	}

	default Set<TransactionId> findReimbursementManagedTransactionIds(
			UserId ownerId,
			Collection<TransactionId> transactionIds) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(transactionIds, "transaction ids must not be null");
		Set<TransactionId> linked = new LinkedHashSet<>();
		for (TransactionId transactionId : transactionIds) {
			Objects.requireNonNull(transactionId, "transaction id must not be null");
			if (isLinkedToReimbursement(ownerId, transactionId)) {
				linked.add(transactionId);
			}
		}
		return Set.copyOf(linked);
	}
}
