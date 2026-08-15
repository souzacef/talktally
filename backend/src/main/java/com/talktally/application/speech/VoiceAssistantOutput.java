package com.talktally.application.speech;

import com.talktally.application.assistant.AssistantStatus;

import java.util.Objects;
import java.util.Optional;

public record VoiceAssistantOutput(
		String transcript,
		String message,
		AssistantStatus status,
		SpeechStatus speechStatus,
		Optional<SpeechAudio> audio) {

	public VoiceAssistantOutput {
		Objects.requireNonNull(transcript, "transcript must not be null");
		Objects.requireNonNull(message, "message must not be null");
		Objects.requireNonNull(status, "assistant status must not be null");
		Objects.requireNonNull(speechStatus, "speech status must not be null");
		Objects.requireNonNull(audio, "audio must not be null");
	}
}
