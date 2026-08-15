package com.talktally.application.person;

import com.talktally.application.person.exception.DuplicatePersonException;
import com.talktally.application.person.exception.InvalidPersonInputException;
import com.talktally.application.person.input.CreatePersonInput;
import com.talktally.application.person.output.PersonOutput;
import com.talktally.domain.Person;
import com.talktally.domain.PersonId;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CreatePersonUseCase {

	private final PersonRepository personRepository;

	public CreatePersonUseCase(PersonRepository personRepository) {
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
	}

	@Transactional
	public PersonOutput execute(UserId actorId, CreatePersonInput input) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (input == null || input.displayName() == null) {
			throw new InvalidPersonInputException("display name is required");
		}
		Person person;
		try {
			person = new Person(PersonId.generate(), actorId, input.displayName());
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidPersonInputException(exception.getMessage(), exception);
		}
		if (personRepository.findByNormalizedName(actorId, person.normalizedName()).isPresent()) {
			throw new DuplicatePersonException();
		}
		return toOutput(personRepository.save(person));
	}

	static PersonOutput toOutput(Person person) {
		return new PersonOutput(person.id(), person.displayName());
	}
}
