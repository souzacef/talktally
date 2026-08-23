package com.talktally.infrastructure.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.genai.errors.ClientException;
import com.talktally.application.assistant.AssistantInput;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SpringAiGoogleAssistantAdapterTests {

	private static final UserId USER_ID = UserId.from(
			UUID.fromString("10000000-0000-0000-0000-000000000091"));
	private static final Logger ADAPTER_LOGGER = (Logger) LoggerFactory.getLogger(
			SpringAiGoogleAssistantAdapter.class);

	private ListAppender<ILoggingEvent> logAppender;

	@AfterEach
	void removeLogAppender() {
		if (logAppender != null) {
			ADAPTER_LOGGER.detachAppender(logAppender);
			logAppender.stop();
		}
	}

	@Test
	void unexpectedProviderFailureIsSafelyLoggedAndNormalized() {
		String privatePrompt = "Record my private financial details";
		String privateProviderDetail = "provider payload contained private data";
		ClientException providerFailure = new ClientException(
				429, "RESOURCE_EXHAUSTED", privateProviderDetail);
		TransientAiException failure = new TransientAiException(
				"request failed for: " + privatePrompt, providerFailure);
		startCapturingLogs();

		AssistantUnavailableException normalized = assertThrows(
				AssistantUnavailableException.class,
				() -> adapter(new ThrowingChatModel(failure)).respond(
						USER_ID,
						TransactionSource.ASSISTANT_TEXT,
						new AssistantInput(privatePrompt)));

		assertSame(failure, normalized.getCause());
		assertEquals(1, logAppender.list.size());
		ILoggingEvent event = logAppender.list.getFirst();
		assertEquals(Level.WARN, event.getLevel());
		String message = event.getFormattedMessage();
		assertTrue(message.contains(TransientAiException.class.getName()));
		assertTrue(message.contains(ClientException.class.getName()));
		assertTrue(message.contains("providerHttpStatus=429"));
		assertTrue(message.contains("providerStatus=RESOURCE_EXHAUSTED"));
		assertFalse(message.contains(privatePrompt));
		assertFalse(message.contains(privateProviderDetail));
		assertNull(event.getThrowableProxy());
	}

	@Test
	void preExistingAssistantUnavailableExceptionIsRethrownWithoutDuplicateLogging() {
		startCapturingLogs();
		SpringAiGoogleAssistantAdapter adapter = adapter(new FixedResponseChatModel("not a valid response"));

		AssistantUnavailableException failure = assertThrows(
				AssistantUnavailableException.class,
				() -> adapter.respond(
						USER_ID,
						TransactionSource.ASSISTANT_TEXT,
						new AssistantInput("hello")));

		assertEquals(AssistantUnavailableException.class, failure.getClass());
		assertTrue(logAppender.list.isEmpty());
	}

	private void startCapturingLogs() {
		logAppender = new ListAppender<>();
		logAppender.start();
		ADAPTER_LOGGER.addAppender(logAppender);
	}

	private SpringAiGoogleAssistantAdapter adapter(ChatModel chatModel) {
		return new SpringAiGoogleAssistantAdapter(
				ChatClient.builder(chatModel).build(),
				new ByteArrayResource("System prompt: {ordinaryTransactionCategories}".getBytes(UTF_8)),
				mock(TransactionAssistantTools.class),
				mock(ReportingAssistantTools.class),
				mock(ReimbursementAssistantTools.class));
	}

	private record ThrowingChatModel(RuntimeException failure) implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			throw failure;
		}
	}

	private record FixedResponseChatModel(String response) implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(
					new Generation(new AssistantMessage(response))));
		}
	}
}
