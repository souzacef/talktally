package com.talktally.application.person;

import com.talktally.application.person.output.PersonOutput;
import com.talktally.domain.Person;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class FindPersonByNameUseCase {

	private final PersonRepository personRepository;

	public FindPersonByNameUseCase(PersonRepository personRepository) {
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
	}

	@Transactional(readOnly = true)
	public Optional<PersonOutput> execute(UserId actorId, String displayName) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		if (displayName == null || displayName.isBlank()) {
			return Optional.empty();
		}
		String normalizedName;
		try {
			normalizedName = Person.normalizeName(displayName);
		}
		catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
		return personRepository.findByNormalizedName(actorId, normalizedName)
				.map(CreatePersonUseCase::toOutput);
	}
}
