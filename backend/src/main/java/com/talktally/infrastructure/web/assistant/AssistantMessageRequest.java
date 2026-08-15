package com.talktally.infrastructure.web.assistant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.talktally.application.assistant.AssistantUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record AssistantMessageRequest(
		@NotBlank
		@Size(max = AssistantUseCase.MAX_MESSAGE_LENGTH)
		String message) {

	@JsonAnySetter
	void rejectUnknownField(String name, Object value) {
		throw new IllegalArgumentException("unsupported request field");
	}
}
