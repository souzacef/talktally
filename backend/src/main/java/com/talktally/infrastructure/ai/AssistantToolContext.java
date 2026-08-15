package com.talktally.infrastructure.ai;

import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AssistantToolContext {

	public static final String USER_ID = "talktally.userId";
	public static final String TRANSACTION_SOURCE = "talktally.transactionSource";

	private AssistantToolContext() {
	}

	public static Map<String, Object> create(UserId actorId, TransactionSource source) {
		Objects.requireNonNull(actorId, "actor id must not be null");
		Objects.requireNonNull(source, "source must not be null");
		return Map.of(
				USER_ID, actorId.value().toString(),
				TRANSACTION_SOURCE, source.name());
	}

	public static UserId requireActor(ToolContext toolContext) {
		Object rawValue = requireContext(toolContext).get(USER_ID);
		if (!(rawValue instanceof String value)) {
			throw invalidContext();
		}
		try {
			return UserId.from(UUID.fromString(value));
		}
		catch (IllegalArgumentException exception) {
			throw invalidContext();
		}
	}

	public static TransactionSource requireSource(ToolContext toolContext) {
		Object rawValue = requireContext(toolContext).get(TRANSACTION_SOURCE);
		if (!(rawValue instanceof String value)) {
			throw invalidContext();
		}
		try {
			return TransactionSource.valueOf(value);
		}
		catch (IllegalArgumentException exception) {
			throw invalidContext();
		}
	}

	private static Map<String, Object> requireContext(ToolContext toolContext) {
		if (toolContext == null) {
			throw invalidContext();
		}
		return toolContext.getContext();
	}

	private static IllegalStateException invalidContext() {
		return new IllegalStateException("assistant tool context is missing or invalid");
	}
}
