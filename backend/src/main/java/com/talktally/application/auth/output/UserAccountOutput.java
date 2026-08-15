package com.talktally.application.auth.output;

import com.talktally.domain.UserId;

import java.util.Objects;

public record UserAccountOutput(
		UserId userId,
		String email,
		String displayName,
		String defaultCurrency) {

	public UserAccountOutput {
		Objects.requireNonNull(userId, "user id must not be null");
		Objects.requireNonNull(email, "email must not be null");
		Objects.requireNonNull(displayName, "display name must not be null");
		Objects.requireNonNull(defaultCurrency, "default currency must not be null");
	}
}
