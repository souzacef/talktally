package com.talktally.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public interface ReimbursementClaimRepository {

	ReimbursementClaim save(ReimbursementClaim claim);

	Optional<ReimbursementClaim> findById(UserId ownerId, ReimbursementClaimId claimId);

	Optional<ReimbursementClaim> findByIdForRepayment(
			UserId ownerId,
			ReimbursementClaimId claimId);

	ReimbursementClaimPage search(UserId ownerId, ReimbursementClaimSearchCriteria criteria);

	List<ReimbursementClaim> findAllByPerson(UserId ownerId, PersonId personId);

	boolean isTransactionLinked(UserId ownerId, TransactionId transactionId);

	default Set<TransactionId> findLinkedTransactionIds(
			UserId ownerId,
			Collection<TransactionId> transactionIds) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(transactionIds, "transaction ids must not be null");
		Set<TransactionId> linked = new LinkedHashSet<>();
		for (TransactionId transactionId : transactionIds) {
			Objects.requireNonNull(transactionId, "transaction id must not be null");
			if (isTransactionLinked(ownerId, transactionId)) {
				linked.add(transactionId);
			}
		}
		return Set.copyOf(linked);
	}
}
