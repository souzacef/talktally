package com.talktally.infrastructure.ai;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record ToolResult(ToolResultStatus status, String message, @Nullable Object data) {

	public ToolResult {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(message, "message must not be null");
	}

	public static ToolResult success(String message, Object data) {
		return new ToolResult(ToolResultStatus.SUCCESS, message, data);
	}

	public static ToolResult clarification(String message) {
		return new ToolResult(ToolResultStatus.NEEDS_CLARIFICATION, message, null);
	}

	public static ToolResult clarification(String message, Object data) {
		return new ToolResult(ToolResultStatus.NEEDS_CLARIFICATION, message, data);
	}

	public static ToolResult notFound(String message) {
		return new ToolResult(ToolResultStatus.NOT_FOUND, message, null);
	}

	public static ToolResult rejected(String message) {
		return new ToolResult(ToolResultStatus.REJECTED, message, null);
	}
}
