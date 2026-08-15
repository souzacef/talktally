package com.talktally.infrastructure.web.auth;

public record AuthenticationRequest(String email, String password) {

	@Override
	public String toString() {
		return "AuthenticationRequest[credentials=REDACTED]";
	}
}
