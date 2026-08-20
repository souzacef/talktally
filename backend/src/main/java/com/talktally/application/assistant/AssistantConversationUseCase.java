package com.talktally.application.assistant;

import com.talktally.domain.UserId;

import java.util.List;
import java.util.Objects;

public class AssistantConversationUseCase {

	public static final int VISIBLE_HISTORY_LIMIT = 100;

	private final AssistantConversationPort conversationPort;

	public AssistantConversationUseCase(AssistantConversationPort conversationPort) {
		this.conversationPort = Objects.requireNonNull(
				conversationPort, "assistant conversation port must not be null");
	}

	public List<AssistantConversationMessage> history(UserId actorId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		return conversationPort.findRecent(actorId, VISIBLE_HISTORY_LIMIT);
	}

	public void clear(UserId actorId) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		conversationPort.clear(actorId);
	}
}
