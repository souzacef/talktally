package com.talktally.infrastructure.speech.google;

import com.talktally.application.speech.SpeechAudioInput;
import com.talktally.application.speech.exception.InvalidAudioException;
import com.talktally.application.speech.exception.SpeechRecognitionUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiSpeechToTextAdapterTests {

	private static final byte[] AUDIO = { 1, 2, 3, 4 };

	@Test
	void documentedMimeTypesAndSafeAliasesAreAcceptedAndNormalized() {
		Map<String, String> supported = new LinkedHashMap<>();
		supported.put("audio/wav", "audio/wav");
		supported.put("audio/x-wav", "audio/wav");
		supported.put("audio/mp3", "audio/mpeg");
		supported.put("audio/mpeg", "audio/mpeg");
		supported.put("audio/aac", "audio/aac");
		supported.put("audio/ogg", "audio/ogg");
		supported.put("audio/flac", "audio/flac");
		supported.put("audio/aiff", "audio/aiff");
		supported.put("audio/x-aiff", "audio/aiff");

		for (Map.Entry<String, String> format : supported.entrySet()) {
			CapturingChatModel model = new CapturingChatModel("transcript");
			new GeminiSpeechToTextAdapter(model).transcribe(
					new SpeechAudioInput(AUDIO, format.getKey() + "; charset=binary"));

			Media media = model.prompt.getUserMessage().getMedia().getFirst();
			assertEquals(format.getValue(), media.getMimeType().toString());
			assertArrayEquals(AUDIO, media.getDataAsByteArray());
		}
	}

	@Test
	void unsupportedMimeTypeIsRejectedBeforeProviderInvocation() {
		CapturingChatModel model = new CapturingChatModel("unused");

		assertThrows(InvalidAudioException.class,
				() -> new GeminiSpeechToTextAdapter(model).transcribe(
						new SpeechAudioInput(AUDIO, "audio/webm")));
		assertEquals(0, model.calls);
	}

	@Test
	void emptyAudioIsRejectedBeforeProviderInvocation() {
		CapturingChatModel model = new CapturingChatModel("unused");

		assertThrows(InvalidAudioException.class,
				() -> new GeminiSpeechToTextAdapter(model).transcribe(
						new SpeechAudioInput(new byte[0], "audio/wav")));
		assertEquals(0, model.calls);
	}

	@Test
	void providerTranscriptionIsTrimmed() {
		CapturingChatModel model = new CapturingChatModel("  Olá, TalkTally.  \n");

		String transcript = new GeminiSpeechToTextAdapter(model).transcribe(
				new SpeechAudioInput(AUDIO, "audio/wav"));

		assertEquals("Olá, TalkTally.", transcript);
	}

	@Test
	void codeSwitchedBrazilianCurrencyTermsAreReturnedWithoutPostProcessing() {
		String providerTranscript = "Record an expense of twelve reais and thirty-four centavos for coffee today.";
		CapturingChatModel model = new CapturingChatModel(providerTranscript);

		String transcript = new GeminiSpeechToTextAdapter(model).transcribe(
				new SpeechAudioInput(AUDIO, "audio/wav"));

		assertEquals(providerTranscript, transcript);
	}

	@Test
	void promptPreservesSpokenLanguageAndSemanticIdentity() {
		String prompt = GeminiSpeechToTextAdapter.TRANSCRIPTION_PROMPT;

		assertTrue(prompt.contains("Preserve the language spoken"));
		assertTrue(prompt.contains("code-switching"));
		assertTrue(prompt.contains("never translate"));
		assertTrue(prompt.contains("currencies"));
		assertTrue(prompt.contains("units of measurement"));
		assertTrue(prompt.contains("proper names"));
		assertTrue(prompt.contains("merchant and person names"));
		assertTrue(prompt.contains("category words"));
		assertTrue(prompt.contains("dates"));
		assertTrue(prompt.contains("reais or centavos"));
		assertTrue(prompt.contains("never render them as dollars or cents"));
		assertTrue(prompt.contains("number words to digits"));
		assertTrue(prompt.contains("meaning is unchanged"));
		assertTrue(prompt.contains("infer intent"));
		assertTrue(prompt.contains("perform any requested action"));
	}

	@Test
	void blankProviderTranscriptionIsRejectedAsInvalidAudio() {
		CapturingChatModel model = new CapturingChatModel("  \n");

		assertThrows(InvalidAudioException.class,
				() -> new GeminiSpeechToTextAdapter(model).transcribe(
						new SpeechAudioInput(AUDIO, "audio/wav")));
	}

	@Test
	void requestContainsOnlyNarrowTranscriptionPromptAndAudio() {
		CapturingChatModel model = new CapturingChatModel("hello");

		new GeminiSpeechToTextAdapter(model).transcribe(
				new SpeechAudioInput(AUDIO, "audio/wav"));

		assertEquals(1, model.prompt.getInstructions().size());
		UserMessage message = assertInstanceOf(
				UserMessage.class, model.prompt.getInstructions().getFirst());
		assertEquals(GeminiSpeechToTextAdapter.TRANSCRIPTION_PROMPT, message.getText());
		assertEquals(1, message.getMedia().size());
		assertEquals("voice-command-audio", message.getMedia().getFirst().getName());
		assertTrue(message.getMetadata().keySet().stream()
				.map(String::toLowerCase)
				.noneMatch(key -> key.contains("user")
						|| key.contains("jwt")
						|| key.contains("tool")
						|| key.contains("context")));
		assertNull(model.prompt.getOptions());
		assertTrue(model.prompt.getSystemMessages().isEmpty());
		assertTrue(message.getText().contains("Return only the transcription"));
		assertTrue(message.getText().contains("Do not answer, interpret, infer intent"));
		assertTrue(List.of(
				"TalkTally", "UserId", "TransactionSource", "application tools", "category code")
				.stream()
				.noneMatch(message.getText()::contains));
	}

	@Test
	void providerFailuresAreSanitizedAtTheApplicationBoundary() {
		CapturingChatModel model = new CapturingChatModel("unused");
		model.failure = new IllegalStateException("secret provider response");

		SpeechRecognitionUnavailableException exception =
				assertThrows(SpeechRecognitionUnavailableException.class,
						() -> new GeminiSpeechToTextAdapter(model).transcribe(
								new SpeechAudioInput(AUDIO, "audio/wav")));

		assertEquals("speech recognition is temporarily unavailable", exception.getMessage());
	}

	private static final class CapturingChatModel implements ChatModel {

		private final String response;
		private int calls;
		private Prompt prompt;
		private RuntimeException failure;

		private CapturingChatModel(String response) {
			this.response = response;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			calls++;
			this.prompt = prompt;
			if (failure != null) {
				throw failure;
			}
			return new ChatResponse(List.of(
					new Generation(new AssistantMessage(response))));
		}
	}
}
