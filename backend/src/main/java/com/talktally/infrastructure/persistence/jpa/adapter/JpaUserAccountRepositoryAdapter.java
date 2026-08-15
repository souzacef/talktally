package com.talktally.infrastructure.persistence.jpa.adapter;

import com.talktally.application.auth.exception.DuplicateEmailException;
import com.talktally.domain.UserAccount;
import com.talktally.domain.UserAccountRepository;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import com.talktally.infrastructure.persistence.jpa.repository.UserAccountEntityRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaUserAccountRepositoryAdapter implements UserAccountRepository {

	private final UserAccountEntityRepository userAccountRepository;

	public JpaUserAccountRepositoryAdapter(UserAccountEntityRepository userAccountRepository) {
		this.userAccountRepository = Objects.requireNonNull(
				userAccountRepository, "user account repository must not be null");
	}

	@Override
	@Transactional
	public UserAccount save(UserAccount account) {
		Objects.requireNonNull(account, "account must not be null");
		Instant now = Instant.now();
		UserAccountJpaEntity entity = userAccountRepository.findById(account.id().value())
				.map(existing -> update(existing, account, now))
				.orElseGet(() -> toNewEntity(account, now));

		try {
			return toDomain(userAccountRepository.saveAndFlush(entity));
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateEmailException();
		}
	}

	@Override
	public Optional<UserAccount> findById(UserId userId) {
		Objects.requireNonNull(userId, "user id must not be null");
		return userAccountRepository.findById(userId.value())
				.map(JpaUserAccountRepositoryAdapter::toDomain);
	}

	@Override
	public Optional<UserAccount> findByNormalizedEmail(String normalizedEmail) {
		Objects.requireNonNull(normalizedEmail, "normalized email must not be null");
		return userAccountRepository.findByEmail(normalizedEmail)
				.map(JpaUserAccountRepositoryAdapter::toDomain);
	}

	@Override
	public boolean existsByNormalizedEmail(String normalizedEmail) {
		Objects.requireNonNull(normalizedEmail, "normalized email must not be null");
		return userAccountRepository.existsByEmail(normalizedEmail);
	}

	private static UserAccountJpaEntity update(
			UserAccountJpaEntity entity,
			UserAccount account,
			Instant updatedAt) {
		entity.update(
				account.normalizedEmail(),
				account.passwordHash(),
				account.displayName(),
				account.defaultCurrency().getCurrencyCode(),
				updatedAt);
		return entity;
	}

	private static UserAccountJpaEntity toNewEntity(UserAccount account, Instant now) {
		return new UserAccountJpaEntity(
				account.id().value(),
				account.normalizedEmail(),
				account.passwordHash(),
				account.displayName(),
				account.defaultCurrency().getCurrencyCode(),
				now,
				now);
	}

	private static UserAccount toDomain(UserAccountJpaEntity entity) {
		return UserAccount.reconstruct(
				UserId.from(entity.getId()),
				entity.getEmail(),
				entity.getPasswordHash(),
				entity.getDisplayName(),
				Currency.getInstance(entity.getDefaultCurrency()));
	}
}
