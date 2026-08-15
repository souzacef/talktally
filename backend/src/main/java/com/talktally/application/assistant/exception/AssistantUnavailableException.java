package com.talktally.application.assistant.exception;

public final class AssistantUnavailableException extends RuntimeException {

	public AssistantUnavailableException() {
		super("assistant is temporarily unavailable");
	}

	public AssistantUnavailableException(Throwable cause) {
		super("assistant is temporarily unavailable", cause);
	}
}
