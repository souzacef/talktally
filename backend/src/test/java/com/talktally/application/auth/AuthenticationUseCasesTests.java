package com.talktally.application.auth;

import com.talktally.application.auth.exception.DuplicateEmailException;
import com.talktally.application.auth.exception.InvalidCredentialsException;
import com.talktally.application.auth.exception.InvalidRegistrationInputException;
import com.talktally.application.auth.input.AuthenticateUserInput;
import com.talktally.application.auth.input.RegisterUserInput;
import com.talktally.application.auth.output.AuthenticationOutput;
import com.talktally.application.auth.output.UserAccountOutput;
import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.application.auth.port.IssuedAccessToken;
import com.talktally.application.auth.port.PasswordHasher;
import com.talktally.domain.UserAccount;
import com.talktally.domain.UserAccountRepository;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationUseCasesTests {

	private static final String PASSWORD = "securepass123";
	private static final Instant ISSUED_AT = Instant.parse("2026-08-15T00:00:00Z");

	private InMemoryUserAccountRepository repository;
	private TestPasswordHasher passwordHasher;
	private RecordingTokenIssuer tokenIssuer;
	private RegisterUserUseCase registerUseCase;
	private AuthenticateUserUseCase authenticateUseCase;

	@BeforeEach
	void setUp() {
		repository = new InMemoryUserAccountRepository();
		passwordHasher = new TestPasswordHasher();
		tokenIssuer = new RecordingTokenIssuer();
		registerUseCase = new RegisterUserUseCase(repository, passwordHasher);
		authenticateUseCase = new AuthenticateUserUseCase(repository, passwordHasher, tokenIssuer);
	}

	@Test
	void validRegistrationGeneratesIdentityAndDefaultsToBrl() {
		UserAccountOutput output = register("person@example.com", PASSWORD, "Person");

		assertAll(
				() -> assertNotNull(output.userId()),
				() -> assertNotNull(output.userId().value()),
				() -> assertEquals("person@example.com", output.email()),
				() -> assertEquals("Person", output.displayName()),
				() -> assertEquals("BRL", output.defaultCurrency()),
				() -> assertTrue(repository.findById(output.userId()).isPresent()));
	}

	@Test
	void registrationNormalizesCaseAndSurroundingEmailWhitespace() {
		UserAccountOutput output = register("  PERSON@Example.COM  ", PASSWORD, "  Person Name  ");

		assertEquals("person@example.com", output.email());
		assertEquals("Person Name", output.displayName());
	}

	@Test
	void registrationHashesPasswordAndNeverPersistsRawValue() {
		UserAccountOutput output = register("person@example.com", PASSWORD, "Person");
		UserAccount persisted = repository.findById(output.userId()).orElseThrow();

		assertNotEquals(PASSWORD, persisted.passwordHash());
		assertTrue(persisted.passwordHash().startsWith("{test}"));
		assertFalse(outputComponentNames().contains("password"));
		assertFalse(outputComponentNames().contains("passwordHash"));
	}

	@Test
	void duplicateNormalizedEmailIsRejected() {
		register("Person@Example.com", PASSWORD, "First");

		assertThrows(
				DuplicateEmailException.class,
				() -> register(" person@example.COM ", "different456", "Second"));
	}

	@Test
	void shortPasswordIsRejected() {
		assertThrows(
				InvalidRegistrationInputException.class,
				() -> register("person@example.com", "short1", "Person"));
	}

	@Test
	void passwordWithoutDigitIsRejected() {
		assertThrows(
				InvalidRegistrationInputException.class,
				() -> register("person@example.com", "onlyletters", "Person"));
	}

	@Test
	void passwordWithoutLetterIsRejected() {
		assertThrows(
				InvalidRegistrationInputException.class,
				() -> register("person@example.com", "1234567890", "Person"));
	}

	@Test
	void passwordIsNotSilentlyTrimmed() {
		String passwordWithSpaces = " pass word 123 ";
		UserAccountOutput account = register(
				"person@example.com", passwordWithSpaces, "Person");

		assertTrue(authenticateUseCase.execute(new AuthenticateUserInput(
				"person@example.com", passwordWithSpaces)).accessToken().startsWith("token-for-"));
		assertThrows(
				InvalidCredentialsException.class,
				() -> authenticateUseCase.execute(new AuthenticateUserInput(
						account.email(), passwordWithSpaces.strip())));
	}

	@Test
	void blankDisplayNameIsRejected() {
		assertThrows(
				InvalidRegistrationInputException.class,
				() -> register("person@example.com", PASSWORD, "   "));
	}

	@Test
	void obviouslyMalformedEmailIsRejected() {
		assertAll(
				() -> assertThrows(
						InvalidRegistrationInputException.class,
						() -> register("not-an-email", PASSWORD, "Person")),
				() -> assertThrows(
						InvalidRegistrationInputException.class,
						() -> register("per son@example.com", PASSWORD, "Person")));
	}

	@Test
	void correctCredentialsReturnBearerTokenAndSafeIdentity() {
		UserAccountOutput registered = register("person@example.com", PASSWORD, "Person");

		AuthenticationOutput output = authenticateUseCase.execute(
				new AuthenticateUserInput("person@example.com", PASSWORD));

		assertAll(
				() -> assertEquals("Bearer", output.tokenType()),
				() -> assertEquals(3600, output.expiresIn()),
				() -> assertEquals(ISSUED_AT.plusSeconds(3600), output.expiresAt()),
				() -> assertEquals(registered, output.user()),
				() -> assertFalse(output.accessToken().contains(PASSWORD)),
				() -> assertFalse(output.toString().contains(output.accessToken())),
				() -> assertFalse(authenticationOutputComponentNames().contains("passwordHash")));
	}

	@Test
	void nonexistentEmailAndWrongPasswordReturnSameFailure() {
		register("person@example.com", PASSWORD, "Person");

		InvalidCredentialsException nonexistent = assertThrows(
				InvalidCredentialsException.class,
				() -> authenticateUseCase.execute(
						new AuthenticateUserInput("missing@example.com", PASSWORD)));
		InvalidCredentialsException wrongPassword = assertThrows(
				InvalidCredentialsException.class,
				() -> authenticateUseCase.execute(
						new AuthenticateUserInput("person@example.com", "incorrect123")));

		assertEquals(nonexistent.getClass(), wrongPassword.getClass());
		assertEquals(nonexistent.getMessage(), wrongPassword.getMessage());
	}

	@Test
	void authenticationAppliesEmailNormalization() {
		register("person@example.com", PASSWORD, "Person");

		AuthenticationOutput output = authenticateUseCase.execute(
				new AuthenticateUserInput("  PERSON@EXAMPLE.COM  ", PASSWORD));

		assertEquals("person@example.com", output.user().email());
	}

	@Test
	void tokenIsIssuedForStableUserId() {
		UserAccountOutput registered = register("person@example.com", PASSWORD, "Person");

		authenticateUseCase.execute(new AuthenticateUserInput("person@example.com", PASSWORD));

		assertEquals(registered.userId(), tokenIssuer.lastIssuedFor);
	}

	@Test
	void requestInputsRedactCredentialsFromStringRepresentation() {
		assertAll(
				() -> assertFalse(
						new RegisterUserInput("person@example.com", PASSWORD, "Person")
								.toString().contains(PASSWORD)),
				() -> assertFalse(
						new AuthenticateUserInput("person@example.com", PASSWORD)
								.toString().contains(PASSWORD)));
	}

	private UserAccountOutput register(String email, String password, String displayName) {
		return registerUseCase.execute(new RegisterUserInput(email, password, displayName));
	}

	private static java.util.Set<String> outputComponentNames() {
		return java.util.Arrays.stream(UserAccountOutput.class.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(java.util.stream.Collectors.toSet());
	}

	private static java.util.Set<String> authenticationOutputComponentNames() {
		return java.util.Arrays.stream(AuthenticationOutput.class.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(java.util.stream.Collectors.toSet());
	}

	private static final class TestPasswordHasher implements PasswordHasher {

		@Override
		public String hash(String rawPassword) {
			return "{test}" + Integer.toUnsignedString(rawPassword.hashCode(), 16);
		}

		@Override
		public boolean matches(String rawPassword, String encodedHash) {
			return hash(rawPassword).equals(encodedHash);
		}
	}

	private static final class RecordingTokenIssuer implements AccessTokenIssuer {

		private UserId lastIssuedFor;

		@Override
		public IssuedAccessToken issue(UserId userId) {
			lastIssuedFor = userId;
			return new IssuedAccessToken(
					"token-for-" + userId.value(), ISSUED_AT, ISSUED_AT.plusSeconds(3600));
		}
	}

	private static final class InMemoryUserAccountRepository implements UserAccountRepository {

		private final Map<UserId, UserAccount> byId = new HashMap<>();
		private final Map<String, UserAccount> byEmail = new HashMap<>();

		@Override
		public UserAccount save(UserAccount account) {
			UserAccount existing = byEmail.get(account.normalizedEmail());
			if (existing != null && !existing.id().equals(account.id())) {
				throw new DuplicateEmailException();
			}
			byId.put(account.id(), account);
			byEmail.put(account.normalizedEmail(), account);
			return account;
		}

		@Override
		public Optional<UserAccount> findById(UserId userId) {
			return Optional.ofNullable(byId.get(userId));
		}

		@Override
		public Optional<UserAccount> findByNormalizedEmail(String normalizedEmail) {
			return Optional.ofNullable(byEmail.get(normalizedEmail));
		}

		@Override
		public boolean existsByNormalizedEmail(String normalizedEmail) {
			return byEmail.containsKey(normalizedEmail);
		}
	}
}
