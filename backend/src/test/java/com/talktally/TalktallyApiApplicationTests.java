package com.talktally;

import com.talktally.application.speech.SpeechAudioInput;
import com.talktally.application.speech.SpeechToTextPort;
import com.talktally.application.speech.TextToSpeechPort;
import com.talktally.application.speech.exception.SpeechRecognitionUnavailableException;
import com.talktally.application.speech.exception.SpeechSynthesisUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TalktallyApiApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private SpeechToTextPort speechToTextPort;

	@Autowired
	private TextToSpeechPort textToSpeechPort;

	@Test
	void contextLoads() {
		assertTrue(applicationContext.getBeansOfType(ChatModel.class).isEmpty());
	}

	@Test
	void ordinaryTestProfileUsesOfflineSpeechPorts() {
		assertThrows(SpeechRecognitionUnavailableException.class,
				() -> speechToTextPort.transcribe(
						new SpeechAudioInput(new byte[] { 1, 2 }, "audio/wav")));
		assertThrows(SpeechSynthesisUnavailableException.class,
				() -> textToSpeechPort.synthesize("Offline response"));
	}

}
