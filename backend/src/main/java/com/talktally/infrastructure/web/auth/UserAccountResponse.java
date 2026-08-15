package com.talktally.infrastructure.web.auth;

import com.talktally.application.auth.output.UserAccountOutput;

import java.util.UUID;

public record UserAccountResponse(
		UUID userId,
		String email,
		String displayName,
		String defaultCurrency) {

	static UserAccountResponse from(UserAccountOutput output) {
		return new UserAccountResponse(
				output.userId().value(), output.email(), output.displayName(), output.defaultCurrency());
	}
}
