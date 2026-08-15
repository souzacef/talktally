package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.application.assistant.exception.InvalidAssistantInputException;
import com.talktally.infrastructure.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AssistantController.class)
public class AssistantExceptionHandler {

	@ExceptionHandler(InvalidAssistantInputException.class)
	public ResponseEntity<ApiError> invalidInput(InvalidAssistantInputException exception) {
		return ResponseEntity.badRequest()
				.body(new ApiError("INVALID_ASSISTANT_INPUT", exception.getMessage()));
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class })
	public ResponseEntity<ApiError> invalidRequest() {
		return ResponseEntity.badRequest()
				.body(new ApiError("INVALID_REQUEST", "request body is invalid"));
	}

	@ExceptionHandler(AssistantUnavailableException.class)
	public ResponseEntity<ApiError> unavailable() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError("ASSISTANT_UNAVAILABLE", "assistant is temporarily unavailable"));
	}
}
