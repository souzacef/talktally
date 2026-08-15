package com.talktally.application.auth.input;

public record RegisterUserInput(String email, String password, String displayName) {

	@Override
	public String toString() {
		return "RegisterUserInput[credentials=REDACTED]";
	}
}
