package com.talktally.application.assistant;

import com.talktally.domain.TransactionSource;

import java.time.Instant;
import java.util.Objects;

public record AssistantConversationMessage(
		long sequenceId,
		AssistantConversationRole role,
		String content,
		TransactionSource source,
		AssistantStatus status,
		Instant createdAt) {

	public AssistantConversationMessage {
		if (sequenceId <= 0) {
			throw new IllegalArgumentException("sequence id must be positive");
		}
		Objects.requireNonNull(role, "role must not be null");
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content must not be blank");
		}
		Objects.requireNonNull(createdAt, "created at must not be null");
		if (role == AssistantConversationRole.USER && source == null) {
			throw new IllegalArgumentException("user message source must not be null");
		}
		if (role == AssistantConversationRole.ASSISTANT && status == null) {
			throw new IllegalArgumentException("assistant message status must not be null");
		}
	}
}
