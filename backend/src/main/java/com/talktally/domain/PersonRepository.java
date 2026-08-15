package com.talktally.domain;

import java.util.List;
import java.util.Optional;

public interface PersonRepository {

	Person save(Person person);

	Optional<Person> findById(UserId ownerId, PersonId personId);

	Optional<Person> findByNormalizedName(UserId ownerId, String normalizedName);

	List<Person> findAll(UserId ownerId);
}
