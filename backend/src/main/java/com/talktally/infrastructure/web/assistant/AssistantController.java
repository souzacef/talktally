package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.AssistantConversationUseCase;
import com.talktally.application.assistant.AssistantUseCase;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/assistant/messages")
public class AssistantController {

	private final AssistantUseCase assistantUseCase;
	private final AssistantConversationUseCase conversationUseCase;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public AssistantController(
			AssistantUseCase assistantUseCase,
			AssistantConversationUseCase conversationUseCase,
			AuthenticatedUserProvider authenticatedUserProvider) {
		this.assistantUseCase = Objects.requireNonNull(
				assistantUseCase, "assistant use case must not be null");
		this.conversationUseCase = Objects.requireNonNull(
				conversationUseCase, "assistant conversation use case must not be null");
		this.authenticatedUserProvider = Objects.requireNonNull(
				authenticatedUserProvider, "authenticated user provider must not be null");
	}

	@GetMapping
	public List<AssistantConversationMessageResponse> history() {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return conversationUseCase.history(actorId).stream()
				.map(AssistantConversationMessageResponse::from)
				.toList();
	}

	@PostMapping
	public AssistantMessageResponse send(@Valid @RequestBody AssistantMessageRequest request) {
		UserId actorId = authenticatedUserProvider.currentUserId();
		return AssistantMessageResponse.from(assistantUseCase.execute(
				actorId,
				TransactionSource.ASSISTANT_TEXT,
				request.message()));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void clear() {
		conversationUseCase.clear(authenticatedUserProvider.currentUserId());
	}
}
