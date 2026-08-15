package com.talktally.infrastructure.web.person;

import com.talktally.application.person.input.CreatePersonInput;
import com.talktally.domain.Person;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonRequest(
		@NotBlank @Size(max = Person.MAX_DISPLAY_NAME_LENGTH) String displayName) {

	CreatePersonInput toInput() {
		return new CreatePersonInput(displayName);
	}
}
