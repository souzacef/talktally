package com.talktally.application.assistant;

import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;

public interface ChatAssistantPort {

	AssistantOutput respond(UserId actorId, TransactionSource source, AssistantInput input);
}
