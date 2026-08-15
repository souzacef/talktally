package com.talktally.infrastructure.speech.google;

import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.talktally.application.speech.SpeechAudio;
import com.talktally.application.speech.SpeechSynthesisPolicy;
import com.talktally.application.speech.exception.InvalidSpeechTextException;
import com.talktally.application.speech.exception.SpeechSynthesisUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiTextToSpeechAdapterTests {

	private static final String MODEL = "gemini-tts-test";
	private static final String VOICE = "Kore";
	private static final byte[] PCM = { 0, 1, 2, 3 };

	@Test
	void finalAssistantTextConfiguredModelAndVoiceAreForwardedWithoutRewriting() {
		CapturingTtsClient client = new CapturingTtsClient(response(PCM, "audio/L16;rate=24000"));
		String assistantText = "Done. I recorded R$42.00 in groceries.";

		SpeechAudio speech = adapter(client).synthesize(assistantText);

		assertEquals(MODEL, client.model);
		assertEquals(
				"Read the following response naturally and clearly, without changing its wording:\n"
						+ assistantText,
				client.prompt);
		assertEquals(List.of("AUDIO"), client.config.responseModalities().orElseThrow());
		assertEquals(
				VOICE,
				client.config.speechConfig().orElseThrow()
						.voiceConfig().orElseThrow()
						.prebuiltVoiceConfig().orElseThrow()
						.voiceName().orElseThrow());
		assertTrue(client.config.tools().isEmpty());
		assertEquals("audio/wav", speech.contentType());
		assertTrue(WavPcm16Encoder.isWav(speech.audio()));
	}

	@Test
	void emptyTextIsRejectedBeforeProviderInvocation() {
		CapturingTtsClient client = new CapturingTtsClient(response(PCM, "audio/pcm"));

		assertThrows(InvalidSpeechTextException.class, () -> adapter(client).synthesize("  "));
		assertEquals(0, client.calls);
	}

	@Test
	void oversizedTextIsRejectedInsteadOfSilentlyTruncated() {
		CapturingTtsClient client = new CapturingTtsClient(response(PCM, "audio/pcm"));

		assertThrows(InvalidSpeechTextException.class,
				() -> adapter(client).synthesize(
						"x".repeat(SpeechSynthesisPolicy.MAX_TEXT_LENGTH + 1)));
		assertEquals(0, client.calls);
	}

	@Test
	void providerFailuresAreSanitizedAtTheApplicationBoundary() {
		CapturingTtsClient client = new CapturingTtsClient(response(PCM, "audio/pcm"));
		client.failure = new IllegalStateException("secret provider response");

		SpeechSynthesisUnavailableException exception =
				assertThrows(SpeechSynthesisUnavailableException.class,
						() -> adapter(client).synthesize("Safe assistant response"));

		assertEquals("speech synthesis is temporarily unavailable", exception.getMessage());
	}

	@Test
	void rawPcmIsWrappedInAValidWavContainer() {
		CapturingTtsClient client = new CapturingTtsClient(response(PCM, "audio/pcm"));

		byte[] wav = adapter(client).synthesize("Speak this").audio();

		assertTrue(WavPcm16Encoder.isWav(wav));
		assertEquals(WavPcm16Encoder.HEADER_SIZE + PCM.length, wav.length);
		assertArrayEquals(PCM, java.util.Arrays.copyOfRange(
				wav, WavPcm16Encoder.HEADER_SIZE, wav.length));
	}

	@Test
	void completeWavProviderOutputIsNotDoubleWrapped() {
		byte[] wav = WavPcm16Encoder.wrap(PCM);
		CapturingTtsClient client = new CapturingTtsClient(response(wav, "audio/wav"));

		byte[] result = adapter(client).synthesize("Speak this").audio();

		assertArrayEquals(wav, result);
		assertEquals(wav.length, result.length);
	}

	@Test
	void unknownProviderAudioFormatIsRejectedRatherThanMislabelledAsWav() {
		CapturingTtsClient client = new CapturingTtsClient(response(PCM, "audio/mpeg"));

		assertThrows(SpeechSynthesisUnavailableException.class,
				() -> adapter(client).synthesize("Speak this"));
		assertFalse(WavPcm16Encoder.isWav(PCM));
	}

	private static GeminiTextToSpeechAdapter adapter(CapturingTtsClient client) {
		return new GeminiTextToSpeechAdapter(client, MODEL, VOICE);
	}

	private static GenerateContentResponse response(byte[] audio, String mediaType) {
		return GenerateContentResponse.builder()
				.candidates(Candidate.builder()
						.content(Content.builder()
								.role("model")
								.parts(Part.fromBytes(audio, mediaType))))
				.build();
	}

	private static final class CapturingTtsClient implements GeminiTtsClient {

		private final GenerateContentResponse response;
		private int calls;
		private String model;
		private String prompt;
		private GenerateContentConfig config;
		private RuntimeException failure;

		private CapturingTtsClient(GenerateContentResponse response) {
			this.response = response;
		}

		@Override
		public GenerateContentResponse generate(
				String model,
				String prompt,
				GenerateContentConfig config) {
			calls++;
			this.model = model;
			this.prompt = prompt;
			this.config = config;
			if (failure != null) {
				throw failure;
			}
			return response;
		}
	}
}
