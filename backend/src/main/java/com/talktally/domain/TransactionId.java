package com.talktally.domain;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) {

	public TransactionId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static TransactionId generate() {
		return new TransactionId(UUID.randomUUID());
	}

	public static TransactionId from(UUID value) {
		return new TransactionId(value);
	}
}
