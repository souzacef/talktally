package com.talktally.infrastructure.web.ratelimit;

import com.talktally.infrastructure.security.ratelimit.RateLimitExceededException;
import com.talktally.infrastructure.web.ApiError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RateLimitExceptionHandler {

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<ApiError> rateLimited(RateLimitExceededException exception) {
		return ResponseEntity
				.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(
						HttpHeaders.RETRY_AFTER,
						Long.toString(exception.retryAfterSeconds()))
				.body(new ApiError("RATE_LIMITED", "too many requests"));
	}
}
