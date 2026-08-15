package com.talktally.application.speech.exception;

public abstract class SpeechSynthesisException extends RuntimeException {

	protected SpeechSynthesisException(String message) {
		super(message);
	}

	protected SpeechSynthesisException(String message, Throwable cause) {
		super(message, cause);
	}
}
