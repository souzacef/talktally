package com.talktally.application.speech;

import com.talktally.application.speech.exception.InvalidSpeechTextException;

public final class SpeechSynthesisPolicy {

	public static final int MAX_TEXT_LENGTH = 2_000;

	private SpeechSynthesisPolicy() {
	}

	public static String requireValidText(String text) {
		if (text == null || text.isBlank()) {
			throw new InvalidSpeechTextException("speech text must not be blank");
		}
		String normalized = text.strip();
		if (normalized.length() > MAX_TEXT_LENGTH) {
			throw new InvalidSpeechTextException(
					"speech text must not exceed " + MAX_TEXT_LENGTH + " characters");
		}
		return normalized;
	}
}
