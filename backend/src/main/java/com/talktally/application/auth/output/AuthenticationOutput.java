package com.talktally.application.auth.output;

import java.time.Instant;
import java.util.Objects;

public record AuthenticationOutput(
		String accessToken,
		String tokenType,
		long expiresIn,
		Instant expiresAt,
		UserAccountOutput user) {

	public AuthenticationOutput {
		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalArgumentException("access token must not be blank");
		}
		Objects.requireNonNull(tokenType, "token type must not be null");
		if (expiresIn < 1) {
			throw new IllegalArgumentException("expiry duration must be positive");
		}
		Objects.requireNonNull(expiresAt, "expiry instant must not be null");
		Objects.requireNonNull(user, "user must not be null");
	}

	@Override
	public String toString() {
		return "AuthenticationOutput[accessToken=REDACTED, tokenType="
				+ tokenType + ", expiresIn=" + expiresIn + ", expiresAt=" + expiresAt
				+ ", user=" + user + "]";
	}
}
