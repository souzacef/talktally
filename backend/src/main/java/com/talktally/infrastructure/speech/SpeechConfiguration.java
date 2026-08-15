package com.talktally.infrastructure.speech;

import com.talktally.application.assistant.AssistantUseCase;
import com.talktally.application.speech.SpeechToTextPort;
import com.talktally.application.speech.TextToSpeechPort;
import com.talktally.application.speech.VoiceAssistantUseCase;
import com.talktally.application.speech.exception.SpeechRecognitionUnavailableException;
import com.talktally.application.speech.exception.SpeechSynthesisUnavailableException;
import com.talktally.infrastructure.speech.google.GeminiSpeechToTextAdapter;
import com.talktally.infrastructure.speech.google.GeminiTextToSpeechAdapter;
import com.talktally.infrastructure.speech.google.GoogleSpeechProperties;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GoogleSpeechProperties.class)
public class SpeechConfiguration {

	@Bean(destroyMethod = "close")
	SpeechProviderBundle speechProviderBundle(
			ObjectProvider<ChatModel> chatModelProvider,
			GoogleSpeechProperties properties) {
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null || properties.apiKey().isBlank()) {
			return new SpeechProviderBundle(
					input -> {
						throw new SpeechRecognitionUnavailableException();
					},
					text -> {
						throw new SpeechSynthesisUnavailableException();
					},
					null);
		}
		var ttsClient = new com.talktally.infrastructure.speech.google.GoogleGenAiTtsClient(
				properties.apiKey());
		return new SpeechProviderBundle(
				new GeminiSpeechToTextAdapter(chatModel),
				new GeminiTextToSpeechAdapter(
						ttsClient, properties.ttsModel(), properties.ttsVoice()),
				ttsClient);
	}

	@Bean
	SpeechToTextPort speechToTextPort(SpeechProviderBundle bundle) {
		return bundle.speechToTextPort();
	}

	@Bean
	TextToSpeechPort textToSpeechPort(SpeechProviderBundle bundle) {
		return bundle.textToSpeechPort();
	}

	@Bean
	VoiceAssistantUseCase voiceAssistantUseCase(
			SpeechToTextPort speechToTextPort,
			AssistantUseCase assistantUseCase,
			TextToSpeechPort textToSpeechPort) {
		return new VoiceAssistantUseCase(
				speechToTextPort, assistantUseCase, textToSpeechPort);
	}
}
