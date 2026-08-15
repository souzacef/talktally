package com.talktally.domain;

import java.util.Locale;
import java.util.Objects;

public record Person(PersonId id, UserId ownerId, String displayName) {

	public static final int MAX_DISPLAY_NAME_LENGTH = 120;

	public Person {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(displayName, "display name must not be null");
		displayName = displayName.strip();
		if (displayName.isEmpty()) {
			throw new IllegalArgumentException("display name must not be blank");
		}
		if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
			throw new IllegalArgumentException(
					"display name must not exceed " + MAX_DISPLAY_NAME_LENGTH + " characters");
		}
	}

	public String normalizedName() {
		return normalizeName(displayName);
	}

	public static String normalizeName(String displayName) {
		Objects.requireNonNull(displayName, "display name must not be null");
		return displayName.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}
}
