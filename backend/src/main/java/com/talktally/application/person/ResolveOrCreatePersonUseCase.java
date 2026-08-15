package com.talktally.application.person;

import com.talktally.application.person.exception.DuplicatePersonException;
import com.talktally.application.person.exception.InvalidPersonInputException;
import com.talktally.application.person.input.CreatePersonInput;
import com.talktally.application.person.output.PersonOutput;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ResolveOrCreatePersonUseCase {

	private final FindPersonByNameUseCase findPersonByNameUseCase;
	private final CreatePersonUseCase createPersonUseCase;

	public ResolveOrCreatePersonUseCase(
			FindPersonByNameUseCase findPersonByNameUseCase,
			CreatePersonUseCase createPersonUseCase) {
		this.findPersonByNameUseCase = Objects.requireNonNull(
				findPersonByNameUseCase, "find person use case must not be null");
		this.createPersonUseCase = Objects.requireNonNull(
				createPersonUseCase, "create person use case must not be null");
	}

	@Transactional
	public PersonOutput execute(UserId actorId, String displayName) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (displayName == null || displayName.isBlank()) {
			throw new InvalidPersonInputException("display name is required");
		}
		return findPersonByNameUseCase.execute(actorId, displayName)
				.orElseGet(() -> createSafely(actorId, displayName));
	}

	private PersonOutput createSafely(UserId actorId, String displayName) {
		try {
			return createPersonUseCase.execute(actorId, new CreatePersonInput(displayName));
		}
		catch (DuplicatePersonException exception) {
			return findPersonByNameUseCase.execute(actorId, displayName).orElseThrow(() -> exception);
		}
	}
}
