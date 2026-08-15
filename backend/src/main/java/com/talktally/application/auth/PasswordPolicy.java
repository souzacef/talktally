package com.talktally.application.auth;

import com.talktally.application.auth.exception.InvalidRegistrationInputException;

public final class PasswordPolicy {

	public static final int MIN_LENGTH = 10;
	public static final int MAX_LENGTH = 128;

	private PasswordPolicy() {
	}

	public static void validate(String password) {
		if (password == null
				|| password.isBlank()
				|| password.length() < MIN_LENGTH
				|| password.length() > MAX_LENGTH) {
			throw invalid();
		}
		boolean hasLetter = password.codePoints().anyMatch(Character::isLetter);
		boolean hasDigit = password.codePoints().anyMatch(Character::isDigit);
		if (!hasLetter || !hasDigit) {
			throw invalid();
		}
	}

	private static InvalidRegistrationInputException invalid() {
		return new InvalidRegistrationInputException(
				"password must contain 10 to 128 characters, including a letter and a digit");
	}
}
