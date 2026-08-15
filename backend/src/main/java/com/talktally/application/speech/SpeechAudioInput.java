package com.talktally.application.speech;

import java.util.Objects;

public record SpeechAudioInput(byte[] audio, String mediaType) {

	public SpeechAudioInput {
		Objects.requireNonNull(audio, "audio must not be null");
		Objects.requireNonNull(mediaType, "media type must not be null");
		audio = audio.clone();
	}

	@Override
	public byte[] audio() {
		return audio.clone();
	}
}
