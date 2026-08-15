package com.talktally.infrastructure.web.dashboard;

import com.talktally.application.reporting.InvalidReportingInputException;
import com.talktally.infrastructure.web.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardExceptionHandler {

	@ExceptionHandler(InvalidReportingInputException.class)
	public ResponseEntity<ApiError> invalidReportingInput(
			InvalidReportingInputException exception) {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REPORTING_REQUEST", exception.getMessage()));
	}

	@ExceptionHandler({
			MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class
	})
	public ResponseEntity<ApiError> invalidParameter() {
		return ResponseEntity
				.badRequest()
				.body(new ApiError("INVALID_REQUEST", "reporting parameters are invalid"));
	}
}
