package com.talktally.application.auth;

import com.talktally.application.auth.exception.DuplicateEmailException;
import com.talktally.application.auth.exception.InvalidRegistrationInputException;
import com.talktally.application.auth.input.RegisterUserInput;
import com.talktally.application.auth.output.UserAccountOutput;
import com.talktally.application.auth.port.PasswordHasher;
import com.talktally.domain.UserAccount;
import com.talktally.domain.UserAccountRepository;
import com.talktally.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class RegisterUserUseCase {

	private final UserAccountRepository userAccountRepository;
	private final PasswordHasher passwordHasher;

	public RegisterUserUseCase(
			UserAccountRepository userAccountRepository,
			PasswordHasher passwordHasher) {
		this.userAccountRepository = Objects.requireNonNull(
				userAccountRepository, "user account repository must not be null");
		this.passwordHasher = Objects.requireNonNull(
				passwordHasher, "password hasher must not be null");
	}

	@Transactional
	public UserAccountOutput execute(RegisterUserInput input) {
		if (input == null) {
			throw new InvalidRegistrationInputException("registration input is required");
		}

		String normalizedEmail = EmailPolicy.normalize(input.email());
		PasswordPolicy.validate(input.password());
		String displayName = normalizeDisplayName(input.displayName());
		if (userAccountRepository.existsByNormalizedEmail(normalizedEmail)) {
			throw new DuplicateEmailException();
		}

		String passwordHash = passwordHasher.hash(input.password());
		if (passwordHash == null
				|| passwordHash.isBlank()
				|| passwordHash.equals(input.password())) {
			throw new IllegalStateException("password hasher returned an unsafe value");
		}
		UserAccount account = UserAccount.create(
				UserId.generate(),
				normalizedEmail,
				passwordHash,
				displayName);

		return UserAccountOutputMapper.toOutput(userAccountRepository.save(account));
	}

	private static String normalizeDisplayName(String displayName) {
		if (displayName == null) {
			throw new InvalidRegistrationInputException("display name is required");
		}
		String normalized = displayName.strip();
		if (normalized.isEmpty() || normalized.length() > 120) {
			throw new InvalidRegistrationInputException(
					"display name must contain 1 to 120 characters");
		}
		return normalized;
	}
}
