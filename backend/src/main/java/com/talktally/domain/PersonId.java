package com.talktally.domain;

import java.util.Objects;
import java.util.UUID;

public record PersonId(UUID value) {

	public PersonId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static PersonId generate() {
		return new PersonId(UUID.randomUUID());
	}

	public static PersonId from(UUID value) {
		return new PersonId(value);
	}
}
