package com.talktally.infrastructure.security;

import com.talktally.application.auth.port.PasswordHasher;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SpringSecurityPasswordHasher implements PasswordHasher {

	private final PasswordEncoder passwordEncoder;

	public SpringSecurityPasswordHasher() {
		this(PasswordEncoderFactories.createDelegatingPasswordEncoder());
	}

	SpringSecurityPasswordHasher(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = Objects.requireNonNull(
				passwordEncoder, "password encoder must not be null");
	}

	@Override
	public String hash(String rawPassword) {
		return passwordEncoder.encode(Objects.requireNonNull(
				rawPassword, "raw password must not be null"));
	}

	@Override
	public boolean matches(String rawPassword, String encodedHash) {
		return passwordEncoder.matches(
				Objects.requireNonNull(rawPassword, "raw password must not be null"),
				Objects.requireNonNull(encodedHash, "encoded hash must not be null"));
	}
}
