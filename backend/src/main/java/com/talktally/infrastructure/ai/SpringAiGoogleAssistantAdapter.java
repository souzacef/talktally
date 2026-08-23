package com.talktally.infrastructure.ai;

import com.google.genai.errors.ApiException;
import com.talktally.application.assistant.AssistantInput;
import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.ChatAssistantPort;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import java.util.Objects;

public class SpringAiGoogleAssistantAdapter implements ChatAssistantPort {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringAiGoogleAssistantAdapter.class);

	private final ChatClient chatClient;
	private final Resource systemPrompt;
	private final Object[] approvedTools;

	public SpringAiGoogleAssistantAdapter(
			ChatClient chatClient,
			Resource systemPrompt,
			TransactionAssistantTools transactionTools,
			ReportingAssistantTools reportingTools,
			ReimbursementAssistantTools reimbursementTools) {
		this.chatClient = Objects.requireNonNull(chatClient, "chat client must not be null");
		this.systemPrompt = Objects.requireNonNull(systemPrompt, "system prompt must not be null");
		this.approvedTools = new Object[] {
				Objects.requireNonNull(transactionTools, "transaction tools must not be null"),
				Objects.requireNonNull(reportingTools, "reporting tools must not be null"),
				Objects.requireNonNull(reimbursementTools, "reimbursement tools must not be null")
		};
	}

	@Override
	public AssistantOutput respond(
			UserId actorId,
			TransactionSource source,
			AssistantInput input) {
		try {
			String response = chatClient.prompt()
					.system(system -> system
							.text(systemPrompt)
							.param("ordinaryTransactionCategories",
									OrdinaryTransactionCategoryVocabulary.systemPromptGuidance()))
					.user(input.message())
					.tools(approvedTools)
					.toolContext(AssistantToolContext.create(actorId, source))
					.call()
					.content();
			return AssistantResponseParser.parse(response);
		}
		catch (AssistantUnavailableException exception) {
			throw exception;
		}
		catch (Exception exception) {
			logProviderFailure(exception);
			throw new AssistantUnavailableException(exception);
		}
	}

	private static void logProviderFailure(Exception exception) {
		Throwable cause = exception.getCause();
		ApiException providerException = findProviderException(exception);
		LOGGER.warn(
				"Assistant provider call failed: exceptionClass={}, causeClass={}, providerHttpStatus={}, providerStatus={}",
				exception.getClass().getName(),
				cause == null ? "none" : cause.getClass().getName(),
				providerException == null ? "unavailable" : providerException.code(),
				providerException == null ? "unavailable" : providerException.status());
	}

	private static ApiException findProviderException(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof ApiException providerException) {
				return providerException;
			}
			current = current.getCause();
		}
		return null;
	}
}
