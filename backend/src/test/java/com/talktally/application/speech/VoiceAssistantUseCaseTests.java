package com.talktally.application.speech;

import com.talktally.application.assistant.AssistantInput;
import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.application.assistant.AssistantUseCase;
import com.talktally.application.assistant.ChatAssistantPort;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.application.speech.exception.InvalidAudioException;
import com.talktally.application.speech.exception.SpeechRecognitionUnavailableException;
import com.talktally.application.speech.exception.SpeechSynthesisUnavailableException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceAssistantUseCaseTests {

	private static final UserId ACTOR = UserId.generate();
	private static final byte[] INPUT_AUDIO = { 1, 2, 3, 4 };
	private static final byte[] OUTPUT_AUDIO = { 10, 20, 30, 40 };

	private FakeSpeechToTextPort speechToText;
	private FakeChatAssistantPort assistant;
	private FakeTextToSpeechPort textToSpeech;
	private VoiceAssistantUseCase useCase;

	@BeforeEach
	void setUp() {
		speechToText = new FakeSpeechToTextPort();
		assistant = new FakeChatAssistantPort();
		textToSpeech = new FakeTextToSpeechPort();
		useCase = new VoiceAssistantUseCase(
				speechToText, new AssistantUseCase(assistant), textToSpeech);
	}

	@Test
	void transcriptionIsForwardedWithAuthenticatedActorAndVoiceSource() {
		speechToText.transcript = "  I spent 42 reais on groceries  ";

		VoiceAssistantOutput output = useCase.execute(ACTOR, input());

		assertEquals("I spent 42 reais on groceries", assistant.input.message());
		assertSame(ACTOR, assistant.actorId);
		assertEquals(TransactionSource.VOICE, assistant.source);
		assertEquals("I spent 42 reais on groceries", output.transcript());
	}

	@Test
	void successfulAssistantMessageIsSynthesizedAndReturnedAsSpeech() {
		String visibleMessage = "You spent R$ 439.90 in August 2026.";
		assistant.output = new AssistantOutput(visibleMessage, AssistantStatus.COMPLETED);

		VoiceAssistantOutput output = useCase.execute(ACTOR, input());

		assertEquals(visibleMessage, textToSpeech.text);
		assertEquals(visibleMessage, output.message());
		assertEquals(AssistantStatus.COMPLETED, output.status());
		assertEquals(SpeechStatus.GENERATED, output.speechStatus());
		assertArrayEquals(OUTPUT_AUDIO, output.audio().orElseThrow().audio());
	}

	@Test
	void clarificationResponseIsAlsoSynthesized() {
		assistant.output = new AssistantOutput(
				"Which category should I use?", AssistantStatus.NEEDS_CLARIFICATION);

		VoiceAssistantOutput output = useCase.execute(ACTOR, input());

		assertEquals("Which category should I use?", textToSpeech.text);
		assertEquals(AssistantStatus.NEEDS_CLARIFICATION, output.status());
		assertEquals(SpeechStatus.GENERATED, output.speechStatus());
	}

	@Test
	void speechRecognitionFailurePreventsAssistantAndSynthesisCalls() {
		speechToText.failure = new SpeechRecognitionUnavailableException();

		assertThrows(SpeechRecognitionUnavailableException.class,
				() -> useCase.execute(ACTOR, input()));
		assertEquals(0, assistant.calls);
		assertEquals(0, textToSpeech.calls);
	}

	@Test
	void assistantFailurePreventsSpeechSynthesis() {
		assistant.failure = new AssistantUnavailableException();

		assertThrows(AssistantUnavailableException.class,
				() -> useCase.execute(ACTOR, input()));
		assertEquals(1, assistant.calls);
		assertEquals(0, textToSpeech.calls);
	}

	@Test
	void synthesisFailurePreservesSuccessfulTextWithoutRepeatingAssistant() {
		assistant.output = new AssistantOutput("Recorded once.", AssistantStatus.COMPLETED);
		textToSpeech.failure = new SpeechSynthesisUnavailableException();

		VoiceAssistantOutput output = useCase.execute(ACTOR, input());

		assertEquals(1, assistant.calls);
		assertEquals(1, textToSpeech.calls);
		assertEquals("Recorded once.", output.message());
		assertEquals(AssistantStatus.COMPLETED, output.status());
		assertEquals(SpeechStatus.UNAVAILABLE, output.speechStatus());
		assertEquals(Optional.empty(), output.audio());
	}

	@Test
	void blankTranscriptionIsRejectedBeforeAssistantAndSynthesis() {
		speechToText.transcript = "  ";

		assertThrows(InvalidAudioException.class, () -> useCase.execute(ACTOR, input()));
		assertEquals(0, assistant.calls);
		assertEquals(0, textToSpeech.calls);
	}

	@Test
	void inputAndOutputAudioArraysAreDefensivelyCopied() {
		byte[] callerAudio = INPUT_AUDIO.clone();
		VoiceAssistantInput input = new VoiceAssistantInput(callerAudio, "audio/wav");
		callerAudio[0] = 99;

		VoiceAssistantOutput output = useCase.execute(ACTOR, input);
		byte[] exposedInput = speechToText.input.audio();
		exposedInput[1] = 99;
		byte[] exposedOutput = output.audio().orElseThrow().audio();
		exposedOutput[0] = 99;

		assertArrayEquals(INPUT_AUDIO, speechToText.input.audio());
		assertArrayEquals(OUTPUT_AUDIO, output.audio().orElseThrow().audio());
		assertTrue(output.audio().isPresent());
	}

	private static VoiceAssistantInput input() {
		return new VoiceAssistantInput(INPUT_AUDIO, "audio/wav");
	}

	private static final class FakeSpeechToTextPort implements SpeechToTextPort {

		private String transcript = "Record groceries";
		private RuntimeException failure;
		private SpeechAudioInput input;

		@Override
		public String transcribe(SpeechAudioInput input) {
			this.input = input;
			if (failure != null) {
				throw failure;
			}
			return transcript;
		}
	}

	private static final class FakeChatAssistantPort implements ChatAssistantPort {

		private int calls;
		private UserId actorId;
		private TransactionSource source;
		private AssistantInput input;
		private AssistantOutput output =
				new AssistantOutput("Recorded.", AssistantStatus.COMPLETED);
		private RuntimeException failure;

		@Override
		public AssistantOutput respond(
				UserId actorId,
				TransactionSource source,
				AssistantInput input) {
			calls++;
			this.actorId = actorId;
			this.source = source;
			this.input = input;
			if (failure != null) {
				throw failure;
			}
			return output;
		}
	}

	private static final class FakeTextToSpeechPort implements TextToSpeechPort {

		private int calls;
		private String text;
		private RuntimeException failure;

		@Override
		public SpeechAudio synthesize(String text) {
			calls++;
			this.text = text;
			if (failure != null) {
				throw failure;
			}
			return new SpeechAudio(OUTPUT_AUDIO, "audio/wav");
		}
	}
}
