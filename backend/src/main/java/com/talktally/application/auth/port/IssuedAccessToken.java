package com.talktally.application.auth.port;

import java.time.Instant;
import java.util.Objects;

public record IssuedAccessToken(String value, Instant issuedAt, Instant expiresAt) {

	public IssuedAccessToken {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("token value must not be blank");
		}
		Objects.requireNonNull(issuedAt, "issued at must not be null");
		Objects.requireNonNull(expiresAt, "expires at must not be null");
		if (!expiresAt.isAfter(issuedAt)) {
			throw new IllegalArgumentException("token expiry must be after issuance");
		}
	}

	@Override
	public String toString() {
		return "IssuedAccessToken[value=REDACTED, issuedAt=" + issuedAt + ", expiresAt=" + expiresAt + "]";
	}
}
