package com.talktally.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "talktally.web.cors")
public record ApiCorsProperties(List<String> allowedOrigins) {

	public ApiCorsProperties {
		allowedOrigins = allowedOrigins == null
				? List.of()
				: allowedOrigins.stream()
						.map(String::strip)
						.filter(origin -> !origin.isEmpty())
						.peek(ApiCorsProperties::validateExactOrigin)
						.distinct()
						.toList();
	}

	private static void validateExactOrigin(String origin) {
		if (origin.contains("*")) {
			throw new IllegalArgumentException("CORS origins must not contain wildcards");
		}
		URI uri;
		try {
			uri = URI.create(origin);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("CORS origin must be a valid URI", exception);
		}
		boolean supportedScheme = "http".equalsIgnoreCase(uri.getScheme())
				|| "https".equalsIgnoreCase(uri.getScheme());
		if (!supportedScheme
				|| uri.getHost() == null
				|| uri.getRawUserInfo() != null
				|| uri.getRawPath() != null && !uri.getRawPath().isEmpty()
				|| uri.getRawQuery() != null
				|| uri.getRawFragment() != null) {
			throw new IllegalArgumentException(
					"CORS origins must be exact HTTP(S) origins without paths");
		}
	}
}
