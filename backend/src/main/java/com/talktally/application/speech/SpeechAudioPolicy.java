package com.talktally.application.speech;

import com.talktally.application.speech.exception.AudioTooLargeException;
import com.talktally.application.speech.exception.InvalidAudioException;

import java.util.Locale;
import java.util.Map;

public final class SpeechAudioPolicy {

	public static final int MAX_AUDIO_BYTES = 8 * 1024 * 1024;

	private static final Map<String, String> SUPPORTED_MEDIA_TYPES = Map.ofEntries(
			Map.entry("audio/wav", "audio/wav"),
			Map.entry("audio/x-wav", "audio/wav"),
			Map.entry("audio/mp3", "audio/mpeg"),
			Map.entry("audio/mpeg", "audio/mpeg"),
			Map.entry("audio/aac", "audio/aac"),
			Map.entry("audio/ogg", "audio/ogg"),
			Map.entry("audio/flac", "audio/flac"),
			Map.entry("audio/aiff", "audio/aiff"),
			Map.entry("audio/x-aiff", "audio/aiff"));

	private SpeechAudioPolicy() {
	}

	public static String validateAndNormalize(SpeechAudioInput input) {
		if (input == null) {
			throw new InvalidAudioException("audio input is required");
		}
		byte[] audio = input.audio();
		validateSize(audio.length);
		String mediaType = normalize(input.mediaType());
		if (mediaType == null) {
			throw new InvalidAudioException("audio media type is unsupported");
		}
		return mediaType;
	}

	public static void validateSize(long size) {
		if (size == 0) {
			throw new InvalidAudioException("audio must not be empty");
		}
		if (size < 0 || size > MAX_AUDIO_BYTES) {
			throw new AudioTooLargeException(MAX_AUDIO_BYTES);
		}
	}

	private static String normalize(String mediaType) {
		if (mediaType == null || mediaType.isBlank()) {
			return null;
		}
		String normalized = mediaType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
		return SUPPORTED_MEDIA_TYPES.get(normalized);
	}
}
