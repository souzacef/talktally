package com.talktally.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemPromptTests {

	@Test
	void promptExistsAndContainsCriticalFinancialGuardrails() throws IOException {
		var resource = getClass().getResourceAsStream("/prompts/talktally-system.txt");
		assertNotNull(resource);
		String prompt = new String(resource.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();

		assertTrue(prompt.contains("application tools"));
		assertTrue(prompt.contains("never invent amounts"));
		assertTrue(prompt.contains("clarification"));
		assertTrue(prompt.contains("not earned income"));
	}
}
