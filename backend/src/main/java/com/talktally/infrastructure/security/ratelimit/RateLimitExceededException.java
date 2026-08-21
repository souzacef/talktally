package com.talktally.infrastructure.security.ratelimit;

public class RateLimitExceededException extends RuntimeException {

	private final long retryAfterSeconds;

	RateLimitExceededException(long retryAfterSeconds) {
		super("too many requests");
		if (retryAfterSeconds <= 0) {
			throw new IllegalArgumentException("retry-after must be positive");
		}
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}
}
