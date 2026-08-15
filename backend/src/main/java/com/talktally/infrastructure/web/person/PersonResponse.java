package com.talktally.infrastructure.web.person;

import com.talktally.application.person.output.PersonOutput;

import java.util.UUID;

public record PersonResponse(UUID id, String displayName) {

	static PersonResponse from(PersonOutput output) {
		return new PersonResponse(output.personId().value(), output.displayName());
	}
}
