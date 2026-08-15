package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.domain.Person;
import com.talktally.domain.PersonId;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.PersonJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.PersonEntityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaPersonRepositoryAdapter implements PersonRepository {

	private final PersonEntityRepository repository;

	public JpaPersonRepositoryAdapter(PersonEntityRepository repository) {
		this.repository = Objects.requireNonNull(repository, "person repository must not be null");
	}

	@Override
	@Transactional
	public Person save(Person person) {
		Objects.requireNonNull(person, "person must not be null");
		Instant now = Instant.now();
		PersonJpaEntity entity = repository
				.findByIdAndUserId(person.id().value(), person.ownerId().value())
				.map(existing -> {
					existing.update(person.displayName(), person.normalizedName(), now);
					return existing;
				})
				.orElseGet(() -> new PersonJpaEntity(
						person.id().value(),
						person.ownerId().value(),
						person.displayName(),
						person.normalizedName(),
						now,
						now));
		return toDomain(repository.saveAndFlush(entity));
	}

	@Override
	public Optional<Person> findById(UserId ownerId, PersonId personId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(personId, "person id must not be null");
		return repository.findByIdAndUserId(personId.value(), ownerId.value())
				.map(JpaPersonRepositoryAdapter::toDomain);
	}

	@Override
	public Optional<Person> findByNormalizedName(UserId ownerId, String normalizedName) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		Objects.requireNonNull(normalizedName, "normalized name must not be null");
		return repository.findByUserIdAndNormalizedName(ownerId.value(), normalizedName)
				.map(JpaPersonRepositoryAdapter::toDomain);
	}

	@Override
	public List<Person> findAll(UserId ownerId) {
		Objects.requireNonNull(ownerId, "owner id must not be null");
		return repository.findAllByUserIdOrderByNormalizedNameAscIdAsc(ownerId.value())
				.stream()
				.map(JpaPersonRepositoryAdapter::toDomain)
				.toList();
	}

	private static Person toDomain(PersonJpaEntity entity) {
		return new Person(
				PersonId.from(entity.getId()),
				UserId.from(entity.getUserId()),
				entity.getDisplayName());
	}
}
