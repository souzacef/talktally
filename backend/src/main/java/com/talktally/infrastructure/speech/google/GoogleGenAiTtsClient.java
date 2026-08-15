package com.talktally.infrastructure.speech.google;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

import java.util.Objects;

public final class GoogleGenAiTtsClient implements GeminiTtsClient, AutoCloseable {

	private final Client client;

	public GoogleGenAiTtsClient(String apiKey) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalArgumentException("Google API key must not be blank");
		}
		this.client = Client.builder().apiKey(apiKey).build();
	}

	@Override
	public GenerateContentResponse generate(
			String model,
			String prompt,
			GenerateContentConfig config) {
		return client.models.generateContent(
				Objects.requireNonNull(model, "model must not be null"),
				Objects.requireNonNull(prompt, "prompt must not be null"),
				Objects.requireNonNull(config, "config must not be null"));
	}

	@Override
	public void close() {
		client.close();
	}
}
