package com.talktally.domain;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

public final class UserAccount {

	private static final Currency BRL = Currency.getInstance("BRL");

	private final UserId id;
	private final String normalizedEmail;
	private final String passwordHash;
	private final String displayName;
	private final Currency defaultCurrency;

	private UserAccount(
			UserId id,
			String normalizedEmail,
			String passwordHash,
			String displayName,
			Currency defaultCurrency) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.normalizedEmail = requireNormalizedEmail(normalizedEmail);
		this.passwordHash = requirePasswordHash(passwordHash);
		this.displayName = requireDisplayName(displayName);
		this.defaultCurrency = Objects.requireNonNull(
				defaultCurrency, "default currency must not be null");
	}

	public static UserAccount create(
			UserId id,
			String normalizedEmail,
			String passwordHash,
			String displayName) {
		return new UserAccount(id, normalizedEmail, passwordHash, displayName, BRL);
	}

	public static UserAccount reconstruct(
			UserId id,
			String normalizedEmail,
			String passwordHash,
			String displayName,
			Currency defaultCurrency) {
		return new UserAccount(id, normalizedEmail, passwordHash, displayName, defaultCurrency);
	}

	private static String requireNormalizedEmail(String value) {
		Objects.requireNonNull(value, "normalized email must not be null");
		if (value.isBlank() || value.length() > 320) {
			throw new IllegalArgumentException("normalized email must contain 1 to 320 characters");
		}
		if (!value.equals(value.strip()) || !value.equals(value.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("email must already be normalized");
		}
		return value;
	}

	private static String requirePasswordHash(String value) {
		Objects.requireNonNull(value, "password hash must not be null");
		if (value.isBlank() || value.length() > 255) {
			throw new IllegalArgumentException("password hash must contain 1 to 255 characters");
		}
		return value;
	}

	private static String requireDisplayName(String value) {
		Objects.requireNonNull(value, "display name must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty() || normalized.length() > 120) {
			throw new IllegalArgumentException("display name must contain 1 to 120 characters");
		}
		return normalized;
	}

	public UserId id() {
		return id;
	}

	public String normalizedEmail() {
		return normalizedEmail;
	}

	public String passwordHash() {
		return passwordHash;
	}

	public String displayName() {
		return displayName;
	}

	public Currency defaultCurrency() {
		return defaultCurrency;
	}
}
