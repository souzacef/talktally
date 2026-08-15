package com.talktally.application.person.exception;

import com.talktally.domain.PersonId;

public final class PersonNotFoundException extends RuntimeException {

	public PersonNotFoundException(PersonId personId) {
		super("person not found: " + personId.value());
	}
}
