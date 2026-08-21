package com.talktally.infrastructure.ai;

import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistantResponseParserTests {

	@Test
	void parsesCompletedResponse() {
		AssistantOutput output = AssistantResponseParser.parse(
				"[COMPLETED] Recorded the expense.");

		assertEquals(AssistantStatus.COMPLETED, output.status());
		assertEquals("Recorded the expense.", output.message());
	}

	@Test
	void stripsOuterWhitespaceFromCompletedResponse() {
		AssistantOutput output = AssistantResponseParser.parse(
				"  \n[COMPLETED] Recorded the expense.\t ");

		assertEquals(AssistantStatus.COMPLETED, output.status());
		assertEquals("Recorded the expense.", output.message());
	}

	@Test
	void parsesClarificationResponse() {
		AssistantOutput output = AssistantResponseParser.parse(
				"[NEEDS_CLARIFICATION] How much did it cost?");

		assertEquals(AssistantStatus.NEEDS_CLARIFICATION, output.status());
		assertEquals("How much did it cost?", output.message());
	}

	@Test
	void stripsOuterWhitespaceFromClarificationResponse() {
		AssistantOutput output = AssistantResponseParser.parse(
				" \r\n[NEEDS_CLARIFICATION] How much did it cost?  \n");

		assertEquals(AssistantStatus.NEEDS_CLARIFICATION, output.status());
		assertEquals("How much did it cost?", output.message());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("malformedResponses")
	void malformedProviderContentFailsClosed(String scenario, String providerContent) {
		assertThrows(
				AssistantUnavailableException.class,
				() -> AssistantResponseParser.parse(providerContent));
	}

	private static Stream<Arguments> malformedResponses() {
		return Stream.of(
				Arguments.of("null content", null),
				Arguments.of("blank content", "  \n\t "),
				Arguments.of("missing marker", "Recorded the expense."),
				Arguments.of("unknown marker", "[UNKNOWN] Recorded the expense."),
				Arguments.of("wrong-case marker", "[complete] Recorded the expense."),
				Arguments.of(
						"leading prose",
						"Sure. [COMPLETED] Recorded the expense."),
				Arguments.of(
						"nested marker",
						"[[COMPLETED]] Recorded the expense."),
				Arguments.of("completed marker only", "[COMPLETED]    "),
				Arguments.of(
						"clarification marker only",
						"[NEEDS_CLARIFICATION]\n\t"));
	}
}
