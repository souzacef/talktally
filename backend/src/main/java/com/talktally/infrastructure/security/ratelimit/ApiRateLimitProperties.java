package com.talktally.infrastructure.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "talktally.security.rate-limit")
public record ApiRateLimitProperties(
		boolean enabled,
		long maximumEntries,
		Duration expireAfterAccess,
		Limit registration,
		Limit login,
		Limit assistantText,
		Limit assistantVoice) {

	private static final long MAXIMUM_CONFIGURED_ENTRIES = 100_000;
	private static final Duration MAXIMUM_WINDOW = Duration.ofDays(1);

	public ApiRateLimitProperties {
		if (maximumEntries <= 0 || maximumEntries > MAXIMUM_CONFIGURED_ENTRIES) {
			throw new IllegalArgumentException(
					"rate-limit maximum entries must be between 1 and 100000");
		}
		Objects.requireNonNull(expireAfterAccess, "rate-limit expiry must not be null");
		List<Limit> limits = List.of(
				Objects.requireNonNull(registration, "registration limit must not be null"),
				Objects.requireNonNull(login, "login limit must not be null"),
				Objects.requireNonNull(assistantText, "assistant text limit must not be null"),
				Objects.requireNonNull(assistantVoice, "assistant voice limit must not be null"));
		Duration longestWindow = limits.stream()
				.map(Limit::window)
				.max(Duration::compareTo)
				.orElseThrow();
		if (expireAfterAccess.compareTo(longestWindow) < 0) {
			throw new IllegalArgumentException(
					"rate-limit expiry must not be shorter than a configured window");
		}
	}

	Limit limit(ApiRateLimitBucket bucket) {
		return switch (bucket) {
			case REGISTRATION -> registration;
			case LOGIN -> login;
			case ASSISTANT_TEXT -> assistantText;
			case ASSISTANT_VOICE -> assistantVoice;
		};
	}

	public record Limit(int capacity, Duration window) {

		public Limit {
			if (capacity <= 0) {
				throw new IllegalArgumentException("rate-limit capacity must be positive");
			}
			Objects.requireNonNull(window, "rate-limit window must not be null");
			if (window.isZero() || window.isNegative() || window.compareTo(MAXIMUM_WINDOW) > 0) {
				throw new IllegalArgumentException(
						"rate-limit window must be positive and no longer than one day");
			}
		}
	}
}
