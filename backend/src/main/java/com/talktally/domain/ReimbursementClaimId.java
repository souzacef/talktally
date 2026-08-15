package com.talktally.domain;

import java.util.Objects;
import java.util.UUID;

public record ReimbursementClaimId(UUID value) {

	public ReimbursementClaimId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static ReimbursementClaimId generate() {
		return new ReimbursementClaimId(UUID.randomUUID());
	}

	public static ReimbursementClaimId from(UUID value) {
		return new ReimbursementClaimId(value);
	}
}
