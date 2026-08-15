package com.talktally.application.speech;

import java.util.Objects;

public record SpeechAudio(byte[] audio, String contentType) {

	public SpeechAudio {
		Objects.requireNonNull(audio, "audio must not be null");
		Objects.requireNonNull(contentType, "content type must not be null");
		if (audio.length == 0) {
			throw new IllegalArgumentException("speech audio must not be empty");
		}
		if (contentType.isBlank()) {
			throw new IllegalArgumentException("speech content type must not be blank");
		}
		audio = audio.clone();
		contentType = contentType.strip();
	}

	@Override
	public byte[] audio() {
		return audio.clone();
	}
}
