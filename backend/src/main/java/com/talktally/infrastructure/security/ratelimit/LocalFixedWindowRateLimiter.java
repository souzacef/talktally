package com.talktally.infrastructure.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-process v0.1 limiter. Restarting clears counters, and horizontal scaling
 * requires replacing this component with shared state.
 */
final class LocalFixedWindowRateLimiter {

	private static final int MAXIMUM_IDENTITY_LENGTH = 128;

	private final ApiRateLimitProperties properties;
	private final Clock clock;
	private final Cache<RateLimitKey, WindowState> windows;

	LocalFixedWindowRateLimiter(ApiRateLimitProperties properties) {
		this(properties, Clock.systemUTC(), Ticker.systemTicker());
	}

	LocalFixedWindowRateLimiter(
			ApiRateLimitProperties properties,
			Clock clock,
			Ticker ticker) {
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		Objects.requireNonNull(ticker, "ticker must not be null");
		this.windows = Caffeine.newBuilder()
				.maximumSize(properties.maximumEntries())
				.expireAfterAccess(properties.expireAfterAccess())
				.ticker(ticker)
				.build();
	}

	Decision acquire(ApiRateLimitBucket bucket, String identity) {
		Objects.requireNonNull(bucket, "bucket must not be null");
		if (identity == null || identity.isBlank() || identity.length() > MAXIMUM_IDENTITY_LENGTH) {
			throw new IllegalArgumentException("rate-limit identity is invalid");
		}
		ApiRateLimitProperties.Limit limit = properties.limit(bucket);
		Instant now = clock.instant();
		AtomicReference<Decision> decision = new AtomicReference<>();
		windows.asMap().compute(new RateLimitKey(bucket, identity), (key, current) -> {
			if (current == null
					|| now.isBefore(current.startedAt())
					|| !now.isBefore(current.startedAt().plus(limit.window()))) {
				decision.set(Decision.permitted());
				return new WindowState(now, 1);
			}
			if (current.used() < limit.capacity()) {
				decision.set(Decision.permitted());
				return new WindowState(current.startedAt(), current.used() + 1);
			}
			Duration remaining = Duration.between(
					now, current.startedAt().plus(limit.window()));
			long millis = Math.max(1, remaining.toMillis());
			long retryAfterSeconds = Math.max(1, (millis + 999) / 1_000);
			decision.set(Decision.rejected(retryAfterSeconds));
			return current;
		});
		return decision.get();
	}

	long estimatedEntryCount() {
		return windows.estimatedSize();
	}

	void cleanUp() {
		windows.cleanUp();
	}

	record Decision(boolean allowed, long retryAfterSeconds) {

		static Decision permitted() {
			return new Decision(true, 0);
		}

		static Decision rejected(long retryAfterSeconds) {
			return new Decision(false, retryAfterSeconds);
		}
	}

	private record RateLimitKey(ApiRateLimitBucket bucket, String identity) {
	}

	private record WindowState(Instant startedAt, int used) {
	}
}
