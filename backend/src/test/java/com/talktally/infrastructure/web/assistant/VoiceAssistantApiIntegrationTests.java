package com.talktally.infrastructure.web.assistant;

import com.jayway.jsonpath.JsonPath;
import com.talktally.application.assistant.AssistantConversationMessage;
import com.talktally.application.assistant.AssistantInput;
import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.application.assistant.ChatAssistantPort;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.application.speech.SpeechAudio;
import com.talktally.application.speech.SpeechAudioInput;
import com.talktally.application.speech.SpeechAudioPolicy;
import com.talktally.application.speech.SpeechToTextPort;
import com.talktally.application.speech.TextToSpeechPort;
import com.talktally.application.speech.exception.SpeechRecognitionUnavailableException;
import com.talktally.application.speech.exception.SpeechSynthesisUnavailableException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.speech.google.WavPcm16Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(VoiceAssistantApiIntegrationTests.FakeVoiceConfiguration.class)
@Transactional
class VoiceAssistantApiIntegrationTests {

	private static final UUID USER_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000081");
	private static final UserId USER = UserId.from(USER_VALUE);
	private static final byte[] WAV = WavPcm16Encoder.wrap(new byte[] { 0, 1, 2, 3 });

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	@Autowired
	private FakeSpeechToTextPort speechToText;

	@Autowired
	private FakeChatAssistantPort assistant;

	@Autowired
	private FakeTextToSpeechPort textToSpeech;

