package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.exception.InvalidTransactionInputException;
import com.talktally.application.person.exception.DuplicatePersonException;
import com.talktally.application.person.exception.InvalidPersonInputException;
import com.talktally.application.person.exception.PersonNotFoundException;
import com.talktally.application.reimbursement.exception.InvalidReimbursementInputException;
import com.talktally.application.reimbursement.exception.ReimbursementClaimNotFoundException;
import com.talktally.infrastructure.web.ApiError;
import com.talktally.infrastructure.web.person.PeopleController;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {ReimbursementController.class, PeopleController.class})
public class ReimbursementExceptionHandler {

	@ExceptionHandler({
			InvalidPersonInputException.class,
			InvalidReimbursementInputException.class,
			InvalidTransactionInputException.class
	})
	public ResponseEntity<ApiError> invalidInput(RuntimeException exception) {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REIMBURSEMENT_REQUEST", exception.getMessage()));
	}

	@ExceptionHandler({PersonNotFoundException.class, ReimbursementClaimNotFoundException.class})
	public ResponseEntity<ApiError> notFound(RuntimeException exception) {
		String code = exception instanceof PersonNotFoundException
				? "PERSON_NOT_FOUND"
				: "REIMBURSEMENT_NOT_FOUND";
		String message = exception instanceof PersonNotFoundException
				? "person not found"
				: "reimbursement claim not found";
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(code, message));
	}

	@ExceptionHandler({DuplicatePersonException.class, DataIntegrityViolationException.class})
	public ResponseEntity<ApiError> conflict() {
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(new ApiError("PERSON_ALREADY_EXISTS", "a person with that name already exists"));
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
