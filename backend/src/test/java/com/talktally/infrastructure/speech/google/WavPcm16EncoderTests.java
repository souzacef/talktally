package com.talktally.infrastructure.speech.google;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavPcm16EncoderTests {

	@Test
	void wrapsMonoTwentyFourKilohertzPcm16WithCorrectWavStructure() {
		byte[] pcm = { 0, 1, 2, 3, 4, 5 };

		byte[] wav = WavPcm16Encoder.wrap(pcm);
		ByteBuffer littleEndian = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);

		assertEquals("RIFF", ascii(wav, 0, 4));
		assertEquals(36 + pcm.length, littleEndian.getInt(4));
		assertEquals("WAVE", ascii(wav, 8, 4));
		assertEquals("fmt ", ascii(wav, 12, 4));
		assertEquals(16, littleEndian.getInt(16));
		assertEquals(1, littleEndian.getShort(20));
		assertEquals(1, littleEndian.getShort(22));
		assertEquals(24_000, littleEndian.getInt(24));
		assertEquals(48_000, littleEndian.getInt(28));
		assertEquals(2, littleEndian.getShort(32));
		assertEquals(16, littleEndian.getShort(34));
		assertEquals("data", ascii(wav, 36, 4));
		assertEquals(pcm.length, littleEndian.getInt(40));
		assertEquals(WavPcm16Encoder.HEADER_SIZE + pcm.length, wav.length);
		assertArrayEquals(pcm, java.util.Arrays.copyOfRange(
				wav, WavPcm16Encoder.HEADER_SIZE, wav.length));
		assertTrue(WavPcm16Encoder.isWav(wav));
	}

	@Test
	void rejectsEmptyOrIncompletePcmSamplesAndDoesNotMisidentifyRawPcm() {
		assertThrows(IllegalArgumentException.class, () -> WavPcm16Encoder.wrap(new byte[0]));
		assertThrows(IllegalArgumentException.class, () -> WavPcm16Encoder.wrap(new byte[] { 1 }));
		assertFalse(WavPcm16Encoder.isWav(new byte[] { 1, 2, 3, 4 }));
	}

	private static String ascii(byte[] value, int offset, int length) {
		return new String(value, offset, length, StandardCharsets.US_ASCII);
	}
}
