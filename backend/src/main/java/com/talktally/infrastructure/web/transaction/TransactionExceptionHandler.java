package com.talktally.infrastructure.web.transaction;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.exception.TransactionNotFoundException;
import com.talktally.infrastructure.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = TransactionController.class)
public class TransactionExceptionHandler {

	@ExceptionHandler(InvalidTransactionInputException.class)
	public ResponseEntity<ApiError> invalidTransaction(InvalidTransactionInputException exception) {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_TRANSACTION", exception.getMessage()));
	}

	@ExceptionHandler(TransactionNotFoundException.class)
	public ResponseEntity<ApiError> transactionNotFound(TransactionNotFoundException exception) {
		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(new ApiError("TRANSACTION_NOT_FOUND", "transaction not found"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validationFailure() {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REQUEST", "request validation failed"));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> unreadableRequest() {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REQUEST", "request body is invalid"));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> invalidParameter() {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REQUEST", "path or query parameter is invalid"));
	}
}
