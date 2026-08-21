package com.talktally.infrastructure.security.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFixedWindowRateLimiterTests {

	private static final Instant START = Instant.parse("2026-08-21T12:00:00Z");

	@Test
	void fixedWindowAndRetryAfterAdvanceDeterministically() {
		MutableClock clock = new MutableClock(START);
		MutableTicker ticker = new MutableTicker();
		LocalFixedWindowRateLimiter limiter = limiter(2, Duration.ofMinutes(1), 100, clock, ticker);

		assertTrue(limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.1").allowed());
		assertTrue(limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.1").allowed());
		LocalFixedWindowRateLimiter.Decision rejected =
				limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.1");
		assertFalse(rejected.allowed());
		assertEquals(60, rejected.retryAfterSeconds());

		clock.advance(Duration.ofSeconds(59).plusMillis(100));
		assertEquals(
				1,
				limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.1")
						.retryAfterSeconds());

		clock.advance(Duration.ofMillis(900));
		assertTrue(limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.1").allowed());
	}

	@Test
	void identitiesAndEndpointBucketsRemainIndependent() {
		MutableClock clock = new MutableClock(START);
		LocalFixedWindowRateLimiter limiter = limiter(
				1, Duration.ofMinutes(1), 100, clock, new MutableTicker());

		assertTrue(limiter.acquire(ApiRateLimitBucket.ASSISTANT_TEXT, "user-a").allowed());
		assertFalse(limiter.acquire(ApiRateLimitBucket.ASSISTANT_TEXT, "user-a").allowed());
		assertTrue(limiter.acquire(ApiRateLimitBucket.ASSISTANT_TEXT, "user-b").allowed());
		assertTrue(limiter.acquire(ApiRateLimitBucket.ASSISTANT_VOICE, "user-a").allowed());
	}

	@Test
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	void concurrentAcquisitionsCannotExceedCapacity() throws Exception {
		int capacity = 10;
		int attempts = 100;
		LocalFixedWindowRateLimiter limiter = limiter(
				capacity,
				Duration.ofMinutes(1),
				100,
				new MutableClock(START),
				new MutableTicker());
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		var executor = Executors.newVirtualThreadPerTaskExecutor();
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int index = 0; index < attempts; index++) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("concurrent limiter start was not released");
					}
					return limiter.acquire(
							ApiRateLimitBucket.ASSISTANT_TEXT, "same-user").allowed();
				}));
			}
			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			int allowed = 0;
			for (Future<Boolean> future : futures) {
				if (future.get(5, TimeUnit.SECONDS)) {
					allowed++;
				}
			}
			assertEquals(capacity, allowed);
		}
		finally {
			start.countDown();
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
		}
	}

	@Test
	void attackerControlledStateIsBoundedAndStaleEntriesExpire() {
		MutableClock clock = new MutableClock(START);
		MutableTicker ticker = new MutableTicker();
		LocalFixedWindowRateLimiter limiter = limiter(
				1, Duration.ofMinutes(1), 2, clock, ticker);

		limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.1");
		limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.2");
		limiter.acquire(ApiRateLimitBucket.LOGIN, "192.0.2.3");
		limiter.cleanUp();
		assertTrue(limiter.estimatedEntryCount() <= 2);

		ticker.advance(Duration.ofHours(2).plusSeconds(1));
		limiter.cleanUp();
		assertEquals(0, limiter.estimatedEntryCount());
	}

	private static LocalFixedWindowRateLimiter limiter(
			int capacity,
			Duration window,
			long maximumEntries,
			Clock clock,
			Ticker ticker) {
		ApiRateLimitProperties.Limit limit =
				new ApiRateLimitProperties.Limit(capacity, window);
		return new LocalFixedWindowRateLimiter(
				new ApiRateLimitProperties(
						true,
						maximumEntries,
						Duration.ofHours(2),
						limit,
						limit,
						limit,
						limit),
				clock,
				ticker);
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}

	private static final class MutableTicker implements Ticker {

		private final AtomicLong nanos = new AtomicLong();

		void advance(Duration duration) {
			nanos.addAndGet(duration.toNanos());
		}

		@Override
		public long read() {
			return nanos.get();
		}
	}
}
