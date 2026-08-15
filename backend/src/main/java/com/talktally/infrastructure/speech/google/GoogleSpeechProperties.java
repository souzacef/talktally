package com.talktally.infrastructure.speech.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "talktally.speech.google")
public record GoogleSpeechProperties(String apiKey, String ttsModel, String ttsVoice) {

	public static final String DEFAULT_TTS_MODEL = "gemini-3.1-flash-tts-preview";
	public static final String DEFAULT_TTS_VOICE = "Kore";

	public GoogleSpeechProperties {
		apiKey = apiKey == null ? "" : apiKey.strip();
		ttsModel = ttsModel == null || ttsModel.isBlank()
				? DEFAULT_TTS_MODEL
				: ttsModel.strip();
		ttsVoice = ttsVoice == null || ttsVoice.isBlank()
				? DEFAULT_TTS_VOICE
				: ttsVoice.strip();
	}
}
