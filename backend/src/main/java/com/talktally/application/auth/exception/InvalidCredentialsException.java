package com.talktally.application.auth.exception;

public final class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("invalid email or password");
	}
}
