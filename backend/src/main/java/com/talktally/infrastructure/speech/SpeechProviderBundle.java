package com.talktally.infrastructure.speech;

import com.talktally.application.speech.SpeechToTextPort;
import com.talktally.application.speech.TextToSpeechPort;

import java.util.Objects;

final class SpeechProviderBundle implements AutoCloseable {

	private final SpeechToTextPort speechToTextPort;
	private final TextToSpeechPort textToSpeechPort;
	private final AutoCloseable closeable;

	SpeechProviderBundle(
			SpeechToTextPort speechToTextPort,
			TextToSpeechPort textToSpeechPort,
			AutoCloseable closeable) {
		this.speechToTextPort = Objects.requireNonNull(
				speechToTextPort, "speech-to-text port must not be null");
		this.textToSpeechPort = Objects.requireNonNull(
				textToSpeechPort, "text-to-speech port must not be null");
		this.closeable = closeable;
	}

	SpeechToTextPort speechToTextPort() {
		return speechToTextPort;
	}

	TextToSpeechPort textToSpeechPort() {
		return textToSpeechPort;
	}

	@Override
	public void close() throws Exception {
		if (closeable != null) {
			closeable.close();
		}
	}
}