	private String token;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""", USER_VALUE, "voice@example.com", "hash", "Voice User");
		token = accessTokenIssuer.issue(USER).value();
		speechToText.reset();
		assistant.reset();
		textToSpeech.reset();
	}

	@Test
	void endpointRequiresJwt() throws Exception {
		mockMvc.perform(multipart("/api/v1/assistant/voice").file(validFile()))
				.andExpect(status().isUnauthorized());

		assertEquals(0, speechToText.calls);
		assertEquals(0, assistant.calls);
		assertEquals(0, textToSpeech.calls);
	}

	@Test
	void validWavReturnsCompleteJsonContractAndPersistsVoiceTurn() throws Exception {
		MvcResult result = mockMvc.perform(multipart("/api/v1/assistant/voice")
						.file(validFile())
						.header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transcript").value("I spent 42 reais on groceries"))
				.andExpect(jsonPath("$.message").value("Recorded safely."))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.speechStatus").value("GENERATED"))
				.andExpect(jsonPath("$.audio.contentType").value("audio/wav"))
				.andExpect(jsonPath("$.audio.base64").isString())
				.andReturn();

		String returnedBase64 = JsonPath.read(
				result.getResponse().getContentAsString(), "$.audio.base64");
		byte[] returnedAudio = Base64.getDecoder().decode(returnedBase64);
		assertTrue(WavPcm16Encoder.isWav(returnedAudio));
		assertArrayEquals(WAV, speechToText.input.audio());
		assertEquals("audio/wav", speechToText.input.mediaType());
		assertEquals(USER, assistant.actorId);
		assertEquals(TransactionSource.VOICE, assistant.source);
		assertEquals(List.of(), assistant.history);
		assertEquals("I spent 42 reais on groceries", assistant.input.message());
		assertEquals("Recorded safely.", textToSpeech.text);
		assertEquals(2, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM assistant_message WHERE user_id = ?",
				Integer.class,
				USER_VALUE));
		assertEquals("VOICE", jdbcTemplate.queryForObject(
				"SELECT source FROM assistant_message WHERE user_id = ? AND role = 'USER'",
				String.class,
				USER_VALUE));
	}

	@Test
	void unsupportedMimeTypeReturnsBadRequestBeforeAnyProviderCall() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "command.webm", "audio/webm", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/api/v1/assistant/voice")
						.file(file)
						.header("Authorization", bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_AUDIO"));

		assertEquals(0, speechToText.calls);
		assertEquals(0, assistant.calls);
	}

	@Test
	void emptyFileReturnsBadRequestBeforeAnyProviderCall() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "empty.wav", "audio/wav", new byte[0]);

		mockMvc.perform(multipart("/api/v1/assistant/voice")
						.file(file)
						.header("Authorization", bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_AUDIO"));

		assertEquals(0, speechToText.calls);
	}

	@Test
	void oversizedFileReturnsContentTooLargeBeforeReadingOrProviderCall() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"large.wav",
				"audio/wav",
				new byte[SpeechAudioPolicy.MAX_AUDIO_BYTES + 1]);

		mockMvc.perform(multipart("/api/v1/assistant/voice")
						.file(file)
						.header("Authorization", bearer()))
				.andExpect(status().isContentTooLarge())
				.andExpect(jsonPath("$.code").value("AUDIO_TOO_LARGE"));

		assertEquals(0, speechToText.calls);
	}

	@Test
	void speechRecognitionUnavailableReturnsSafeServiceUnavailableAndStopsPipeline()
			throws Exception {
		speechToText.failure = new SpeechRecognitionUnavailableException(
				new IllegalStateException("provider detail"));

		mockMvc.perform(multipart("/api/v1/assistant/voice")
						.file(validFile())
						.header("Authorization", bearer()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("SPEECH_RECOGNITION_UNAVAILABLE"))
				.andExpect(jsonPath("$.message")
						.value("speech recognition is temporarily unavailable"));

		assertEquals(0, assistant.calls);
		assertEquals(0, textToSpeech.calls);
	}

	@Test
	void assistantUnavailableReturnsSafeServiceUnavailableWithoutCallingTts() throws Exception {
		assistant.failure = new AssistantUnavailableException(
				new IllegalStateException("provider detail"));

		mockMvc.perform(multipart("/api/v1/assistant/voice")
						.file(validFile())
						.header("Authorization", bearer()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("ASSISTANT_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("assistant is temporarily unavailable"));

		assertEquals(1, assistant.calls);
		assertEquals(0, textToSpeech.calls);
	}

	@Test
	void ttsUnavailableAfterAssistantSuccessReturnsTextSuccessWithoutRetryingAction()
			throws Exception {
		textToSpeech.failure = new SpeechSynthesisUnavailableException(
				new IllegalStateException("provider detail"));

		mockMvc.perform(multipart("/api/v1/assistant/voice")
						.file(validFile())
						.header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transcript").value("I spent 42 reais on groceries"))
				.andExpect(jsonPath("$.message").value("Recorded safely."))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.speechStatus").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.audio").doesNotExist());

		assertEquals(1, assistant.calls);
		assertEquals(1, textToSpeech.calls);
	}

	private static MockMultipartFile validFile() {
		return new MockMultipartFile("file", "command.wav", "audio/wav", WAV);
	}

	private String bearer() {
		return "Bearer " + token;
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeVoiceConfiguration {

		@Bean
		@Primary
		FakeSpeechToTextPort fakeSpeechToTextPort() {
			return new FakeSpeechToTextPort();
		}

		@Bean
		@Primary
		FakeChatAssistantPort fakeChatAssistantPort() {
			return new FakeChatAssistantPort();
		}

		@Bean
		@Primary
		FakeTextToSpeechPort fakeTextToSpeechPort() {
			return new FakeTextToSpeechPort();
		}
	}

	static final class FakeSpeechToTextPort implements SpeechToTextPort {

		private int calls;
		private SpeechAudioInput input;
		private RuntimeException failure;

		@Override
		public String transcribe(SpeechAudioInput input) {
			calls++;
			this.input = input;
			if (failure != null) {
				throw failure;
			}
			return "I spent 42 reais on groceries";
		}

		void reset() {
			calls = 0;
			input = null;
			failure = null;
		}
	}

	static final class FakeChatAssistantPort implements ChatAssistantPort {

		private int calls;
		private UserId actorId;
		private TransactionSource source;
		private List<AssistantConversationMessage> history = List.of();
		private AssistantInput input;
		private RuntimeException failure;

		@Override
		public AssistantOutput respond(
				UserId actorId,
				TransactionSource source,
				List<AssistantConversationMessage> history,
				AssistantInput input) {
			calls++;
			this.actorId = actorId;
			this.source = source;
			this.history = history;
			this.input = input;
			if (failure != null) {
				throw failure;
			}
			return new AssistantOutput("Recorded safely.", AssistantStatus.COMPLETED);
		}

		void reset() {
			calls = 0;
			actorId = null;
			source = null;
			history = List.of();
			input = null;
			failure = null;
		}
	}

	static final class FakeTextToSpeechPort implements TextToSpeechPort {

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
			return new SpeechAudio(WAV, "audio/wav");
		}

		void reset() {
			calls = 0;
			text = null;
			failure = null;
		}
	}
}
