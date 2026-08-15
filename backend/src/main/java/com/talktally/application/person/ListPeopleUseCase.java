package com.talktally.application.person;

import com.talktally.application.person.output.PersonOutput;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ListPeopleUseCase {

	private final PersonRepository personRepository;

	public ListPeopleUseCase(PersonRepository personRepository) {
		this.personRepository = Objects.requireNonNull(
				personRepository, "person repository must not be null");
	}

	@Transactional(readOnly = true)
	public List<PersonOutput> execute(UserId actorId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		return personRepository.findAll(actorId).stream()
				.map(CreatePersonUseCase::toOutput)
				.toList();
	}
}
