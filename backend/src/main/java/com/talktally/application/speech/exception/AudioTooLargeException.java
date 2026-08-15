package com.talktally.application.speech.exception;

public final class AudioTooLargeException extends InvalidAudioException {

	public AudioTooLargeException(long maximumBytes) {
		super("audio must not exceed " + maximumBytes + " bytes");
	}
}
