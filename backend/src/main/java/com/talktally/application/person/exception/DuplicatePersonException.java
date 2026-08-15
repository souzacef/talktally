package com.talktally.application.person.exception;

public final class DuplicatePersonException extends RuntimeException {

	public DuplicatePersonException() {
		super("a person with that name already exists");
	}
}
