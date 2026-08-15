package com.talktally.application.speech.exception;

public final class SpeechRecognitionUnavailableException extends RuntimeException {

	public SpeechRecognitionUnavailableException() {
		super("speech recognition is temporarily unavailable");
	}

	public SpeechRecognitionUnavailableException(Throwable cause) {
		super("speech recognition is temporarily unavailable", cause);
	}
}
