package com.talktally.infrastructure.web.assistant;

import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssistantConversationOwnershipIntegrationTests {

	private static final UUID USER_A_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000073");
	private static final UUID USER_B_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000074");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	private String tokenA;
	private String tokenB;

	@BeforeEach
	void setUp() {
		insertUser(USER_A_VALUE, "conversation-a@example.com");
		insertUser(USER_B_VALUE, "conversation-b@example.com");
		jdbcTemplate.update("""
				INSERT INTO assistant_message
					(user_id, role, content, source, status, created_at)
				VALUES (?, 'USER', 'Private message from A', 'ASSISTANT_TEXT', NULL, CURRENT_TIMESTAMP)
				""", USER_A_VALUE);
		tokenA = accessTokenIssuer.issue(UserId.from(USER_A_VALUE)).value();
		tokenB = accessTokenIssuer.issue(UserId.from(USER_B_VALUE)).value();
	}

	@Test
	void anotherUserCannotReadOrClearTheOwnersConversation() throws Exception {
		mockMvc.perform(get("/api/v1/assistant/messages")
						.header("Authorization", bearer(tokenB)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		mockMvc.perform(delete("/api/v1/assistant/messages")
						.header("Authorization", bearer(tokenB)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/assistant/messages")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].content").value("Private message from A"));
	}

	private void insertUser(UUID id, String email) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""", id, email, "hash", email);
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
