package com.talktally.infrastructure.web.auth;

import com.talktally.application.auth.exception.DuplicateEmailException;
import com.talktally.application.auth.exception.InvalidCredentialsException;
import com.talktally.application.auth.exception.InvalidRegistrationInputException;
import com.talktally.infrastructure.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

	@ExceptionHandler(InvalidRegistrationInputException.class)
	public ResponseEntity<ApiError> invalidRegistration(InvalidRegistrationInputException exception) {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REGISTRATION", exception.getMessage()));
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ApiError> duplicateEmail(DuplicateEmailException exception) {
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(new ApiError("EMAIL_ALREADY_REGISTERED", exception.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException exception) {
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiError("INVALID_CREDENTIALS", exception.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> unreadableRequest() {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REQUEST", "request body is invalid"));
	}
}
