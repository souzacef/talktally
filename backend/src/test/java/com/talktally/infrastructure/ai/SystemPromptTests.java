package com.talktally.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemPromptTests {

	@Test
	void promptExistsAndContainsCriticalFinancialGuardrails() throws IOException {
		String prompt = promptTemplate().toLowerCase();

		assertTrue(prompt.contains("application tools"));
		assertTrue(prompt.contains("never invent amounts"));
		assertTrue(prompt.contains("clarification"));
		assertTrue(prompt.contains("not earned income"));
	}

	@Test
	void effectivePromptAndToolMetadataExposeAuthoritativeOrdinaryCategories() throws IOException {
		String template = promptTemplate();
		assertTrue(template.contains("{ordinaryTransactionCategories}"));

		List<String> expectedCodes = List.of(
				"SALARY",
				"FREELANCE",
				"FOOD_DINING",
				"GROCERIES",
				"HOUSING",
				"UTILITIES",
				"TRANSPORT",
				"HEALTH",
				"EDUCATION",
				"ENTERTAINMENT",
				"SHOPPING",
				"TRAVEL",
				"TAXES_FEES",
				"OTHER");
		assertEquals(expectedCodes, OrdinaryTransactionCategoryVocabulary.codes());

		String effectivePrompt = template.replace(
				"{ordinaryTransactionCategories}",
				OrdinaryTransactionCategoryVocabulary.systemPromptGuidance());
		assertFalse(effectivePrompt.contains("{ordinaryTransactionCategories}"));
		expectedCodes.forEach(code -> assertTrue(effectivePrompt.contains(code)));

		String normalizedPrompt = effectivePrompt.toLowerCase();
		assertTrue(normalizedPrompt.contains("explicitly names a valid category"));
		assertTrue(normalizedPrompt.contains("dinner"));
		assertTrue(normalizedPrompt.contains("coffee at a cafe"));
		assertTrue(normalizedPrompt.contains("supermarket"));
		assertTrue(normalizedPrompt.contains("grocery-store"));
		assertTrue(normalizedPrompt.contains("reimbursement is reserved"));

		Method recordTransaction = Arrays.stream(TransactionAssistantTools.class.getDeclaredMethods())
				.filter(method -> method.getName().equals("recordTransaction"))
				.findFirst()
				.orElseThrow();
		String inputSchema = JsonSchemaGenerator.generateForMethodInput(recordTransaction);
		expectedCodes.forEach(code -> assertTrue(inputSchema.contains(code)));
		assertTrue(inputSchema.contains("explicitly named valid category"));
		assertTrue(inputSchema.contains("Never use REIMBURSEMENT"));
	}

	private String promptTemplate() throws IOException {
		var resource = getClass().getResourceAsStream("/prompts/talktally-system.txt");
		assertNotNull(resource);
		return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
	}
}
