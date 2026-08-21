package com.talktally.infrastructure.security.ratelimit;

import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Objects;

final class ApiRateLimitInterceptor implements HandlerInterceptor {

	private final ApiRateLimitProperties properties;
	private final LocalFixedWindowRateLimiter rateLimiter;
	private final ClientIpAddressResolver clientIpAddressResolver;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	ApiRateLimitInterceptor(
			ApiRateLimitProperties properties,
			LocalFixedWindowRateLimiter rateLimiter,
			ClientIpAddressResolver clientIpAddressResolver,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.rateLimiter = Objects.requireNonNull(rateLimiter, "rate limiter must not be null");
		this.clientIpAddressResolver = Objects.requireNonNull(
				clientIpAddressResolver, "client IP resolver must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@Override
	public boolean preHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler) {
		if (!properties.enabled() || !"POST".equals(request.getMethod())) {
			return true;
		}
		Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		String path = bestPattern == null ? pathWithinApplication(request) : bestPattern.toString();
		ApiRateLimitBucket bucket = ApiRateLimitBucket.fromPath(path);
		if (bucket == null) {
			return true;
		}
		String identity = bucket.usesAuthenticatedUser()
				? authenticatedUserProvider.currentUserId().value().toString()
				: clientIpAddressResolver.resolve(request);
		LocalFixedWindowRateLimiter.Decision decision = rateLimiter.acquire(bucket, identity);
		if (!decision.allowed()) {
			throw new RateLimitExceededException(decision.retryAfterSeconds());
		}
		return true;
	}

	private static String pathWithinApplication(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null
				&& !contextPath.isEmpty()
				&& requestUri.startsWith(contextPath)) {
			return requestUri.substring(contextPath.length());
		}
		return requestUri;
	}
}
