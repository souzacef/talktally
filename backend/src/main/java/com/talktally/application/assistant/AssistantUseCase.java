package com.talktally.application.assistant;

import com.talktally.application.assistant.exception.InvalidAssistantInputException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;

import java.util.Objects;

public class AssistantUseCase {

	public static final int MAX_MESSAGE_LENGTH = 4_000;

	private final ChatAssistantPort chatAssistantPort;

	public AssistantUseCase(ChatAssistantPort chatAssistantPort) {
		this.chatAssistantPort = Objects.requireNonNull(
				chatAssistantPort, "chat assistant port must not be null");
	}

	public AssistantOutput execute(
			UserId actorId,
			TransactionSource source,
			String message) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(source, "source must not be null");
		if (message == null || message.isBlank()) {
			throw new InvalidAssistantInputException("message must not be blank");
		}
		String normalizedMessage = message.strip();
		if (normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
			throw new InvalidAssistantInputException(
					"message must not exceed " + MAX_MESSAGE_LENGTH + " characters");
		}
		return chatAssistantPort.respond(
				actorId, source, new AssistantInput(normalizedMessage));
	}
}
