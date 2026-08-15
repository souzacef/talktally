package com.talktally.application.speech.exception;

public class InvalidAudioException extends RuntimeException {

	public InvalidAudioException(String message) {
		super(message);
	}

	public InvalidAudioException(String message, Throwable cause) {
		super(message, cause);
	}
}
