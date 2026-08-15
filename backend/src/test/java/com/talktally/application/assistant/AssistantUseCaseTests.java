package com.talktally.application.assistant;

import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.application.assistant.exception.InvalidAssistantInputException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistantUseCaseTests {

	private static final UserId ACTOR = UserId.generate();

	@Test
	void blankMessageIsRejectedBeforeCallingPort() {
		CapturingPort port = new CapturingPort();

		assertThrows(InvalidAssistantInputException.class,
				() -> new AssistantUseCase(port).execute(ACTOR, TransactionSource.ASSISTANT_TEXT, "  "));
		assertEquals(0, port.calls);
	}

	@Test
	void oversizedMessageIsRejectedBeforeCallingPort() {
		CapturingPort port = new CapturingPort();

		assertThrows(InvalidAssistantInputException.class,
				() -> new AssistantUseCase(port).execute(
						ACTOR,
						TransactionSource.ASSISTANT_TEXT,
						"x".repeat(AssistantUseCase.MAX_MESSAGE_LENGTH + 1)));
		assertEquals(0, port.calls);
	}

	@Test
	void authenticatedActorIsPassedToPort() {
		CapturingPort port = new CapturingPort();

		new AssistantUseCase(port).execute(ACTOR, TransactionSource.ASSISTANT_TEXT, "hello");

		assertSame(ACTOR, port.actorId);
	}

	@Test
	void trustedSourceIsPassedToPortAndRemainsReusableForVoice() {
		CapturingPort port = new CapturingPort();

		new AssistantUseCase(port).execute(ACTOR, TransactionSource.VOICE, "hello");

		assertEquals(TransactionSource.VOICE, port.source);
	}

	@Test
	void assistantResponseIsReturned() {
		CapturingPort port = new CapturingPort();
		port.output = new AssistantOutput("Recorded.", AssistantStatus.COMPLETED);

		AssistantOutput output = new AssistantUseCase(port).execute(
				ACTOR, TransactionSource.ASSISTANT_TEXT, "  record it  ");

		assertSame(port.output, output);
		assertEquals("record it", port.input.message());
	}

	@Test
	void providerUnavailableExceptionPropagatesAsApplicationException() {
		ChatAssistantPort port = (actorId, source, input) -> {
			throw new AssistantUnavailableException();
		};

		assertThrows(AssistantUnavailableException.class,
				() -> new AssistantUseCase(port).execute(
						ACTOR, TransactionSource.ASSISTANT_TEXT, "hello"));
	}

	private static final class CapturingPort implements ChatAssistantPort {

		private int calls;
		private UserId actorId;
		private TransactionSource source;
		private AssistantInput input;
		private AssistantOutput output = new AssistantOutput("Okay.", AssistantStatus.COMPLETED);

		@Override
		public AssistantOutput respond(
				UserId actorId,
				TransactionSource source,
				AssistantInput input) {
			calls++;
			this.actorId = actorId;
			this.source = source;
			this.input = input;
			return output;
		}
	}
}
