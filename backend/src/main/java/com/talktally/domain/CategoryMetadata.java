package com.talktally.domain;

import java.util.Objects;
import java.util.Set;

public record CategoryMetadata(CategoryId id, Set<TransactionKind> allowedKinds) {

	public CategoryMetadata {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(allowedKinds, "allowed kinds must not be null");
		allowedKinds = Set.copyOf(allowedKinds);
		if (allowedKinds.isEmpty()) {
			throw new IllegalArgumentException("allowed kinds must not be empty");
		}
	}

	public boolean allows(TransactionKind kind) {
		return allowedKinds.contains(Objects.requireNonNull(kind, "kind must not be null"));
	}
}
