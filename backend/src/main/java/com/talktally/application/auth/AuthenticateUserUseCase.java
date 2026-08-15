package com.talktally.application.auth;

import com.talktally.application.auth.exception.InvalidCredentialsException;
import com.talktally.application.auth.exception.InvalidRegistrationInputException;
import com.talktally.application.auth.input.AuthenticateUserInput;
import com.talktally.application.auth.output.AuthenticationOutput;
import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.application.auth.port.IssuedAccessToken;
import com.talktally.application.auth.port.PasswordHasher;
import com.talktally.domain.UserAccount;
import com.talktally.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;

@Service
public class AuthenticateUserUseCase {

	private final UserAccountRepository userAccountRepository;
	private final PasswordHasher passwordHasher;
	private final AccessTokenIssuer accessTokenIssuer;

	public AuthenticateUserUseCase(
			UserAccountRepository userAccountRepository,
			PasswordHasher passwordHasher,
			AccessTokenIssuer accessTokenIssuer) {
		this.userAccountRepository = Objects.requireNonNull(
				userAccountRepository, "user account repository must not be null");
		this.passwordHasher = Objects.requireNonNull(
				passwordHasher, "password hasher must not be null");
		this.accessTokenIssuer = Objects.requireNonNull(
				accessTokenIssuer, "access token issuer must not be null");
	}

	@Transactional(readOnly = true)
	public AuthenticationOutput execute(AuthenticateUserInput input) {
		if (input == null || input.password() == null) {
			throw new InvalidCredentialsException();
		}

		String normalizedEmail;
		try {
			normalizedEmail = EmailPolicy.normalize(input.email());
		}
		catch (InvalidRegistrationInputException exception) {
			throw new InvalidCredentialsException();
		}

		UserAccount account = userAccountRepository.findByNormalizedEmail(normalizedEmail)
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordHasher.matches(input.password(), account.passwordHash())) {
			throw new InvalidCredentialsException();
		}

		IssuedAccessToken token = accessTokenIssuer.issue(account.id());
		long expiresIn = Duration.between(token.issuedAt(), token.expiresAt()).toSeconds();
		return new AuthenticationOutput(
				token.value(),
				"Bearer",
				expiresIn,
				token.expiresAt(),
				UserAccountOutputMapper.toOutput(account));
	}
}
