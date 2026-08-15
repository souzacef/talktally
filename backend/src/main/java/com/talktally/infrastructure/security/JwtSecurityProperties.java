package com.talktally.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@ConfigurationProperties("talktally.security.jwt")
public final class JwtSecurityProperties {

	public static final String ISSUER = "talktally-api";
	private static final int MINIMUM_SECRET_BYTES = 32;

	private final SecretKey secretKey;
	private final long accessTtlSeconds;

	public JwtSecurityProperties(String secretBase64, long accessTtlSeconds) {
		if (secretBase64 == null || secretBase64.isBlank()) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must be configured");
		}

		byte[] decodedSecret;
		try {
			decodedSecret = Base64.getDecoder().decode(secretBase64);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must be valid Base64", exception);
		}
		if (decodedSecret.length < MINIMUM_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT_SECRET_BASE64 must decode to at least 32 bytes");
		}
		if (accessTtlSeconds < 1) {
			throw new IllegalStateException("JWT_ACCESS_TTL_SECONDS must be positive");
		}

		this.secretKey = new SecretKeySpec(decodedSecret, "HmacSHA256");
		this.accessTtlSeconds = accessTtlSeconds;
	}

	SecretKey secretKey() {
		return secretKey;
	}

	public long accessTtlSeconds() {
		return accessTtlSeconds;
	}
}
