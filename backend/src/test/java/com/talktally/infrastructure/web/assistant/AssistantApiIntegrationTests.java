package com.talktally.infrastructure.web.assistant;

import com.talktally.application.assistant.AssistantConversationMessage;
import com.talktally.application.assistant.AssistantInput;
import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.application.assistant.ChatAssistantPort;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AssistantApiIntegrationTests.FakeAssistantConfiguration.class)
@Transactional
class AssistantApiIntegrationTests {

	private static final UUID USER_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000071");
	private static final UserId USER = UserId.from(USER_VALUE);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	@Autowired
	private FakeChatAssistantPort fakePort;

	private String token;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""", USER_VALUE, "assistant@example.com", "hash", "Assistant User");
		token = accessTokenIssuer.issue(USER).value();
		fakePort.reset();
	}

	@Test
	void endpointsRequireJwt() throws Exception {
		mockMvc.perform(get("/api/v1/assistant/messages"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/assistant/messages")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/assistant/messages"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void successfulTurnIsPersistedAndReturnedAsUserOwnedHistory() throws Exception {
		mockMvc.perform(post("/api/v1/assistant/messages")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"Record lunch\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Safe fake response"))
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		assertEquals(USER, fakePort.actorId);
		assertEquals(TransactionSource.ASSISTANT_TEXT, fakePort.source);

		mockMvc.perform(get("/api/v1/assistant/messages")
						.header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].role").value("USER"))
				.andExpect(jsonPath("$[0].content").value("Record lunch"))
				.andExpect(jsonPath("$[0].source").value("ASSISTANT_TEXT"))
				.andExpect(jsonPath("$[0].status").doesNotExist())
				.andExpect(jsonPath("$[1].role").value("ASSISTANT"))
				.andExpect(jsonPath("$[1].content").value("Safe fake response"))
				.andExpect(jsonPath("$[1].source").doesNotExist())
				.andExpect(jsonPath("$[1].status").value("COMPLETED"));
	}

	@Test
	void nextTurnReceivesPreviousExchangeAsConversationContext() throws Exception {
		send("I paid for dinner");
		assertEquals(List.of(), fakePort.history);

		send("Rose owes me");

		assertEquals(2, fakePort.history.size());
		assertEquals("I paid for dinner", fakePort.history.get(0).content());
		assertEquals("Safe fake response", fakePort.history.get(1).content());
	}

	@Test
	void clearRemovesConversationAndResetsModelContext() throws Exception {
		send("hello");

		mockMvc.perform(delete("/api/v1/assistant/messages")
						.header("Authorization", bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/assistant/messages")
						.header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		send("fresh start");
		assertEquals(List.of(), fakePort.history);
	}

	@Test
	void blankAndOversizedMessagesReturnBadRequestWithoutCallingPort() throws Exception {
		mockMvc.perform(post("/api/v1/assistant/messages")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"  \"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/v1/assistant/messages")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"" + "x".repeat(4_001) + "\"}"))
				.andExpect(status().isBadRequest());

		assertEquals(0, fakePort.calls);
	}

	@Test
	void serverOwnedFieldsAreRejected() throws Exception {
		mockMvc.perform(post("/api/v1/assistant/messages")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"message":"hello","userId":"00000000-0000-0000-0000-000000000000","source":"MANUAL","model":"other"}
								"""))
				.andExpect(status().isBadRequest());

		assertEquals(0, fakePort.calls);
	}

	@Test
	void providerFailureReturnsSafeServiceUnavailablePayloadWithoutPersistingTurn() throws Exception {
		fakePort.unavailable = true;

		mockMvc.perform(post("/api/v1/assistant/messages")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("ASSISTANT_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("assistant is temporarily unavailable"));

		mockMvc.perform(get("/api/v1/assistant/messages")
						.header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	private void send(String message) throws Exception {
		mockMvc.perform(post("/api/v1/assistant/messages")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"" + message + "\"}"))
				.andExpect(status().isOk());
	}

	private String bearer() {
		return "Bearer " + token;
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeAssistantConfiguration {

		@Bean
		@Primary
		FakeChatAssistantPort fakeChatAssistantPort() {
			return new FakeChatAssistantPort();
		}
	}

	static final class FakeChatAssistantPort implements ChatAssistantPort {

		private int calls;
		private UserId actorId;
		private TransactionSource source;
		private List<AssistantConversationMessage> history = List.of();
		private boolean unavailable;

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
			if (unavailable) {
				throw new AssistantUnavailableException(new RuntimeException("provider detail"));
			}
			return new AssistantOutput("Safe fake response", AssistantStatus.COMPLETED);
		}

		void reset() {
			calls = 0;
			actorId = null;
			source = null;
			history = List.of();
			unavailable = false;
		}
	}
}
