package com.talktally.infrastructure.ai;

import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.application.assistant.exception.AssistantUnavailableException;

final class AssistantResponseParser {

	private static final String COMPLETED_MARKER = "[COMPLETED]";
	private static final String CLARIFICATION_MARKER = "[NEEDS_CLARIFICATION]";

	private AssistantResponseParser() {
	}

	static AssistantOutput parse(String providerContent) {
		if (providerContent == null || providerContent.isBlank()) {
			throw new AssistantUnavailableException();
		}
		String response = providerContent.strip();
		if (response.startsWith(COMPLETED_MARKER)) {
			return output(response, COMPLETED_MARKER, AssistantStatus.COMPLETED);
		}
		if (response.startsWith(CLARIFICATION_MARKER)) {
			return output(
					response,
					CLARIFICATION_MARKER,
					AssistantStatus.NEEDS_CLARIFICATION);
		}
		throw new AssistantUnavailableException();
	}

	private static AssistantOutput output(
			String response,
			String marker,
			AssistantStatus status) {
		String message = response.substring(marker.length()).strip();
		if (message.isBlank()) {
			throw new AssistantUnavailableException();
		}
		return new AssistantOutput(message, status);
	}
}
