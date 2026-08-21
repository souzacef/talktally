package com.talktally.infrastructure.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRateLimitPropertiesTests {

	@Test
	void productionDefaultsAreEnabledAndMatchThePublishedPolicy() throws Exception {
		MutablePropertySources sources = new MutablePropertySources();
		new YamlPropertySourceLoader()
				.load("application", new ClassPathResource("application.yml"))
				.forEach(sources::addLast);
		ApiRateLimitProperties properties = new Binder(ConfigurationPropertySources.from(sources))
				.bind(
						"talktally.security.rate-limit",
						Bindable.of(ApiRateLimitProperties.class))
				.orElseThrow(() -> new AssertionError("rate-limit defaults must be configured"));

		assertTrue(properties.enabled());
		assertEquals(10_000, properties.maximumEntries());
		assertEquals(Duration.ofHours(2), properties.expireAfterAccess());
		assertEquals(new ApiRateLimitProperties.Limit(3, Duration.ofHours(1)),
				properties.registration());
		assertEquals(new ApiRateLimitProperties.Limit(10, Duration.ofMinutes(1)),
				properties.login());
		assertEquals(new ApiRateLimitProperties.Limit(20, Duration.ofMinutes(1)),
				properties.assistantText());
		assertEquals(new ApiRateLimitProperties.Limit(6, Duration.ofMinutes(1)),
				properties.assistantVoice());
	}
}
