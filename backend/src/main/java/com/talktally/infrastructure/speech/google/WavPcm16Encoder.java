package com.talktally.infrastructure.speech.google;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class WavPcm16Encoder {

	public static final int HEADER_SIZE = 44;
	public static final int CHANNELS = 1;
	public static final int SAMPLE_RATE = 24_000;
	public static final int BITS_PER_SAMPLE = 16;

	private static final int BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8;

	private WavPcm16Encoder() {
	}

	public static byte[] wrap(byte[] pcm) {
		if (pcm == null || pcm.length == 0) {
			throw new IllegalArgumentException("PCM audio must not be empty");
		}
		if (pcm.length % BYTES_PER_SAMPLE != 0) {
			throw new IllegalArgumentException("16-bit PCM audio must contain complete samples");
		}
		if (pcm.length > Integer.MAX_VALUE - HEADER_SIZE) {
			throw new IllegalArgumentException("PCM audio is too large for WAV packaging");
		}

		ByteBuffer wav = ByteBuffer.allocate(HEADER_SIZE + pcm.length)
				.order(ByteOrder.LITTLE_ENDIAN);
		putAscii(wav, "RIFF");
		wav.putInt(36 + pcm.length);
		putAscii(wav, "WAVE");
		putAscii(wav, "fmt ");
		wav.putInt(16);
		wav.putShort((short) 1);
		wav.putShort((short) CHANNELS);
		wav.putInt(SAMPLE_RATE);
		wav.putInt(SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE);
		wav.putShort((short) (CHANNELS * BYTES_PER_SAMPLE));
		wav.putShort((short) BITS_PER_SAMPLE);
		putAscii(wav, "data");
		wav.putInt(pcm.length);
		wav.put(pcm);
		return wav.array();
	}

	public static boolean isWav(byte[] audio) {
		return audio != null
				&& audio.length >= 12
				&& asciiEquals(audio, 0, "RIFF")
				&& asciiEquals(audio, 8, "WAVE");
	}

	private static void putAscii(ByteBuffer target, String value) {
		target.put(value.getBytes(StandardCharsets.US_ASCII));
	}

	private static boolean asciiEquals(byte[] audio, int offset, String expected) {
		byte[] value = expected.getBytes(StandardCharsets.US_ASCII);
		for (int index = 0; index < value.length; index++) {
			if (audio[offset + index] != value[index]) {
				return false;
			}
		}
		return true;
	}
}
