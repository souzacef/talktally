package com.talktally.infrastructure.ai;

import com.talktally.application.assistant.AssistantInput;
import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantStatus;
import com.talktally.application.assistant.ChatAssistantPort;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import java.util.Objects;

public class SpringAiGoogleAssistantAdapter implements ChatAssistantPort {

	private static final String COMPLETED_MARKER = "[COMPLETED]";
	private static final String CLARIFICATION_MARKER = "[NEEDS_CLARIFICATION]";

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
					.system(systemPrompt)
					.user(input.message())
					.tools(approvedTools)
					.toolContext(AssistantToolContext.create(actorId, source))
					.call()
					.content();
			if (response == null || response.isBlank()) {
				throw new AssistantUnavailableException();
			}
			return toOutput(response.strip());
		}
		catch (AssistantUnavailableException exception) {
			throw exception;
		}
		catch (Exception exception) {
			throw new AssistantUnavailableException(exception);
		}
	}

	private static AssistantOutput toOutput(String response) {
		if (response.startsWith(CLARIFICATION_MARKER)) {
			return new AssistantOutput(
					stripMarker(response, CLARIFICATION_MARKER),
					AssistantStatus.NEEDS_CLARIFICATION);
		}
		return new AssistantOutput(
					stripMarker(response, COMPLETED_MARKER),
					AssistantStatus.COMPLETED);
	}

	private static String stripMarker(String response, String marker) {
		String cleaned = response.startsWith(marker)
				? response.substring(marker.length()).strip()
				: response;
		return cleaned.isEmpty() ? "The assistant completed the request without a message." : cleaned;
	}
}
