package com.talktally.infrastructure.ai;

import com.talktally.application.assistant.AssistantOutput;
import com.talktally.application.assistant.AssistantUseCase;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("ai-live")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
@SpringBootTest(properties = {
		"spring.ai.model.chat=google-genai",
		"spring.datasource.url=jdbc:h2:mem:talktally-ai-live;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=validate",
		"talktally.security.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class GoogleAssistantLiveTests {

	@Autowired
	private AssistantUseCase assistantUseCase;

	@Test
	void googleGenAiReturnsANonMutatingTextResponse() {
		AssistantOutput output = assistantUseCase.execute(
				UserId.generate(),
				TransactionSource.ASSISTANT_TEXT,
				"Reply with one short sentence saying TalkTally is ready. Do not use financial tools.");

		assertFalse(output.message().isBlank());
	}
}
