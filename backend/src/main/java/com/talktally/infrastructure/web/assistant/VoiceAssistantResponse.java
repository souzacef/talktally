package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.AssistantStatus;
import com.talktally.application.speech.SpeechAudio;
import com.talktally.application.speech.SpeechStatus;
import com.talktally.application.speech.VoiceAssistantOutput;
import org.jspecify.annotations.Nullable;

import java.util.Base64;

public record VoiceAssistantResponse(
		String transcript,
		String message,
		AssistantStatus status,
		SpeechStatus speechStatus,
		@Nullable AudioResponse audio) {

	static VoiceAssistantResponse from(VoiceAssistantOutput output) {
		return new VoiceAssistantResponse(
				output.transcript(),
				output.message(),
				output.status(),
				output.speechStatus(),
				output.audio().map(AudioResponse::from).orElse(null));
	}

	public record AudioResponse(String contentType, String base64) {

		static AudioResponse from(SpeechAudio audio) {
			return new AudioResponse(
					audio.contentType(),
					Base64.getEncoder().encodeToString(audio.audio()));
		}
	}
}
