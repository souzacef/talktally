package com.talktally.infrastructure.web.auth;

public record RegistrationRequest(String email, String password, String displayName) {

	@Override
	public String toString() {
		return "RegistrationRequest[credentials=REDACTED]";
	}
}
