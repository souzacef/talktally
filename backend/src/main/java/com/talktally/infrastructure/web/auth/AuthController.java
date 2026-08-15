package com.talktally.infrastructure.web.auth;

import com.talktally.application.auth.AuthenticateUserUseCase;
import com.talktally.application.auth.RegisterUserUseCase;
import com.talktally.application.auth.input.AuthenticateUserInput;
import com.talktally.application.auth.input.RegisterUserInput;
import com.talktally.application.auth.output.AuthenticationOutput;
import com.talktally.application.auth.output.UserAccountOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final RegisterUserUseCase registerUserUseCase;
	private final AuthenticateUserUseCase authenticateUserUseCase;

	public AuthController(
			RegisterUserUseCase registerUserUseCase,
			AuthenticateUserUseCase authenticateUserUseCase) {
		this.registerUserUseCase = Objects.requireNonNull(
				registerUserUseCase, "register user use case must not be null");
		this.authenticateUserUseCase = Objects.requireNonNull(
				authenticateUserUseCase, "authenticate user use case must not be null");
	}

	@PostMapping("/registrations")
	public ResponseEntity<UserAccountResponse> register(@RequestBody RegistrationRequest request) {
		UserAccountOutput output = registerUserUseCase.execute(new RegisterUserInput(
				request.email(), request.password(), request.displayName()));
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(UserAccountResponse.from(output));
	}

	@PostMapping("/sessions")
	public AuthenticationResponse authenticate(@RequestBody AuthenticationRequest request) {
		AuthenticationOutput output = authenticateUserUseCase.execute(new AuthenticateUserInput(
				request.email(), request.password()));
		return AuthenticationResponse.from(output);
	}
}
