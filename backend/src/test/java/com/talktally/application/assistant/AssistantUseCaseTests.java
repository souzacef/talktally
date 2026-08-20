package com.talktally.application.assistant;

import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.application.assistant.exception.InvalidAssistantInputException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistantUseCaseTests {

	private static final UserId ACTOR = UserId.generate();

	@Test
	void blankMessageIsRejectedBeforeCallingPort() {
		CapturingPort port = new CapturingPort();
		CapturingConversation conversation = new CapturingConversation();

		assertThrows(InvalidAssistantInputException.class,
				() -> new AssistantUseCase(port, conversation).execute(
						ACTOR, TransactionSource.ASSISTANT_TEXT, "  "));
		assertEquals(0, port.calls);
		assertEquals(0, conversation.findCalls);
	}

	@Test
	void oversizedMessageIsRejectedBeforeCallingPort() {
		CapturingPort port = new CapturingPort();
		CapturingConversation conversation = new CapturingConversation();

		assertThrows(InvalidAssistantInputException.class,
				() -> new AssistantUseCase(port, conversation).execute(
						ACTOR,
						TransactionSource.ASSISTANT_TEXT,
						"x".repeat(AssistantUseCase.MAX_MESSAGE_LENGTH + 1)));
		assertEquals(0, port.calls);
		assertEquals(0, conversation.findCalls);
	}

	@Test
	void authenticatedActorAndTrustedSourceArePassedToPort() {
		CapturingPort port = new CapturingPort();
		CapturingConversation conversation = new CapturingConversation();

		new AssistantUseCase(port, conversation).execute(ACTOR, TransactionSource.VOICE, "hello");

		assertSame(ACTOR, port.actorId);
		assertEquals(TransactionSource.VOICE, port.source);
	}

	@Test
	void recentConversationIsPassedToProviderBeforeCurrentMessage() {
		CapturingPort port = new CapturingPort();
		CapturingConversation conversation = new CapturingConversation();
		conversation.history = List.of(
				new AssistantConversationMessage(
						1,
						AssistantConversationRole.USER,
						"I paid for dinner.",
						TransactionSource.ASSISTANT_TEXT,
						null,
						Instant.parse("2026-08-20T18:00:00Z")),
				new AssistantConversationMessage(
						2,
						AssistantConversationRole.ASSISTANT,
						"Who owes you for it?",
						null,
						AssistantStatus.NEEDS_CLARIFICATION,
						Instant.parse("2026-08-20T18:00:01Z")));

		new AssistantUseCase(port, conversation).execute(
				ACTOR, TransactionSource.ASSISTANT_TEXT, "Rose does");

		assertEquals(AssistantUseCase.MODEL_HISTORY_LIMIT, conversation.requestedLimit);
		assertSame(conversation.history, port.history);
		assertEquals("Rose does", port.input.message());
	}

	@Test
	void successfulExchangeIsPersistedAndResponseReturned() {
		CapturingPort port = new CapturingPort();
		CapturingConversation conversation = new CapturingConversation();
		port.output = new AssistantOutput("Recorded.", AssistantStatus.COMPLETED);

		AssistantOutput output = new AssistantUseCase(port, conversation).execute(
				ACTOR, TransactionSource.ASSISTANT_TEXT, "  record it  ");

		assertSame(port.output, output);
		assertSame(ACTOR, conversation.appendActorId);
		assertEquals(TransactionSource.ASSISTANT_TEXT, conversation.appendSource);
		assertEquals("record it", conversation.appendUserMessage);
		assertSame(port.output, conversation.appendOutput);
	}

	@Test
	void providerUnavailableDoesNotAppendExchange() {
		ChatAssistantPort port = (actorId, source, history, input) -> {
			throw new AssistantUnavailableException();
		};
		CapturingConversation conversation = new CapturingConversation();

		assertThrows(AssistantUnavailableException.class,
				() -> new AssistantUseCase(port, conversation).execute(
						ACTOR, TransactionSource.ASSISTANT_TEXT, "hello"));
		assertEquals(0, conversation.appendCalls);
	}

	private static final class CapturingPort implements ChatAssistantPort {

		private int calls;
		private UserId actorId;
		private TransactionSource source;
		private List<AssistantConversationMessage> history;
		private AssistantInput input;
		private AssistantOutput output = new AssistantOutput("Okay.", AssistantStatus.COMPLETED);

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
			return output;
		}
	}

	private static final class CapturingConversation implements AssistantConversationPort {

		private int findCalls;
		private int requestedLimit;
		private List<AssistantConversationMessage> history = List.of();
		private int appendCalls;
		private UserId appendActorId;
		private TransactionSource appendSource;
		private String appendUserMessage;
		private AssistantOutput appendOutput;

		@Override
		public List<AssistantConversationMessage> findRecent(UserId actorId, int limit) {
			findCalls++;
			requestedLimit = limit;
			return history;
		}

		@Override
		public void appendExchange(
				UserId actorId,
				TransactionSource source,
				String userMessage,
				AssistantOutput assistantOutput) {
			appendCalls++;
			appendActorId = actorId;
			appendSource = source;
			appendUserMessage = userMessage;
			appendOutput = assistantOutput;
		}

		@Override
		public void clear(UserId actorId) {
		}
	}
}
