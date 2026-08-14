package com.talktally.domain;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {

	public CategoryId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static CategoryId generate() {
		return new CategoryId(UUID.randomUUID());
	}

	public static CategoryId from(UUID value) {
		return new CategoryId(value);
	}
}
