package com.talktally.application.assistant;

import com.talktally.application.assistant.exception.InvalidAssistantInputException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;

import java.util.List;
import java.util.Objects;

public class AssistantUseCase {

	public static final int MAX_MESSAGE_LENGTH = 4_000;
	public static final int MODEL_HISTORY_LIMIT = 20;

	private final ChatAssistantPort chatAssistantPort;
	private final AssistantConversationPort conversationPort;

	public AssistantUseCase(
			ChatAssistantPort chatAssistantPort,
			AssistantConversationPort conversationPort) {
		this.chatAssistantPort = Objects.requireNonNull(
				chatAssistantPort, "chat assistant port must not be null");
		this.conversationPort = Objects.requireNonNull(
				conversationPort, "assistant conversation port must not be null");
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
		List<AssistantConversationMessage> history = conversationPort.findRecent(
				actorId, MODEL_HISTORY_LIMIT);
		AssistantOutput output = chatAssistantPort.respond(
				actorId,
				source,
				history,
				new AssistantInput(normalizedMessage));
		conversationPort.appendExchange(actorId, source, normalizedMessage, output);
		return output;
	}
}
