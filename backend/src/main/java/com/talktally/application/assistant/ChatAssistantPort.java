package com.talktally.application.assistant;

import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;

import java.util.List;

public interface ChatAssistantPort {

	AssistantOutput respond(
			UserId actorId,
			TransactionSource source,
			List<AssistantConversationMessage> history,
			AssistantInput input);
}
