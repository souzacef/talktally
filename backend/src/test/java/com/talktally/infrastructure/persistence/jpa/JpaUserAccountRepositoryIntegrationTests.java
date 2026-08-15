package com.talktally.infrastructure.persistence.jpa;

import com.talktally.application.auth.exception.DuplicateEmailException;
import com.talktally.application.auth.port.PasswordHasher;
import com.talktally.domain.UserAccount;
import com.talktally.domain.UserAccountRepository;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaUserAccountRepositoryIntegrationTests {

	private static final String RAW_PASSWORD = "securepass123";

	@Autowired
	private UserAccountRepository repository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void savesAndReconstructsNormalizedAccountLosslessly() {
		String encodedPassword = passwordHasher.hash(RAW_PASSWORD);
		UserAccount account = UserAccount.create(
				UserId.generate(),
				"person@example.com",
				encodedPassword,
				"Person Name");

		UserAccount saved = repository.save(account);
		UserAccount byId = repository.findById(account.id()).orElseThrow();
		UserAccount byEmail = repository
				.findByNormalizedEmail("person@example.com")
				.orElseThrow();

		assertAll(
				() -> assertAccountEquals(account, saved),
				() -> assertAccountEquals(account, byId),
				() -> assertAccountEquals(account, byEmail),
				() -> assertEquals(Currency.getInstance("BRL"), byId.defaultCurrency()),
				() -> assertTrue(repository.existsByNormalizedEmail("person@example.com")),
				() -> assertFalse(repository.existsByNormalizedEmail("missing@example.com")),
				() -> assertEquals(encodedPassword, jdbcTemplate.queryForObject(
						"SELECT password_hash FROM app_user WHERE id = ?",
						String.class,
						account.id().value())),
				() -> assertNotEquals(RAW_PASSWORD, encodedPassword));
	}

	@Test
	void delegatingPasswordHasherUsesPrefixedAdaptiveHash() {
		String encoded = passwordHasher.hash(RAW_PASSWORD);

		assertAll(
				() -> assertTrue(encoded.startsWith("{bcrypt}")),
				() -> assertTrue(passwordHasher.matches(RAW_PASSWORD, encoded)),
				() -> assertFalse(passwordHasher.matches("wrongpass123", encoded)),
				() -> assertFalse(encoded.contains(RAW_PASSWORD)));
	}

	@Test
	void databaseUniquenessIsMappedToDuplicateEmailException() {
		String encoded = passwordHasher.hash(RAW_PASSWORD);
		repository.save(UserAccount.create(
				UserId.generate(), "person@example.com", encoded, "First"));

		assertThrows(
				DuplicateEmailException.class,
				() -> repository.save(UserAccount.create(
						UserId.generate(), "person@example.com", encoded, "Second")));
	}

	private static void assertAccountEquals(UserAccount expected, UserAccount actual) {
		assertEquals(expected.id(), actual.id());
		assertEquals(expected.normalizedEmail(), actual.normalizedEmail());
		assertEquals(expected.passwordHash(), actual.passwordHash());
		assertEquals(expected.displayName(), actual.displayName());
		assertEquals(expected.defaultCurrency(), actual.defaultCurrency());
	}
}
