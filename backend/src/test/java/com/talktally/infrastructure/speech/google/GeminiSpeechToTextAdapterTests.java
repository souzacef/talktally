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
		assertTrue(message.getText().contains("Do not answer the speech"));
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
