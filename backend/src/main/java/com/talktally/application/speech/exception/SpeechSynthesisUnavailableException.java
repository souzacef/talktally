package com.talktally.application.speech.exception;

public final class SpeechSynthesisUnavailableException extends SpeechSynthesisException {

	public SpeechSynthesisUnavailableException() {
		super("speech synthesis is temporarily unavailable");
	}

	public SpeechSynthesisUnavailableException(Throwable cause) {
		super("speech synthesis is temporarily unavailable", cause);
	}
}
