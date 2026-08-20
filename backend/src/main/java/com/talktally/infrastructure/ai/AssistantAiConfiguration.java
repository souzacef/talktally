package com.talktally.infrastructure.ai;

import com.talktally.application.assistant.AssistantConversationPort;
import com.talktally.application.assistant.AssistantConversationUseCase;
import com.talktally.application.assistant.AssistantUseCase;
import com.talktally.application.assistant.ChatAssistantPort;
import com.talktally.application.assistant.exception.AssistantUnavailableException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration(proxyBeanMethods = false)
public class AssistantAiConfiguration {

	@Bean
	AssistantUseCase assistantUseCase(
			ChatAssistantPort chatAssistantPort,
			AssistantConversationPort conversationPort) {
		return new AssistantUseCase(chatAssistantPort, conversationPort);
	}

	@Bean
	AssistantConversationUseCase assistantConversationUseCase(
			AssistantConversationPort conversationPort) {
		return new AssistantConversationUseCase(conversationPort);
	}

	@Bean
	ChatAssistantPort chatAssistantPort(
			ObjectProvider<ChatModel> chatModelProvider,
			TransactionAssistantTools transactionTools,
			ReportingAssistantTools reportingTools,
			ReimbursementAssistantTools reimbursementTools,
			@org.springframework.beans.factory.annotation.Value(
					"classpath:prompts/talktally-system.txt") Resource systemPrompt) {
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			return (actorId, source, history, input) -> {
				throw new AssistantUnavailableException();
			};
		}
		return new SpringAiGoogleAssistantAdapter(
				ChatClient.builder(chatModel).build(),
				systemPrompt,
				transactionTools,
				reportingTools,
				reimbursementTools);
	}
}
