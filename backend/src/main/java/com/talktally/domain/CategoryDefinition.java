package com.talktally.domain;

import java.util.Objects;

public record CategoryDefinition(
		CategoryId id,
		String code,
		String displayName,
		CategoryAllowedKind allowedKind,
		boolean builtIn) {

	public CategoryDefinition {
		Objects.requireNonNull(id, "id must not be null");
		code = requireText(code, "code");
		displayName = requireText(displayName, "display name");
		Objects.requireNonNull(allowedKind, "allowed kind must not be null");
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return normalized;
	}
}
