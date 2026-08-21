package com.talktally.domain;

import java.util.List;
import java.util.Optional;

public interface ReimbursementClaimRepository {

	ReimbursementClaim save(ReimbursementClaim claim);

	Optional<ReimbursementClaim> findById(UserId ownerId, ReimbursementClaimId claimId);

	Optional<ReimbursementClaim> findByIdForRepayment(
			UserId ownerId,
			ReimbursementClaimId claimId);

	ReimbursementClaimPage search(UserId ownerId, ReimbursementClaimSearchCriteria criteria);

	List<ReimbursementClaim> findAllByPerson(UserId ownerId, PersonId personId);

	boolean isTransactionLinked(UserId ownerId, TransactionId transactionId);
}
