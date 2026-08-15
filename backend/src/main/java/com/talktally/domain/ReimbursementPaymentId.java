package com.talktally.domain;

import java.util.Objects;
import java.util.UUID;

public record ReimbursementPaymentId(UUID value) {

	public ReimbursementPaymentId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static ReimbursementPaymentId generate() {
		return new ReimbursementPaymentId(UUID.randomUUID());
	}

	public static ReimbursementPaymentId from(UUID value) {
		return new ReimbursementPaymentId(value);
	}
}
