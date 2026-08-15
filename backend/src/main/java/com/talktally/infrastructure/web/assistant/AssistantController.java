package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.AssistantUseCase;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/assistant/messages")
public class AssistantController {

	private final AssistantUseCase assistantUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public AssistantController(
			AssistantUseCase assistantUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.assistantUseCase = Objects.requireNonNull(
				assistantUseCase, "assistant use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@PostMapping
	public AssistantMessageResponse send(@Valid @RequestBody AssistantMessageRequest request) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return AssistantMessageResponse.from(assistantUseCase.execute(
				actorId,
				TransactionSource.ASSISTANT_TEXT,
				request.message()));
	}
}
