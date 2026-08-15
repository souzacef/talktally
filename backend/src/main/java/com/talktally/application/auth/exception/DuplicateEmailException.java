package com.talktally.application.auth.exception;

public final class DuplicateEmailException extends RuntimeException {

	public DuplicateEmailException() {
		super("an account already exists for this email");
	}
}
