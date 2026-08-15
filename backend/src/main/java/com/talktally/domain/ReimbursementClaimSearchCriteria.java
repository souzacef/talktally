package com.talktally.domain;

import java.util.Objects;
import java.util.Optional;

public record ReimbursementClaimSearchCriteria(
		Optional<PersonId> personId,
		Optional<ReimbursementStatus> status,
		int page,
		int size) {

	public ReimbursementClaimSearchCriteria {
		Objects.requireNonNull(personId, "person id must not be null");
		Objects.requireNonNull(status, "status must not be null");
		if (page < 0 || size < 1) {
			throw new IllegalArgumentException("invalid reimbursement search page");
		}
	}
}
