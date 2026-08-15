package com.talktally.application.assistant;

import java.util.Objects;

public record AssistantOutput(String message, AssistantStatus status) {

	public AssistantOutput {
		Objects.requireNonNull(message, "message must not be null");
		Objects.requireNonNull(status, "status must not be null");
	}
}
