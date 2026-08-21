package com.talktally.infrastructure.security.ratelimit;

enum ApiRateLimitBucket {

	REGISTRATION,
	LOGIN,
	ASSISTANT_TEXT,
	ASSISTANT_VOICE;

	static ApiRateLimitBucket fromPath(String path) {
		if (path == null) {
			return null;
		}
		return switch (path) {
			case "/api/v1/auth/registrations" -> REGISTRATION;
			case "/api/v1/auth/sessions" -> LOGIN;
			case "/api/v1/assistant/messages" -> ASSISTANT_TEXT;
			case "/api/v1/assistant/voice" -> ASSISTANT_VOICE;
			default -> null;
		};
	}

	boolean usesAuthenticatedUser() {
		return this == ASSISTANT_TEXT || this == ASSISTANT_VOICE;
	}
}
