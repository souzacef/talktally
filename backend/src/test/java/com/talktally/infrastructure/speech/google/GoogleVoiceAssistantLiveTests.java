package com.talktally.infrastructure.speech.google;

import com.talktally.application.speech.SpeechAudio;
import com.talktally.application.speech.SpeechStatus;
import com.talktally.application.speech.TextToSpeechPort;
import com.talktally.application.speech.VoiceAssistantInput;
import com.talktally.application.speech.VoiceAssistantOutput;
import com.talktally.application.speech.VoiceAssistantUseCase;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("ai-voice-live")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
@SpringBootTest(properties = {
		"spring.ai.model.chat=google-genai",
		"spring.datasource.url=jdbc:h2:mem:talktally-ai-voice-live;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=validate",
		"talktally.security.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class GoogleVoiceAssistantLiveTests {

	@Autowired
	private TextToSpeechPort textToSpeechPort;

	@Autowired
	private VoiceAssistantUseCase voiceAssistantUseCase;

	@Test
	void googleGenAiCompletesANonDestructiveTtsSttAssistantTtsRoundTrip() {
		SpeechAudio question = textToSpeechPort.synthesize(
				"What can TalkTally help me with?");
		assertEquals("audio/wav", question.contentType());
		assertTrue(WavPcm16Encoder.isWav(question.audio()));

		VoiceAssistantOutput result = voiceAssistantUseCase.execute(
				UserId.generate(),
				new VoiceAssistantInput(question.audio(), question.contentType()));

		assertFalse(result.transcript().isBlank());
		assertFalse(result.message().isBlank());
		assertEquals(SpeechStatus.GENERATED, result.speechStatus());
		SpeechAudio answer = result.audio().orElseThrow();
		assertEquals("audio/wav", answer.contentType());
		assertTrue(WavPcm16Encoder.isWav(answer.audio()));
	}
}
