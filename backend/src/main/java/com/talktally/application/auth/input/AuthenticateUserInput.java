package com.talktally.application.auth.input;

public record AuthenticateUserInput(String email, String password) {

	@Override
	public String toString() {
		return "AuthenticateUserInput[credentials=REDACTED]";
	}
}
