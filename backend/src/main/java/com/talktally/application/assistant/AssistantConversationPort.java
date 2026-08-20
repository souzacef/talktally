package com.talktally.application.assistant;

import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;

import java.util.List;

public interface AssistantConversationPort {

	List<AssistantConversationMessage> findRecent(UserId actorId, int limit);

	void appendExchange(
			UserId actorId,
			TransactionSource source,
			String userMessage,
			AssistantOutput assistantOutput);

	void clear(UserId actorId);
}
