package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantStatus;

public record AssistantMessageResponse(String message, AssistantStatus status) {

	static AssistantMessageResponse from(AssistantOutput output) {
		return new AssistantMessageResponse(output.message(), output.status());
	}
}
