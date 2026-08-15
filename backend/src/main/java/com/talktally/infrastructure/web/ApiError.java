package com.talktally.infrastructure.web;

import java.util.Objects;

public record ApiError(String code, String message) {

	public ApiError {
		Objects.requireNonNull(code, "error code must not be null");
		Objects.requireNonNull(message, "error message must not be null");
	}
}
