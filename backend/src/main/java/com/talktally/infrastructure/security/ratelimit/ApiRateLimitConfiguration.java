package com.talktally.infrastructure.security.ratelimit;

import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiRateLimitProperties.class)
public class ApiRateLimitConfiguration {

	@Bean
	LocalFixedWindowRateLimiter localFixedWindowRateLimiter(
			ApiRateLimitProperties properties) {
		return new LocalFixedWindowRateLimiter(properties);
	}

	@Bean
	ClientIpAddressResolver clientIpAddressResolver() {
		return new ClientIpAddressResolver();
	}

	@Bean
	ApiRateLimitInterceptor apiRateLimitInterceptor(
			ApiRateLimitProperties properties,
			LocalFixedWindowRateLimiter rateLimiter,
			ClientIpAddressResolver clientIpAddressResolver,
			AuthenticatedUserProvider authenticatedUserProvider) {
		return new ApiRateLimitInterceptor(
				properties,
				rateLimiter,
				clientIpAddressResolver,
				authenticatedUserProvider);
	}

	@Bean
	WebMvcConfigurer apiRateLimitWebMvcConfigurer(ApiRateLimitInterceptor interceptor) {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(interceptor)
						.addPathPatterns(
								"/api/v1/auth/registrations",
								"/api/v1/auth/sessions",
								"/api/v1/assistant/messages",
								"/api/v1/assistant/voice");
			}
		};
	}
}
