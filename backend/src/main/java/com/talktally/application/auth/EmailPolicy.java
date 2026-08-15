package com.talktally.application.auth;

import com.talktally.application.auth.exception.InvalidRegistrationInputException;

import java.util.Locale;

public final class EmailPolicy {

	private static final int MAX_EMAIL_LENGTH = 320;
	private static final int MAX_LOCAL_PART_LENGTH = 64;
	private static final int MAX_DOMAIN_LABEL_LENGTH = 63;
	private static final String LOCAL_CHARACTERS =
			"abcdefghijklmnopqrstuvwxyz0123456789.!#$%&'*+/=?^_`{|}~-";

	private EmailPolicy() {
	}

	public static String normalize(String email) {
		if (email == null) {
			throw invalid();
		}
		String normalized = email.strip().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()
				|| normalized.length() > MAX_EMAIL_LENGTH
				|| normalized.chars().anyMatch(Character::isWhitespace)) {
			throw invalid();
		}

		int atIndex = normalized.indexOf('@');
		if (atIndex < 1
				|| atIndex != normalized.lastIndexOf('@')
				|| atIndex > MAX_LOCAL_PART_LENGTH
				|| atIndex == normalized.length() - 1) {
			throw invalid();
		}

		String localPart = normalized.substring(0, atIndex);
		String domain = normalized.substring(atIndex + 1);
		if (localPart.startsWith(".")
				|| localPart.endsWith(".")
				|| localPart.contains("..")
				|| localPart.chars().anyMatch(character ->
						LOCAL_CHARACTERS.indexOf(character) < 0)) {
			throw invalid();
		}

		String[] labels = domain.split("\\.", -1);
		if (labels.length < 2) {
			throw invalid();
		}
		for (String label : labels) {
			if (label.isEmpty()
					|| label.length() > MAX_DOMAIN_LABEL_LENGTH
					|| label.startsWith("-")
					|| label.endsWith("-")
					|| label.chars().anyMatch(character ->
							!isAsciiLetterOrDigit(character) && character != '-')) {
				throw invalid();
			}
		}

		return normalized;
	}

	private static boolean isAsciiLetterOrDigit(int character) {
		return character >= 'a' && character <= 'z'
				|| character >= '0' && character <= '9';
	}

	private static InvalidRegistrationInputException invalid() {
		return new InvalidRegistrationInputException("email is invalid");
	}
}
