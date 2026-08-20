package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.AssistantConversationMessage;
import com.talktally.application.assistant.AssistantConversationRole;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.domain.TransactionSource;

import java.time.Instant;

public record AssistantConversationMessageResponse(
		long id,
		AssistantConversationRole role,
		String content,
		TransactionSource source,
		AssistantStatus status,
		Instant createdAt) {

	static AssistantConversationMessageResponse from(AssistantConversationMessage message) {
		return new AssistantConversationMessageResponse(
				message.sequenceId(),
				message.role(),
				message.content(),
				message.source(),
				message.status(),
				message.createdAt());
	}
}
