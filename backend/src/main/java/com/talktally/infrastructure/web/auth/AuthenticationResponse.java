package com.talktally.infrastructure.web.auth;

import com.talktally.application.auth.output.AuthenticationOutput;

import java.time.Instant;

public record AuthenticationResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		Instant expiresAt,
		UserAccountResponse user) {

	static AuthenticationResponse from(AuthenticationOutput output) {
		return new AuthenticationResponse(
				output.accessToken(),
				output.tokenType(),
				output.expiresIn(),
				output.expiresAt(),
				UserAccountResponse.from(output.user()));
	}

	@Override
	public String toString() {
		return "AuthenticationResponse[accessToken=REDACTED, tokenType="
				+ tokenType + ", expiresIn=" + expiresIn + ", expiresAt=" + expiresAt
				+ ", user=" + user + "]";
	}
}
