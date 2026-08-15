package com.talktally.infrastructure.security;

import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.application.auth.port.IssuedAccessToken;
import com.talktally.domain.UserId;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final long accessTtlSeconds;
	private final Clock clock;

	@Autowired
	public JwtAccessTokenIssuer(
			JwtEncoder jwtEncoder,
			JwtSecurityProperties properties) {
		this(jwtEncoder, properties.accessTtlSeconds(), Clock.systemUTC());
	}

	JwtAccessTokenIssuer(JwtEncoder jwtEncoder, long accessTtlSeconds, Clock clock) {
		this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "JWT encoder must not be null");
		if (accessTtlSeconds < 1) {
			throw new IllegalArgumentException("access token TTL must be positive");
		}
		this.accessTtlSeconds = accessTtlSeconds;
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public IssuedAccessToken issue(UserId userId) {
		Objects.requireNonNull(userId, "user id must not be null");
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plusSeconds(accessTtlSeconds);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(JwtSecurityProperties.ISSUER)
				.subject(userId.value().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedAccessToken(token, issuedAt, expiresAt);
	}
}
